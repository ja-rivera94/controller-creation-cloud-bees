import com.cloudbees.hudson.plugins.folder.AbstractFolder
import com.cloudbees.masterprovisioning.kubernetes.KubernetesImagePullSecret
import com.cloudbees.masterprovisioning.kubernetes.KubernetesMasterProvisioning
import com.cloudbees.opscenter.bluesteel.BlueSteelConstants
import com.cloudbees.opscenter.bluesteel.model.Member
import com.cloudbees.opscenter.bluesteel.model.PredefinedRecipes
import com.cloudbees.opscenter.bluesteel.model.TeamModel
import com.cloudbees.opscenter.server.bluesteel.BlueSteelHelper
import com.cloudbees.opscenter.server.bluesteel.ConnectedMasterTeamProperty
import com.cloudbees.opscenter.server.bluesteel.TeamInfo
import com.cloudbees.opscenter.server.bluesteel.TeamMasterSetupScheduler
import com.cloudbees.opscenter.server.bluesteel.security.BlueSteelDefaultRoleManager
import com.cloudbees.opscenter.server.bluesteel.security.BlueSteelSecurityUtils
import com.cloudbees.opscenter.server.bluesteel.security.xml.TeamSecurity
import com.cloudbees.opscenter.server.model.ConnectedMasterProperty
import com.cloudbees.opscenter.server.model.ConnectedMasterPropertyDescriptor
import com.cloudbees.opscenter.server.model.ManagedMaster
import com.cloudbees.opscenter.server.properties.ConnectedMasterLicenseServerProperty
import com.cloudbees.opscenter.server.properties.ConnectedMasterOwnerProperty
import hudson.Util
import hudson.model.User
import hudson.util.DescribableList

String tenantName = 'TENANT_NAME_PLACEHOLDER'
String tenantControllerNamespace = 'TENANT_CONTROLLER_NAMESPACE_PLACEHOLDER'
String tenantTeamName = 'TENANT_TEAM_NAME_PLACEHOLDER'
String cjocVersionInfo = ''

println('tenantControllerNamespace: ' + tenantControllerNamespace)
println('tenantName: ' + tenantName)
println('tenantTeamName: ' + tenantTeamName)

/**
* Following attributes may be specified. The values proposed are the default from version 2.2.9 of Master Provisioning
*
* Note: If not setting properties explicitly, the defaults will be used.
*/
/* Master */
String  masterDescription = ""
String  masterPropertyOwners = ""
Integer masterPropertyOwnersDelay = 5

/* Master Provisioning */
Integer k8sDisk = 50
Integer k8sMemory = 3072
/**
* Since version 2.235.4.1, we recommend not using the Heap Ratio. Instead add `-XX:MinRAMPercentage` and
* `-XX:MaxRAMPercentage` to the Java options. For example, a ratio of 0.5d translate to a percentage of 50:
* `-XX:MinRAMPercentage=50.0 -XX:MaxRAMPercentage=50.0`
*
* See https://support.cloudbees.com/hc/en-us/articles/204859670-Java-Heap-settings-best-practice and
* https://docs.cloudbees.com/docs/release-notes/latest/cloudbees-ci/modern-cloud-platforms/2.235.4.1.
*/
Double  k8sMemoryRatio = null
Double  k8sCpus = 1
String  k8sFsGroup = "1000"
Boolean k8sAllowExternalAgents = false
String  k8sClusterEndpointId = "default"
String  k8sEnvVars = ""
String  k8sJavaOptions = "-XX:MinRAMPercentage=50.0 -XX:MaxRAMPercentage=50.0"
String  k8sJenkinsOptions = ""
String  k8sImage = 'CloudBees CI - Managed Controller - CJOC_VERSION_PLACEHOLDER'
println(k8sImage)

// Set image pull secret to caas-docker-release-local-ro
def k8sImagePullSecret1 = new KubernetesImagePullSecret()
k8sImagePullSecret1.setValue("caas-docker-release-local-ro")
List<KubernetesImagePullSecret> k8sImagePullSecrets = Arrays.asList(k8sImagePullSecret1)

Integer k8sLivenessInitialDelaySeconds = 300
Integer k8sLivenessPeriodSeconds = 10
Integer k8sLivenessTimeoutSeconds = 10
Integer k8sReadinessInitialDelaySeconds = 30
Integer k8sReadinessFailureThreshold = 100
Integer k8sReadinessTimeoutSeconds = 5
String  k8sStorageClassName = "nfs-client-cloudbees"
String  k8sSystemProperties = ""
String  k8sNamespace = "${tenantControllerNamespace}"
String  k8sNodeSelectors = ""
Long    k8sTerminationGracePeriodSeconds = 1200L
String  k8sYaml = ""

/* Team */
String iconName = "cloudbees"
String iconNColor = "#7ac2d6"
Member [] members = [
  new Member("caas_${tenantName}_ci", Arrays.asList("TEAM_MEMBER")),
  new Member("caas_${tenantName}_dev", Arrays.asList("TEAM_MEMBER")),
  new Member("caas-test-user", Arrays.asList("TEAM_MEMBER"))
  // ... Add more TEAM_ADMIN, TEAM_MEMBER or TEAM_GUEST
] as Member[]

/*****************
* VALIDATION    *
*****************/

/**
* Validate that the master name is not empty
*/
name = Util.fixEmpty(tenantTeamName)
if (name == null) {
  String errMsg = "[ERROR] A team name must be provided"
  println(errMsg)
  throw new Exception(errMsg)
}

/**
* Validate that there is a "teams" folder
*/
AbstractFolder teamsFolder = jenkins.model.Jenkins.instanceOrNull.getItemByFullName(BlueSteelConstants.CJOC_TEAMS_FOLDER_NAME, AbstractFolder.class)
if (teamsFolder == null) {
  String errMsg = "[ERROR] Could not create the 'Teams' folder on Operations Center There may be another item which is " +
                  "not a folder, with the same name. No Teams can be created until this item is removed"
  println(errMsg)
  throw new Exception(errMsg)
}

teamName = BlueSteelHelper.sanitizeName(name)
String teamDisplayName = name

if (BlueSteelHelper.getTeamMaster(teamName) != null) {
  String errMsg = "[ERROR] Another team is named that already!"
  println(errMsg)
  throw new Exception(errMsg)
}

/**
* Validate there isn't any item already with the same name, only validating in the root folder where we are going to create it
*/
if (teamsFolder.getItem(teamName) != null) {
  String errMsg = "The Team \"%s\" has a naming collision with an Item named \"%s\" in the folder \"%s\""
  printf(errMsg, teamDisplayName, teamName, BlueSteelConstants.CJOC_TEAMS_FOLDER_NAME)
  throw new Exception(errMsg)
}

/**********************
* PREPARE TEAM MODEL *
**********************/

/**
* Provide Team configuration (similar to what is configured through the Teams UI)
*/
TeamModel teamModel = new TeamModel()
teamModel.setName(teamName)
teamModel.setDisplayName(teamDisplayName)
teamModel.setMembers(members)
teamModel.setIcon(new TeamModel.TeamIcon(iconName, iconNColor))
teamModel.setCreationRecipe(PredefinedRecipes.BASIC)

/*****************
* CREATE MASTER *
*****************/

/**
* Create a Managed Masters with just a name (this will automatically fill required values for id, idName, grantId, etc...)
* Similar to creating an item in the UI
*/
ManagedMaster master = teamsFolder.createProject(ManagedMaster.class, teamName)
master.setDisplayName(teamModel.getDisplayName())
master.setDescription(masterDescription)

/********************
* CONFIGURE MASTER *
********************/

/**
* Initialize the default MasterProvisioning configuration
*/
KubernetesMasterProvisioning masterProvisioning = new KubernetesMasterProvisioning()
masterProvisioning.setDomain(teamName.toLowerCase())

/**
* Apply Managed Master provisioning configuration (similar to what is configured through the Managed Master UI)
* Note: If not setting properties explicitly, the defaults will be used.
*/
masterProvisioning.setDisk(k8sDisk)
masterProvisioning.setMemory(k8sMemory)
if (k8sMemoryRatio) {
  masterProvisioning.setHeapRatio(new com.cloudbees.jce.masterprovisioning.Ratio(k8sMemoryRatio))
  /**
  * For versions earlier than 2.235.4.1 (Master Provisioning plugin 2.5.6), use setRatio
  * masterProvisioning.setRatio(k8sMemoryRatio)
  */
}
masterProvisioning.setCpus(k8sCpus)
masterProvisioning.setFsGroup(k8sFsGroup)
masterProvisioning.setAllowExternalAgents(k8sAllowExternalAgents)
masterProvisioning.setClusterEndpointId(k8sClusterEndpointId)
masterProvisioning.setEnvVars(k8sEnvVars)
masterProvisioning.setJavaOptions(k8sJavaOptions)
masterProvisioning.setJenkinsOptions(k8sJenkinsOptions)
masterProvisioning.setImage(k8sImage)
masterProvisioning.setImagePullSecrets(k8sImagePullSecrets)
masterProvisioning.setLivenessInitialDelaySeconds(k8sLivenessInitialDelaySeconds)
masterProvisioning.setLivenessPeriodSeconds(k8sLivenessPeriodSeconds)
masterProvisioning.setLivenessTimeoutSeconds(k8sLivenessTimeoutSeconds)
masterProvisioning.setReadinessInitialDelaySeconds(k8sReadinessInitialDelaySeconds)
masterProvisioning.setReadinessFailureThreshold(k8sReadinessFailureThreshold)
masterProvisioning.setReadinessTimeoutSeconds(k8sReadinessTimeoutSeconds)
masterProvisioning.setStorageClassName(k8sStorageClassName)
masterProvisioning.setSystemProperties(k8sSystemProperties)
masterProvisioning.setNamespace(k8sNamespace)
masterProvisioning.setNodeSelectors(k8sNodeSelectors)
masterProvisioning.setTerminationGracePeriodSeconds(k8sTerminationGracePeriodSeconds)
masterProvisioning.setYaml(k8sYaml)

/**
* Save the configuration
*/
master.setConfiguration(masterProvisioning)
master.save()

/**
* Team Master required configuration
*/
// Force install the plugins we needs
BlueSteelHelper.replaceSystemProperty(master, BlueSteelConstants.IM_PLUGIN_WAR_PROFILE_PROPERTY_NAME, BlueSteelConstants.IM_PLUGIN_WAR_PROFILE)
// Force auto install of incremental updates during first boot
BlueSteelHelper.replaceSystemProperty(master, BlueSteelConstants.BK_AUTO_INTALL_INCREMENTAL_PROPERTY_NAME, Boolean.TRUE.toString())
// Disable full upgrades for this first boot or the incremental updates won't be taken into account
BlueSteelHelper.replaceSystemProperty(master, BlueSteelConstants.BK_NO_FULL_UPGRADE_PROPERTY_NAME, Boolean.TRUE.toString())

/**
* Team Master Properties
*/
DescribableList<ConnectedMasterProperty, ConnectedMasterPropertyDescriptor> masterProperties = master.getProperties()
if (masterPropertyOwners != null && !masterPropertyOwners.isEmpty()) {
  masterProperties.replace(new ConnectedMasterOwnerProperty(masterPropertyOwners, masterPropertyOwnersDelay))
}
masterProperties.replace(new ConnectedMasterLicenseServerProperty(new ConnectedMasterLicenseServerProperty.DescriptorImpl().defaultStrategy()))

/**
* Team Master required property
*/
ConnectedMasterTeamProperty property = new ConnectedMasterTeamProperty()
masterProperties.replace(property)

// Set Team information
TeamInfo teamInfo = new TeamInfo(
  teamModel.getName(),
  teamModel.getDisplayName(),
  teamModel.getIcon() != null ? new TeamInfo.TeamIcon(teamModel.getIcon()) : null,
  null,
  teamModel.getCreationRecipe())
property.setTeamInfo(teamInfo)
// Set Team security
TeamSecurity teamSecurity = new TeamSecurity(
  BlueSteelDefaultRoleManager.getDefaultRolesMap(),
  BlueSteelSecurityUtils.toUserMapping(teamModel.getMembers()),
  BlueSteelDefaultRoleManager.getDefaultRole())
BlueSteelSecurityUtils.checkTeamSecurityRule(teamSecurity)
property.setTeamSecurity(teamSecurity)

/**
* Save the configuration
*/
master.save()

/******************************
* PROVISION AND START MASTER *
******************************/

master.provisionAndStartAction()
println "Started the master..."

/**
* Retrieve the master from the API and print the details of the created Managed Master
  Wait for it to start up first
*/
def instance = jenkins.model.Jenkins.instanceOrNull.getItemByFullName(master.fullName, ManagedMaster.class)
println "instanceName ${instance.name}"
println " id: ${instance.id}"
println " idName: ${instance.idName}"
println " info: ${instance.properties.get(ConnectedMasterTeamProperty.class).getTeamInfo()}"
println " security: ${instance.properties.get(ConnectedMasterTeamProperty.class).getTeamSecurity().userMapping}"

TeamMasterSetupScheduler.get().submitTeamSetup(master)
return
