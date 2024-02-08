@Library(['common-lib@3-stable', 'caas-shared-lib']) _

import groovy.transform.Field;

properties([
  parameters([
    string(name: 'tenant_name', description: 'The Name of the tenant (Team/LOB) being onboarded.'),
    string(name: 'buc_code', description: 'The BUC code of the Tenant being onboarded'),
    string(name: 'email_address', description: 'The email address which will be notified after finished the onboarding'),
    choice(name: 'sonarqube_edition', description: 'The SonarQube version to be onboarded', choices: ['SonarQube_Community_Edition','SonarQube_Enterprise_Edition']),
    string(name: 'owner', description: 'The FIS E-number of the LOB Bitbucket Respository'),
    booleanParam(name: 'RUN_STAGE_SETUP_KUBERNETES', defaultValue: true, description: 'Enables or disables running the Setting up of Kubernetes step'),
    booleanParam(name: 'RUN_STAGE_SETUP_BITBUCKET_REPO', defaultValue: true, description: 'Enables or disables running the setup of the bitbucket repo step'),
    booleanParam(name: 'RUN_STAGE_SETUP_APPLY_CJOC_BUNDLE', defaultValue: true, description: 'Enables or disables running the setup and apply of the cjoc bundle step'),
    booleanParam(name: 'RUN_STAGE_VERIFY_BUNDLES_RELOAD', defaultValue: true, description: 'Enables or disables running the verify bundle reload step'),
    booleanParam(name: 'RUN_STAGE_ADD_PR_PIPELINES', defaultValue: true, description: 'Enables or disables running the add PR pipelines step'),
    booleanParam(name: 'RUN_STAGE_CREATE_TEAM_CONTROLLER', defaultValue: true, description: 'Enables or disables running the create team controller step'),
    booleanParam(name: 'RUN_STAGE_POST_CREATE_TEAM_CONTROLLER', defaultValue: true, description: 'Enables or disables running the rpost create team controller step'),
    booleanParam(name: 'RUN_STAGE_ONBOARD_SONARQUBE', defaultValue: true, description: 'Enables or disables running the onboarding of sonarQube step'),
    booleanParam(name: 'RUN_STAGE_ONBOARD_EMAIL_NOTIFICATION', defaultValue: true, description: 'Enables or disables running the onboard email notification step'),
    booleanParam(name: 'RUN_WITH_DEBUG_LOGS', defaultValue: true, description: 'Enables or disables running the the pipeline with debug logs'),
  ]),
  disableConcurrentBuilds()
])

// Config different between branches
@Field final String CLOUDBEES_EXTERNAL_URL = "https://cloudbees-internal.dev.fiscicd.com"
@Field final String CLUSTER = "internal"
@Field final String SONARQUBE_BRANCH = 'development'

// Shared config
@Field final String BITBUCKET_HOSTNAME = "bitbucket.fis.dev"
@Field final String BITBUCKET_URL = "https://${BITBUCKET_HOSTNAME}"
@Field final String BITBUCKET_PROJECT_KEY = "CAASCUST"
@Field final String LOCAL_FOLDER_CLOUDBEES = "caas-controller-bundle"
@Field final String CJOC_CASC_PROJECT_KEY = "caas"
@Field final String CJOC_CASC_REPO_NAME = "cloudbees-infrastructure"
@Field final String CJOC_CASC_REPO_URL = "${BITBUCKET_URL}/scm/${CJOC_CASC_PROJECT_KEY}/${CJOC_CASC_REPO_NAME}.git"
@Field final String CJOC_CASC_REPO_BRANCH = "main"
@Field final String CJOC_CASC_PATH_PREFIX = "infra/env"
@Field final String CLOUDBEES_JENKINS_YAMLFILE = "jenkins.yaml"
@Field final String CLOUDBEES_BUNDLE_YAMLFILE = "bundle.yaml"
@Field final String JENKINS_CREDENTIALS_ID_RW = "SVCACCT_CAAS_BB_RW"
@Field final String JENKINS_CREDENTIALS_ID_RO = "SVCACCT_CAAS_BB_RO"
@Field final String JENKINS_CREDENTIALS_BB_PR_TOKEN = "bitbucket-pr-webhook-token"
@Field final String CJOC_CONFIG_MAP_NAME = "oc-casc-bundle"
@Field final String CLOUDBEES_K8S_URL = "http://cjoc.cloudbees-sda.svc.cluster.local"
@Field final String CLOUDBEES_MANAGEMENT_K8S_URL = "http://caas.lob-caas.svc.cluster.local"
@Field final String CJOC_PR_PIPELINES_FOLDER_NAME = "controller-pr-jobs"
@Field final String SONARQUBE_COMMUNITY_URL = 'http://sonarqube.sonarqube-community.svc.cluster.local:33333'
@Field final String SONARQUBE_ENTERPRISE_URL = 'http://sonarqube.sonarqube.svc.cluster.local:33333'
@Field String APPROVED_PR_TOKEN = ""
@Field final String CAAS_CONTROLLER_DOMAIN_NAME = "caas"
// the teams webhook is for Caas Alerts Channel
@Field final String TEAMS_WEBHOOK_URL = 'https://fisglobal.webhook.office.com/webhookb2/288701ce-8102-4ffb-84ca-21c04ece47fc@e3ff91d8-34c8-4b15-a0b4-18910a6ac575/JenkinsCI/559b293d48fd4e1b92c0861b2425b15e/f88aac12-97f6-41f5-bac9-fe98566cdc42'


@Field final String newBundleTplString = """
name: <bundleName>
retriever:
  SCM:
    defaultVersion: <scmDefaultVersion>
    scmSource:
      git:
        credentialsId: <gitCredentialsId>
        id: <gitId>
        remote: <gitRemoteUrl>
        traits:
        - gitBranchDiscovery
        - headRegexFilter:
            regex: "<scmDefaultVersion>"
"""

@Field final String pullRequestJenkinsfileTplString = """
@Library(['common-lib@3-stable', 'caas-shared-lib']) _

cloudbeesPRValidation()

"""

/*****************
 * INPUTS        *
 *****************/

def teamName = "${tenant_name}"
def lobNamespace = ("lob-" + "${tenant_name}")
def cjocNamespace = "cloudbees-sda"
def buc_code = "${buc_code}"
def default_folder_name = "welcome"
def tenanturl = "${CLOUDBEES_EXTERNAL_URL}/${tenant_name}"
String tenantK8sUrl = "http://${tenant_name}.lob-${tenant_name}.svc.cluster.local"
String tenantEmail = "${email_address}"
String owner = "${owner}"
def bundleName = "${tenant_name}_bundles"
String repoName = "caas_${tenant_name}-internal_jenkins"
String branchName = "main"

String podTemplateString = '''apiVersion: v1
kind: Pod
spec:
  containers:
    - name: helm
      image: caas-docker-release-local.docker.fis.dev/mirror/alpine/helm
      command:
        - sleep
      args:
        - 99d
    - name: aws-cli
      image: caas-docker-release-local.docker.fis.dev/mirror/amazon/aws-cli:2.7.25
      command:
        - sleep
      args:
        - 99d
    - name: kubectl
      image: caas-docker-release-local.docker.fis.dev/mirror/bitnami/kubectl:1.24.2
      command:
        - sleep
      args:
        - 99d
      securityContext:
        runAsUser: 1000
    - name: python3
      image: caas-docker-release-local.docker.fis.dev/mirror/python:latest
      command:
        - sleep
      args:
        - 99d
  imagePullSecrets:
    - name: caas-docker-release-local-ro
  '''

podTemplate(yaml: podTemplateString, serviceAccount: 'controller-manager') {
    node(POD_LABEL) {
        try {
            stage('checkout') {
                dir("${LOCAL_FOLDER_CLOUDBEES}") {
                    git branch: scm.branches[0].name, credentialsId: JENKINS_CREDENTIALS_ID_RO, url: scm.userRemoteConfigs[0].url
                }
            }


            stage('Setup') {
                //turn on debug, if run with debug logs is true
                if (params.RUN_WITH_DEBUG_LOGS) {
                    enaLogger.setDebug(true)
                }

                // Install python modules early
                container('python3') {
                    sh "pip install ansible pyyaml jinja2 requests"
                }
            }

            stage('Setting Up Kubernetes') {
                if (params.RUN_STAGE_SETUP_KUBERNETES) {
                    container('kubectl') {
                        createNamespace(lobNamespace, buc_code)
                    }
                    container('python3') {
                        createTenantSecret()
                    }
                    container('helm') {
                        createControllerRBAC(cjocNamespace, lobNamespace)
                    }
                }
            }

            stage('Setting Up BitBucket Repo') {
                if (params.RUN_STAGE_SETUP_BITBUCKET_REPO) {
                    container("python3") {
                        createRepo(repoName, branchName)
                        dir("${bundleName}") {
                            settingControllerFiles(tenant_name, sonarqube_edition, repoName, branchName, bundleName)
                        }
                        addAdminUserToBBRepo(repoName, owner)
                        addReadOnlySvcAcctToBBRepo(repoName, JENKINS_CREDENTIALS_ID_RO)
                        addPRWebhookToBBRepo(repoName, "${CLOUDBEES_EXTERNAL_URL}/${CAAS_CONTROLLER_DOMAIN_NAME}")
                        addMergedWebhookToBBRepo(repoName, teamName)
                    }
                }
            }

            stage('Setting & Applying Bundle in CJOC') {
                if (params.RUN_STAGE_SETUP_APPLY_CJOC_BUNDLE) {
                    container("python3") {
                        dir("${CJOC_CASC_REPO_NAME}") {
                            addCascBundleLocation(bundleName, repoName, branchName)
                        }
                    }
                    container('kubectl') {
                        dir("./${CJOC_CASC_REPO_NAME}/${CJOC_CASC_PATH_PREFIX}/caas-${CLUSTER}") {
                            updateCJOCConfigMap(
                                cjocNamespace: cjocNamespace,
                                cjocConfigMapName: CJOC_CONFIG_MAP_NAME
                            )
                        }
                    }
                }
            }

            stage('Verify bundle Reload') {
                if (params.RUN_STAGE_VERIFY_BUNDLES_RELOAD) {
                    container('kubectl') {
                        /**
                        DE522480
                        updateCJOCConfigMap is a bit confusing. while it updates the configmap right away, there is a related delay
                        This is because the configmap changes may take up to around 1 minute for the volume mount files to reflect
                        the configmap changes due to how k8s syncs changes using a controller
                        https://kubernetes.io/docs/concepts/configuration/configmap/#mounted-configmaps-are-updated-automatically
                        We need to consider the delay for up to around a minute for cjoc to see changes in /var/jenkins_config/oc-casc-bundle/jenkins.yaml
                        once /var/jenkins_config/oc-casc-bundle/jenkins.yaml has been updated, it might take Jenkins a bit of time
                        to then detect that jenkins.yaml has changes
                        */
                        // assert against expected state. namely that the /var/jenkins_config/oc-casc-bundle/jenkins.yaml file reflects the new onboard. retry up till our bailout point
                        cjoc.waitForBundleFileSystemDetection(timeout: 600, matchString: bundleName, matchStringExists: true)
                    }
                    // assert that we expect casc-bundle-check-bundle-update has "update-available":true. retry up till our bailout point
                    container('python3') {
                        cjoc.waitForCheckBundleUpdateDetection(timeout: 600, url: "${CLOUDBEES_K8S_URL}/cjoc", credentialsId: 'caas-cbj-svc')
                        cjoc.reloadBundle(url: "${CLOUDBEES_K8S_URL}/cjoc", credentialsId: 'caas-cbj-svc')

                        /**
                            DE522480 part 2
                            From changes above, we should be decently assured that the casc bundle is configured in cjoc
                            Now, there is an assumed additional wait period as CJOC has reloaded its cjoc-level bundle but has yet to
                            sync the tenant level bundle into its internal storage
                            Note - Our current version of cloudbees production (2.346.1.4) does not support the method
                            'casc-check-out-bundles' which appears to force a check out of the casc bundles configured in cjoc so
                            work around that by adding an additional wait to account for 'polling' which is set to 60 seconds for each
                            bundle plus some overhead for the actual syncing of the bundle into internal storage
                            validateUploadedBundle throws an exception if a non-200 is returned so with a retry of up to 7 times
                            gives the pipeline up to 3.5 minutes to complete syncing the bundle into internal storage
                            */
                        retry(7) {
                            cjoc.validateUploadedBundle(
                                    cjocCredentialsId: 'caas-cbj-svc',
                                    cloudbeesK8sUrl: CLOUDBEES_K8S_URL,
                                    bundleId: bundleName
                            )
                            sleep 30
                        }
                    }
                }
            }

            stage('Add PR Pipelines') {
                if (params.RUN_STAGE_ADD_PR_PIPELINES) {
                    container('python3') {
                        dir("${LOCAL_FOLDER_CLOUDBEES}") {
                            createApprovedPRPipeline(
                                    bitbucketProjectKey: BITBUCKET_PROJECT_KEY,
                                    bitbucketRepoName: repoName,
                                    bitbucketROCredentialsId: JENKINS_CREDENTIALS_ID_RO,
                                    bitbucketUrl: BITBUCKET_URL,
                                    cjocCredentialsId: 'caas-cbj-svc',
                                    cloudbeesK8sUrl: CLOUDBEES_MANAGEMENT_K8S_URL,
                                    jobFolderPath: CJOC_PR_PIPELINES_FOLDER_NAME,
                                    teamName: teamName
                            )

                            createMergedPRPipeline(
                                    cjocCredentialsId: 'caas-cbj-svc',
                                    cloudbeesK8sUrl: CLOUDBEES_MANAGEMENT_K8S_URL,
                                    jobFolderPath: CJOC_PR_PIPELINES_FOLDER_NAME,
                                    prTokenCredentialsId: JENKINS_CREDENTIALS_BB_PR_TOKEN,
                                    teamName: teamName
                            )
                        }
                    }
                }
            }

            stage('Create Team Controller') {
                if (params.RUN_STAGE_CREATE_TEAM_CONTROLLER) {
                    // Create and start tenant controller in CJOC
                    createAndStartTeamsController(lobNamespace, tenant_name, teamName)
                }
            }

            // Post tenant controller creation activities
            stage('Post Team Controller Creation') {
                if (params.RUN_STAGE_POST_CREATE_TEAM_CONTROLLER) {
                    container('python3') {
                        addMemberToGroupInCjoc(tenant_name)
                        createFolderInTeamController(tenant_name, default_folder_name)
                    }
                    addRBACPermissionForTeamsController(tenant_name, bundleName)
                    createTemplateCatalogInTenant(tenant_name)
                }
            }

            stage('Onboarding Sonarqube') {
                if (params.RUN_STAGE_ONBOARD_SONARQUBE) {
                    container('python3') {
                        sonarqubeOnboard(
                            tenant: tenant_name,
                            edition: sonarqube_edition,
                            branch: SONARQUBE_BRANCH,
                            cloudbeesUrl: tenantK8sUrl,
                        )
                    }
                }
            }

            stage('Onboarding Email Notification') {
                if (params.RUN_STAGE_ONBOARD_EMAIL_NOTIFICATION) {
                    container('aws-cli') {
                        sendOnboardingEmail(tenantEmail, teamName)
                    }
                }
            }
        }
        catch (err) {
            alertMessage = "\nCreate Controller has failed here are the details\n"
            // sends Teams Alert. Each name/template only shows if they are not = null
            // Only send alerts if main branch is used, not when people are testing branches.
            println "branch name is : ${scm.branches[0].toString()}" //output debugging information for now.
            if (scm.branches[0].toString().contains('main')) {
                enaNotifiers.notifyMicrosoftTeams(webhookUrls: TEAMS_WEBHOOK_URL, message: alertMessage, status: 'failure')
            }
            enaLogger.debug(msg: alertMessage, err: err)
            throw new Exception(alertMessage, err)
        }
        finally {
            enaLogger.debug(msg: 'End of Pipeline)')
        }

    }
}

def createAndStartTeamsController(lobNamespace, tenant_name, teamName) {
  enaLogger.debug(msg: 'Start of method createAndStartTeamsController()')
  enaLogger.debug(msg: "Parameter lobNamespace passed in value: ${lobNamespace}")
  enaLogger.debug(msg: "Parameter tenant_name passed in value: ${tenant_name}")
  enaLogger.debug(msg: "Parameter lobNamespace passed in value: ${lobNamespace}")

  enaParams.required(name: 'lobNamespace', value: lobNamespace, class: String.class)
  enaParams.required(name: 'tenant_name', value: tenant_name, class: String.class)
  enaParams.required(name: 'teamName', value: teamName, class: String.class)

  dir("${LOCAL_FOLDER_CLOUDBEES}/controller-creation") {
    String templateFile = 'create_teams_controller.groovy'
    String cjocUrl = "${CLOUDBEES_K8S_URL}/cjoc"
    String version = cjoc.version(baseURL: cjocUrl, credentialsId: 'caas-cbj-svc')

    // replace stubbed values in the template. this can very likely be improved
    sh "sed -i 's/TENANT_NAME_PLACEHOLDER/${tenant_name}/g' ${templateFile}"
    sh "sed -i 's/TENANT_CONTROLLER_NAMESPACE_PLACEHOLDER/${lobNamespace}/g' ${templateFile}"
    sh "sed -i 's/TENANT_TEAM_NAME_PLACEHOLDER/${teamName}/g' ${templateFile}"
    sh "sed -i 's/CJOC_VERSION_PLACEHOLDER/${version}/g' ${templateFile}"

    cjoc.fetchJenkinsCLIIfNotAvailable(baseURL: cjocUrl)

    try {
      container('jnlp') { // need java
        withCredentials([usernameColonPassword(credentialsId: 'caas-cbj-svc', variable: 'auth')]) {
          // withEnv being used to allow for secure passing of auth parameter without resulting in sh wrapped doublequotes
          // JENKINS_URL is automatically used by jenkins-cli for '-s' param for server url
          withEnv(["JENKINS_URL=${cjocUrl}", "templateFile=${templateFile}"]) {
            sh 'java -jar jenkins-cli.jar -auth ${auth} -webSocket groovy = < ${templateFile}'
          }
        }
      }
    } catch (err) {
      println err
      error 'An error occurred while creating the teams controller'
    }
  }

  enaLogger.debug(msg: 'End of method createAndStartTeamsController()')
}

def addMemberToGroupInCjoc(String tenantName) {
  enaLogger.debug(msg: 'Start of method addMemberToGroupInCjoc()')
  enaLogger.debug(msg: "Parameter tenantName passed in value: ${tenantName}")

  enaParams.required(name: 'tenantName', value: tenantName, class: String.class)

  withCredentials([usernameColonPassword(credentialsId: 'caas-cbj-svc', variable: 'auth')]) {
    sh 'curl --user "${auth}" -X POST ' + "${CLOUDBEES_K8S_URL}/cjoc/groups/caas_developers/addGroup?name=caas_${tenantName}_dev"
    sh 'curl --user "${auth}" -X POST ' + "${CLOUDBEES_K8S_URL}/cjoc/groups/caas_developers/addGroup?name=caas_${tenantName}_ci"
  }

  enaLogger.debug(msg: 'End of method addMemberToGroupInCjoc()')
}

// Create the Controller RBAC for the new controller creation
def createControllerRBAC(cjoc_namepsace, lob_namespace) {
  enaLogger.debug(msg: 'Start of method createControllerRBAC()')
  enaLogger.debug(msg: "Parameter cjoc_namepsace passed in value: ${cjoc_namepsace}")
  enaLogger.debug(msg: "Parameter lob_namespace passed in value: ${lob_namespace}")

  enaParams.required(name: 'cjoc_namepsace', value: cjoc_namepsace, class: String.class)
  enaParams.required(name: 'lob_namespace', value: lob_namespace, class: String.class)

  //Version of cjoc/agents/masters we should be using
  String imageVersion = "2.346.1.4"
  String chartVersion = "3.12552.0+e067425808d0" //latest as of June 14th 2023 - locking this down so we are not in a point of using the wrong values
  String dockerSecretName = "caas-docker-release-local-ro"
  String cloudbeesDockerRegistry = "caas-docker-release-local.docker.fis.dev/mirror/cloudbees"

  sh """
      helm repo add cloudbees https://charts.cloudbees.com/public/cloudbees
      helm install controllers-rbac cloudbees/cloudbees-core \
        --version ${chartVersion} \
        --namespace ${lob_namespace} \
        --set OperationsCenter.Enabled=false \
        --set OperationsCenter.ImagePullSecrets=${dockerSecretName} \
        --set Master.Enabled=true \
        --set Master.OperationsCenterNamespace=${cjoc_namepsace} \
        --set Master.image.registry=${cloudbeesDockerRegistry} \
        --set Master.image.repository=cloudbees-core-mm \
        --set Master.image.tag=${imageVersion} \
        --set Agents.Enabled=true \
        --set Agents.image.registry=${cloudbeesDockerRegistry} \
        --set Agents.image.repository=cloudbees-core-agent \
        --set Agents.image.tag=${imageVersion} \
        --set Agents.ImagePullSecrets=${dockerSecretName} \
        --debug
     """

  enaLogger.debug(msg: 'End of method createControllerRBAC()')
}

// Create the namespace to which controller will be provisioned

def createNamespace(namespace_name, buc_code) {
  enaLogger.debug(msg: 'Start of method createNamespace()')
  enaLogger.debug(msg: "Parameter namespace_name passed in value: ${namespace_name}")
  enaLogger.debug(msg: "Parameter buc_code passed in value: ${buc_code}")

  enaParams.required(name: 'namespace_name', value: namespace_name, class: String.class)
  enaParams.required(name: 'buc_code', value: buc_code, class: String.class)

  enaLogger.log(msg: "Creating namespace: ${namespace_name}")
  sh "kubectl create namespace ${namespace_name}"
  sh "kubectl label namespace ${namespace_name} buc=${buc_code}"

  enaLogger.log(msg: "Namespace ${namespace_name} created")
  enaLogger.debug(msg: 'End of method createNamespace()')
}

/**
  This method requires the tenant controller URL to be operational to succeed. Based on watching a few startup times,
  this takes about a min for a new controller. Adding a retry loop with a 1 minute wait for slow starting controllers.
*/

// Create the catalog pipeline jobs

def createTemplateCatalogInTenant(tenantName) {
  String tenant_url = "http://${tenantName}.lob-${tenantName}.svc.cluster.local/${tenantName}"
  String catalogJson = """[
  {
    "branchOrTag": "master",
    "parentName": "/${tenantName}",
    "scm": {
      "\$class": "GitSCMSource",
      "credentialsId": "${JENKINS_CREDENTIALS_ID_RO}",
      "remote": "https://bitbucket.fis.dev/scm/caas/caas-pipeline-templates-catalog.git"
    },
    "updateInterval": "1d"
  }
  ]"""

  writeFile(file: 'tenant-catalog.json', text: catalogJson)

  container('python3') {
    cjoc.waitForControllerOnline(baseURL: tenant_url, timeout: 600)
    cjoc.fetchJenkinsCLIIfNotAvailable(baseURL: "${CLOUDBEES_K8S_URL}/cjoc")
  }

  try {
    container('jnlp') { // need java
      withCredentials([usernameColonPassword(credentialsId: 'caas-cbj-svc', variable: 'auth')]) {
        // withEnv being used to allow for secure passing of auth parameter without resulting in sh wrapped doublequotes
        // JENKINS_URL is automatically used by jenkins-cli for '-s' param for server url
        withEnv(["JENKINS_URL=${tenant_url}"]) {
          retry(5) {
            sleep 5  // likely unneeded when using k8s internal service urls
            sh 'java -jar jenkins-cli.jar -auth "${auth}" pipeline-template-catalogs --put < tenant-catalog.json'
          }
        }
      }
    }
  } catch (err) {
    println err
    error 'An error occurred while attempting to create the Pipeline Templates Catalog'
  }
}
/**
  This method waits till the tenant controller URL to be operational. Based curl retry command.
*/

// Configure the RBAC, sets the bundle availability and restarts the controller to take up the CASC configuration

def addRBACPermissionForTeamsController(tenantName, bundleName) {
  // Using kubernetes local address for cjoc
  String cjocUrl = "${CLOUDBEES_K8S_URL}/cjoc"
  String tenant_url = "http://${tenantName}.lob-${tenantName}.svc.cluster.local/${tenantName}"

  container('python3') {
    cjoc.waitForControllerOnline(baseURL: tenant_url, timeout: 600)
    cjoc.fetchJenkinsCLIIfNotAvailable(baseURL: cjocUrl)
  }

  try {
    container('jnlp') { // need java
      withCredentials([usernameColonPassword(credentialsId: 'caas-cbj-svc', variable: 'auth')]) {
        // withEnv being used to allow for secure passing of auth parameter without resulting in sh wrapped doublequotes
        // JENKINS_URL is automatically used by jenkins-cli for '-s' param for server url
        withEnv(["JENKINS_URL=${cjocUrl}"]) {
          retry(5) {
            sleep 5  // likely unneeded when using k8s internal service urls
            sh """
              java -jar jenkins-cli.jar -auth ${auth} team-permissions ${tenantName}  TEAM_MEMBER --add hudson.model.View.Create --type team
              java -jar jenkins-cli.jar -auth ${auth} team-permissions ${tenantName} TEAM_MEMBER --add hudson.model.View.Configure --type team
              java -jar jenkins-cli.jar -auth ${auth} team-permissions ${tenantName} TEAM_MEMBER --add hudson.model.View.Delete --type team
            """
          }
          retry(5) {
            // Additional debugging for DE522480. Remove casc-bundle-list and casc-uploaded-bundle-validate lines after July 15th 2023
            sh "java -jar jenkins-cli.jar -auth ${auth} -webSocket casc-bundle-list"
            sh "java -jar jenkins-cli.jar -auth ${auth} -webSocket casc-uploaded-bundle-validate -b ${bundleName}"
            sh """
              java -jar jenkins-cli.jar -auth ${auth} casc-bundle-set-controller -b ${bundleName} -c Teams/${tenantName}
              java -jar jenkins-cli.jar -auth ${auth} -webSocket managed-master-restart Teams/${tenantName}
            """
            sleep 15
          }
        }
      }
    }
  } catch (err) {
    println err
    error 'An error occurred while attempting to give additional rbac permission to controller'
  }
}

// Create the TF secrets in the controller
// https://tfe.dev.fiscicd.com/app/caas/workspaces/tfe-sync-k8s-namespace-secret-for-controllers-internal
def createTenantSecret() {
  enaLogger.debug(msg: 'Start of method createTenantSecret()')

  String tfePayload = '''{
  "data": {
    "attributes": {
      "message": "Create",
      "is-destroy": "false"
    },
    "type": "runs",
    "relationships": {
      "workspace": {
        "data": {
          "type": "workspaces",
          "id": "ws-EruAebEntAvTpFZM"
        }
      }
    }
  }
  }'''

  try {
    // Work around String credentials not being accepted in items.yaml
    withCredentials([usernamePassword(credentialsId: 'TFE_TOKEN_CAAS_ORG', passwordVariable: 'tfe_token', usernameVariable: 'fake_tfe_token_user')]) {
      String cmd = "curl --silent --location --write-out '%{http_code}' -o /dev/null" +
                   " --request POST --url https://tfe.dev.fiscicd.com/api/v2/runs" +
                   " --header 'Authorization: Bearer ${tfe_token}' " +
                   " --header 'Content-Type: application/vnd.api+json' " +
                   " --data '${tfePayload}'"
      int status = sh(returnStdout: true, script: cmd).toInteger()
      if (status != 201) {
        throw new Exception('TFE api did not return a 201 response')
      }
      enaLogger.log(msg: "Namespace secret creation has been triggered successfully in TFE")
    }
  } catch (err) {
    println "${err}\nThere was an issue when triggering namespace secret creation in TFE"
  }

  enaLogger.debug(msg: 'End of method createTenantSecret()')
}

// Create the "welcome" folder inside the root folder for each controller. This is added as a workaround step and will be removed once the Cloudbees fix the bug.

def createFolderInTeamController(tenantName, folderName) {
  enaLogger.debug(msg: 'Start of method createFolderInTeamController()')
  enaLogger.debug(msg: "Parameter tenantName passed in value: ${tenantName}")
  enaLogger.debug(msg: "Parameter folderName passed in value: ${folderName}")

  enaParams.required(name: 'tenantName', value: tenantName, class: String.class)
  enaParams.required(name: 'folderName', value: folderName, class: String.class)

  String tenant_url = "http://${tenantName}.lob-${tenantName}.svc.cluster.local/${tenantName}"
  cjoc.waitForControllerOnline(baseURL: tenant_url, timeout: 600)

  String apiEndpoint = "${tenant_url}/job/${tenantName}/createItem"

  try {
    withCredentials([usernameColonPassword(credentialsId: 'caas-cbj-svc', variable: 'auth')]) {
      sh 'curl -X POST --user "${auth}" -H "Content-Type:application/x-www-form-urlencoded" ' + apiEndpoint +
      " --data name=${folderName}" +
      ' --data mode=com.cloudbees.hudson.plugins.folder.Folder'
    }
  } catch (e) {
    println "Failed to create a new Folder within the newly provisioned controller. Please see the error message below \n ${e}"
  }

  enaLogger.debug(msg: 'End of method createFolderInTeamController()')
}

def sendOnboardingEmail(String email, String teamName) {
  def bccEmails = ['CICD_DL_OPS@fisglobal.com','Jeffrey.Dao@fisglobal.com','Scott.Sylvester@fisglobal.com']
  String command = "aws ses send-templated-email --source 'CaaS <no-reply@fiscicd.com>' --destination 'ToAddresses=${email},BccAddresses=${bccEmails.join(',')}' --template 'CaaSWelcomeTemplate' --template-data '{\"teamName\": \"${teamName}\"}'"

  withCredentials([[
    $class: 'AmazonWebServicesCredentialsBinding',
    credentialsId: 'AWS_ENA-CAAS-1',
    accessKeyVariable: 'AWS_ACCESS_KEY_ID',
    secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
  ]]) {
    sh(command)
  }
}

String getBitbucketBaseApiUrl(Map params = [:]) {
  String url = "${params.host}/rest/${params.apiType}/1.0/${params.path}"
  println(url)
  return url
}

String createRepo(String repoName, String defaultBranch) {
  enaLogger.debug(msg: "Parameter repoName passed in value: ${repoName}")
  enaLogger.debug(msg: "Parameter defaultBranch passed in value: ${defaultBranch}")

  enaParams.required(name: 'repoName', value: repoName, class: String.class)
  enaParams.required(name: 'defaultBranch', value: defaultBranch, class: String.class)

  def formatted = { name -> name.replace(" ", "-").toLowerCase() }
  def forkable = "False"

  String commonRestApiUrl = getBitbucketBaseApiUrl(host: BITBUCKET_URL,
    apiType: 'api',
    path: "projects/${BITBUCKET_PROJECT_KEY}/repos"
  )
  enaLogger.log(msg:"BitBucket API Request endpoint: ${commonRestApiUrl}")

  jsonMap = ['name'         : repoName,
             'scmId'        : 'git',
             'forkable'     : forkable,
             'defaultBranch': defaultBranch
  ]

  String jsonBody = writeJSON returnText: true, json: jsonMap
  enaLogger.log(msg:"BitBucket API Request Body")
  enaLogger.log(msg:"${jsonBody}")

  withCredentials([usernamePassword(credentialsId: JENKINS_CREDENTIALS_ID_RW, usernameVariable: 'BB_USER', passwordVariable: 'BB_PWD')]) {
    String cmd = """curl -s -X POST '${commonRestApiUrl}' -d '${jsonBody}' \
                    -u "${BB_USER}:${BB_PWD}" \
                    -H 'Accept: application/json' -H 'Content-Type: application/json'"""
    String response = sh(returnStdout: true, script: cmd).trim()
    Map result = readJSON(text: response)
    enaLogger.log(msg:"BitBucket API Response Body")
    enaLogger.log(msg:"${result}")

    if (result?.errors) {
      throw new Exception(response)
    }
    return result
  }
}

def addUserToBBRepo(String repoName, String user, String repoPermission){
  enaLogger.debug(msg: "Parameter repoName passed in value: ${repoName}")
  enaLogger.debug(msg: "Parameter user passed in value: ${user}")

  enaParams.required(name: 'repoName', value: repoName, class: String.class)
  enaParams.required(name: 'user', value: user, class: String.class)

  String commonRestApiUrl = getBitbucketBaseApiUrl(host: BITBUCKET_URL,
    apiType: 'api',
    path: "projects/${BITBUCKET_PROJECT_KEY}/repos/${repoName}/permissions/users?name=${user}&permission=${repoPermission}"
  )
  enaLogger.log(msg:"BitBucket API Request endpoint: ${commonRestApiUrl}")

  withCredentials([usernamePassword(credentialsId: JENKINS_CREDENTIALS_ID_RW, usernameVariable: 'BB_USER', passwordVariable: 'BB_PWD')]) {
    String cmd = """curl --silent --location --write-out '%{http_code}' --request PUT '${commonRestApiUrl}' \
                    -u "${BB_USER}:${BB_PWD}"
                 """
    def response = sh(returnStdout: true, script: cmd).trim()
    enaLogger.log("Response from API: ${response}")
    int status = response.drop(response.size() - 3).toInteger()
    if (status != 204) {
      if (status == 404) {
        enaLogger.log(msg: "User ${user} was not found when trying to add them to repo ${repoName}. The user has been notified in the CaaS UI about this with instructions on how to get access to BitBucket and manually get this access.")
        // Mark current stage as unstable
        unstable("Failure to add non-existent user ${user} to repo ${repoName} caused the current stage to be unstable")
      } else {
        throw new Exception("There was an exception adding user ${user} as Administrator to Repository ${repoName}")
      }
    }
  }
}

def addAdminUserToBBRepo(String repoName, String owner){
  enaLogger.debug(msg: "Parameter repoName passed in value: ${repoName}")
  enaLogger.debug(msg: "Parameter owner passed in value: ${owner}")

  enaParams.required(name: 'repoName', value: repoName, class: String.class)
  enaParams.required(name: 'owner', value: owner, class: String.class)

  addUserToBBRepo(repoName, owner, "REPO_ADMIN")
}

def addReadOnlySvcAcctToBBRepo(String repoName, String serviceAccountName){
  enaLogger.debug(msg: "Parameter repoName passed in value: ${repoName}")
  enaLogger.debug(msg: "Parameter serviceAccountName passed in value: ${serviceAccountName}")

  enaParams.required(name: 'repoName', value: repoName, class: String.class)
  enaParams.required(name: 'serviceAccountName', value: serviceAccountName, class: String.class)

  addUserToBBRepo(repoName, serviceAccountName, "REPO_READ")
}

def addPRWebhookToBBRepo(String repoName, String url) {
    enaLogger.debug(msg: "Parameter repoName passed in value: ${repoName}")
    enaLogger.debug(msg: "Parameter url passed in value: ${url}")

    enaParams.required(name: 'repoName', value: repoName, class: String.class)
    enaParams.required(name: 'url', value: url, class: String.class)

    def webhookEvents = ["pr:modified","pr:opened","pr:from_ref_updated"]
    String webhookUrl = "${url}/bitbucket-scmsource-hook/notify?server_url=https%3A%2F%2F${BITBUCKET_HOSTNAME}"
    addWebhookToBBRepo(repoName, "Pull Request - WebHook", webhookEvents, webhookUrl)
}

def addMergedWebhookToBBRepo(String repoName, String teamName) {
    enaLogger.debug(msg: "Parameter repoName passed in value: ${repoName}")
    enaLogger.debug(msg: "Parameter teamName passed in value: ${teamName}")

    enaParams.required(name: 'repoName', value: repoName, class: String.class)
    enaParams.required(name: 'teamName', value: teamName, class: String.class)

    withCredentials([usernamePassword(credentialsId: JENKINS_CREDENTIALS_BB_PR_TOKEN, passwordVariable: 'token', usernameVariable: 'not_used')]) {
        APPROVED_PR_TOKEN = token
    }

    def webhookEvents = ["pr:merged"]
    String webhookUrl = "${CLOUDBEES_EXTERNAL_URL}/${CAAS_CONTROLLER_DOMAIN_NAME}/generic-webhook-trigger/invoke?token=${APPROVED_PR_TOKEN}-${teamName}&controller=${teamName}"
    addWebhookToBBRepo(repoName, "PR Merged - WebHook", webhookEvents, webhookUrl)
}

def addWebhookToBBRepo(String repoName, String webhookName, List events, String url){
  enaLogger.debug(msg: "Parameter repoName passed in value: ${repoName}")
  enaLogger.debug(msg: "Parameter webhookName passed in value: ${webhookName}")
  enaLogger.debug(msg: "Parameter events passed in value: ${events}")
  enaLogger.debug(msg: "Parameter url passed in value: ${url}")

  enaParams.required(name: 'repoName', value: repoName, class: String.class)
  enaParams.required(name: 'webhookName', value: webhookName, class: String.class)
  enaParams.required(name: 'events', value: events, class: List.class)
  enaParams.required(name: 'url', value: url, class: String.class)

  String commonRestApiUrl = getBitbucketBaseApiUrl(host: BITBUCKET_URL,
    apiType: 'api',
    path: "projects/${BITBUCKET_PROJECT_KEY}/repos/${repoName}/webhooks"
  )
  enaLogger.log(msg:"BitBucket API Request endpoint: ${commonRestApiUrl}")

  jsonMap = [
    'name': webhookName,
    'events': events,
    'url': url
  ]

  String jsonBody = writeJSON returnText: true, json: jsonMap
  enaLogger.log(msg:"BitBucket API Request Body")
  enaLogger.log(msg:"${jsonBody}")

  withCredentials([usernamePassword(credentialsId: JENKINS_CREDENTIALS_ID_RW, usernameVariable: 'BB_USER', passwordVariable: 'BB_PWD')]) {
    String cmd = """curl --silent --location --write-out '%{http_code}' --request POST '${commonRestApiUrl}' -d '${jsonBody}' \
                    -u "${BB_USER}:${BB_PWD}" \
                    -H 'Accept: application/json' -H 'Content-Type: application/json'
                 """
    def response = sh(returnStdout: true, script: cmd).trim()
    enaLogger.log("Response from API: ${response}")
    int status = response.drop(response.size() - 3).toInteger()
    if (status != 201) {
      throw new Exception("There was an exception adding webhook ${webhookName} to Repository ${repoName}")
    }
  }
}

def settingControllerFiles(String tenantName, sonarqube_edition, String repoName, String branch, String bundleName) {
  enaLogger.debug(msg: 'Start of method settingControllerFiles()')
  enaLogger.debug(msg: "Parameter tenantName passed in value: ${tenantName}")
  enaLogger.debug(msg: "Parameter sonarqube_edition passed in value: ${sonarqube_edition}")
  enaLogger.debug(msg: "Parameter repoName passed in value: ${repoName}")
  enaLogger.debug(msg: "Parameter branch passed in value: ${branch}")
  enaLogger.debug(msg: "Parameter bundleName passed in value: ${bundleName}")

  enaParams.required(name: 'tenantName', value: tenantName, class: String.class)
  enaParams.required(name: 'sonarqube_edition', value: sonarqube_edition, class: String.class)
  enaParams.required(name: 'repoName', value: repoName, class: String.class)
  enaParams.required(name: 'branch', value: branch, class: String.class)
  enaParams.required(name: 'bundleName', value: bundleName, class: String.class)

  withCredentials([usernameColonPassword(credentialsId: JENKINS_CREDENTIALS_ID_RW, variable: 'auth')]) {
    sh """
          git clone https://${auth}@${BITBUCKET_HOSTNAME}/scm/${BITBUCKET_PROJECT_KEY}/${repoName}.git .
          python3 ../${LOCAL_FOLDER_CLOUDBEES}/controller-creation/create_bundle.py ${tenantName} ${bundleName} ${sonarqube_edition} ${LOCAL_FOLDER_CLOUDBEES}
          git status
          git add ${bundleName}
       """
    writeFile file: 'Jenkinsfile', text: pullRequestJenkinsfileTplString
    sh """
          git status
          git add Jenkinsfile
       """
    enaLogger.log(msg: "Committing and Pushing")
    sh """
          git status
          git config --global user.email "SVCACCT_CAAS_BB_RW@fisglobal.com"
          git commit -m "Onboarding a new controller with the team name: ${tenantName}"
          git push -u origin HEAD:${branch}
       """
  }
  enaLogger.debug(msg: 'End of method settingControllerFiles()')
}

def addCascBundleLocation(String bundleName, String repoName, String branchName){
  enaLogger.debug(msg: 'Start of method addCascBundleLocation()')
  enaLogger.debug(msg: "Parameter bundleName passed in value: ${bundleName}")
  enaLogger.debug(msg: "Parameter repoName passed in value: ${repoName}")
  enaLogger.debug(msg: "Parameter branchName passed in value: ${branchName}")

  enaParams.required(name: 'bundleName', value: bundleName, class: String.class)
  enaParams.required(name: 'repoName', value: repoName, class: String.class)
  enaParams.required(name: 'branchName', value: branchName, class: String.class)

  withCredentials([usernameColonPassword(credentialsId: JENKINS_CREDENTIALS_ID_RW, variable: 'auth')]) {
    // scm checkout
    git branch: "${CJOC_CASC_REPO_BRANCH}", credentialsId: "${JENKINS_CREDENTIALS_ID_RW}", url: "${CJOC_CASC_REPO_URL}"

    def cascBundleFoler = "./${CJOC_CASC_PATH_PREFIX}/caas-${CLUSTER}/casc_bundle"
    dir(cascBundleFoler) {
      // check if the bundle exists
      def bundleExists = false
      if (!fileExists(file: "${CLOUDBEES_JENKINS_YAMLFILE}")) {
        error("File ${CLOUDBEES_JENKINS_YAMLFILE} not exists in folder ${cascBundleFoler}")
      }

      if (!fileExists(file: "${CLOUDBEES_BUNDLE_YAMLFILE}")) {
        error("File ${CLOUDBEES_BUNDLE_YAMLFILE} not exists in folder ${cascBundleFoler}")
      }

      def cascBundleYaml = readYaml file: CLOUDBEES_JENKINS_YAMLFILE
      def bundleData = cascBundleYaml["unclassified"]["bundleStorageService"]["bundles"]
      enaLogger.log(msg: "${bundleData}")

      if(bundleData == null){
        error "Failed to get bundles from ${CLOUDBEES_JENKINS_YAMLFILE} file"
      }

      for(data in bundleData) {
        if(data["name"] == bundleName){
          bundleExists = true
        }
      }

      if(bundleExists) {
        enaLogger.log(msg: "Bundle value found in ${CLOUDBEES_JENKINS_YAMLFILE} file. No changes required in ${CLOUDBEES_JENKINS_YAMLFILE} file")
      } else {
        enaLogger.log(msg: "Bundle value not found in ${CLOUDBEES_JENKINS_YAMLFILE} file. Creating new Bundle value in ${CLOUDBEES_JENKINS_YAMLFILE} file...")
        // create bundle yaml from template
        def bitbucketRepo = "${BITBUCKET_URL}/scm/${BITBUCKET_PROJECT_KEY}/${repoName}.git"
        newBundleString = newBundleTplString.replaceAll("<bundleName>", bundleName).replaceAll("<scmDefaultVersion>", branchName).replaceAll("<gitCredentialsId>", JENKINS_CREDENTIALS_ID_RO).replaceAll("<gitId>",JENKINS_CREDENTIALS_ID_RO).replaceAll("<gitRemoteUrl>", bitbucketRepo)
        def newBundleYaml = readYaml text: newBundleString

        enaLogger.log(msg: "adding bundle to ${CLOUDBEES_JENKINS_YAMLFILE} file")
        enaLogger.log(msg: "${newBundleYaml}")

        // Update the jenkins yaml file
        cascBundleYaml["unclassified"]["bundleStorageService"]["bundles"].add(newBundleYaml)
        writeYaml file: CLOUDBEES_JENKINS_YAMLFILE, data: cascBundleYaml, overwrite: true
        sh "cat ${CLOUDBEES_JENKINS_YAMLFILE}"

        // update the bundle yaml file
        def yamlData = readYaml file: CLOUDBEES_BUNDLE_YAMLFILE
        version = yamlData["version"].toInteger() + 1
        yamlData["version"] = version.toString()
        writeYaml file: CLOUDBEES_BUNDLE_YAMLFILE, data: yamlData, overwrite: true
        sh "cat ${CLOUDBEES_BUNDLE_YAMLFILE}"

        // git push changes
        def gitUsername = ""
        def gitPassword = ""
        withCredentials(
          [usernamePassword(
            credentialsId: JENKINS_CREDENTIALS_ID_RW,
            passwordVariable: 'GIT_PASSWORD',
            usernameVariable: 'GIT_USERNAME')
          ]) {
          gitUsername = GIT_USERNAME
          LinkedHashMap<String, String> specialCharsMap = ['!': '%21', '#': '%23', '$': '%24', '&': '%26', "'": '%27',
                                                          '(': '%28', ')': '%29', '*': '%2A', '+': '%2B', ',': '%2C',
                                                          '/': '%2F', ':': '%3A', ';': '%3B', '=': '%3D', '?': '%3F',
                                                          '@': '%40', '[': '%5B', ']': '%5D']
          char[] specialCharsList = specialCharsMap.keySet()
          GIT_PASSWORD.toCharArray().each {
            gitPassword = (it in specialCharsList) ? gitPassword + specialCharsMap[it.toString()] : gitPassword + it
          }
          withEnv([
            "GIT_AUTHOR_NAME=$gitUsername",
            "GIT_AUTHOR_EMAIL=$gitUsername@fisglobal.com",
            "GIT_COMMITTER_EMAIL=$gitUsername@fisglobal.com",
            "GIT_COMMITTER_NAME=$gitUsername",
            "GIT_USER=$gitUsername",
            "GIT_PASS=$gitPassword",
            "HOST=${BITBUCKET_URL.replaceAll('https://', '')}",
            "REPO_PATH=scm/${CJOC_CASC_PROJECT_KEY}/${CJOC_CASC_REPO_NAME}.git"
          ]) {
            sh(returnStdout: true, script: "git add ${CLOUDBEES_JENKINS_YAMLFILE}")
            sh(returnStdout: true, script: "git add ${CLOUDBEES_BUNDLE_YAMLFILE}")
            sh(returnStdout: true, script: "git commit -m 'Automation - Adding Bundle: ${bundleName} in CJOC System config'")
            wrap(
                  [
                  $class: "MaskPasswordsBuildWrapper",
                  varPasswordPairs: [
                  [password: GIT_USER], [password: GIT_PASS]
                    ]
                  ]){
                command = 'git push https://$GIT_USER:$GIT_PASS@$HOST/$REPO_PATH'
                sh(returnStdout: true, script: command)
                }
            }
        }
      }
    }
  }
  enaLogger.debug(msg: 'End of method addCascBundleLocation()')
}

/**
 * This will update the CJOC config map.
 *
 * @param cjocCredentialsId String containing the cjoc jenkins credentials id
 * @param cloudbeesK8sUrl String containing the internal k8s url to cloudbees
 * @example <pre> {@code
 *   updateCJOCConfigMap(
 *     cjocNamespace: 'cloudbees-sda',
 *     cjocConfigMapName: 'http://cloudbees.k8s.local'
 *   )
 * }</pre>
 */
void updateCJOCConfigMap(Map params = [:]) {
    enaLogger.debug(msg: 'Start of method updateCJOCConfigMap()')
    enaLogger.debug(msg: "Parameter cjocNamespace passed in value: ${params.cjocNamespace}")
    enaLogger.debug(msg: "Parameter cjocConfigMapName passed in value: ${params.cjocConfigMapName}")

    enaParams.required(name: 'cjocNamespace', value: params.cjocNamespace, class: String.class)
    enaParams.required(name: 'cjocConfigMapName', value: params.cjocConfigMapName, class: String.class)

    sh "kubectl create configmap ${params.cjocConfigMapName} --from-file=./casc_bundle/ -n ${params.cjocNamespace} --dry-run=client -o yaml | kubectl apply -f -"

    enaLogger.debug(msg: 'End of method updateCJOCConfigMap()')
}

/**
 * This will setup the approved PR jenkins pipeline.
 *
 * @param bitbucketProjectKey String containing the bitbucket project key
 * @param bitbucketRepoName String containing the bitbucket repository name
 * @param bitbucketROCredentialsId String containing the bitbucket read only jenkins credentials id
 * @param bitbucketUrl String containing the bitbucket url
 * @param cjocCredentialsId String containing the cjoc jenkins credentials id
 * @param cloudbeesK8sUrl String containing the internal k8s url to cloudbees
 * @param jobFolderPath String containing the cjoc folder path in cloudbees where the job should be created
 * @param prTokenCredentialsId String containing the pr token jenkins credentials id
 * @param teamName String containing the team name being on boarded
 * @example <pre> {@code
 *   createApprovedPRPipeline(
 *     bitbucketProjectKey: 'CAASCUST',
 *     bitbucketRepoName: 'my-repo-name',
 *     bitbucketROCredentialsId: 'bitbucket_jenkins_cred_id',
 *     bitbucketUrl: 'https://bitbucket.fis.dev',
 *     cjocCredentialsId: 'cjoc_cloudbees_jenkins_cred_id',
 *     cloudbeesK8sUrl: 'http://cloudbees.k8s.local',
 *     jobFolderPath: 'controller-pr-jobs',
 *     prTokenCredentialsId: 'pr_token_jenkins_cred_id',
 *     teamName: 'team-name'
 *   )
 * }</pre>
 */
void createApprovedPRPipeline(Map params = [:]) {
    enaLogger.debug(msg: 'Start of method createApprovedPRPipeline()')
    enaLogger.debug(msg: "Parameter bitbucketProjectKey passed in value: ${params.bitbucketProjectKey}")
    enaLogger.debug(msg: "Parameter bitbucketRepoName passed in value: ${params.bitbucketRepoName}")
    enaLogger.debug(msg: "Parameter bitbucketROCredentialsId passed in value: ${params.bitbucketROCredentialsId}")
    enaLogger.debug(msg: "Parameter bitbucketUrl passed in value: ${params.bitbucketUrl}")
    enaLogger.debug(msg: "Parameter cjocCredentialsId passed in value: ${params.cjocCredentialsId}")
    enaLogger.debug(msg: "Parameter cloudbeesK8sUrl passed in value: ${params.cloudbeesK8sUrl}")
    enaLogger.debug(msg: "Parameter jobFolderPath passed in value: ${params.jobFolderPath}")
    enaLogger.debug(msg: "Parameter teamName passed in value: ${params.teamName}")

    enaParams.required(name: 'bitbucketProjectKey', value: params.bitbucketProjectKey, class: String.class)
    enaParams.required(name: 'bitbucketRepoName', value: params.bitbucketRepoName, class: String.class)
    enaParams.required(name: 'bitbucketROCredentialsId', value: params.bitbucketROCredentialsId, class: String.class)
    enaParams.required(name: 'bitbucketUrl', value: params.bitbucketUrl, class: String.class)
    enaParams.required(name: 'cjocCredentialsId', value: params.cjocCredentialsId, class: String.class)
    enaParams.required(name: 'cloudbeesK8sUrl', value: params.cloudbeesK8sUrl, class: String.class)
    enaParams.required(name: 'jobFolderPath', value: params.jobFolderPath, class: String.class)
    enaParams.required(name: 'teamName', value: params.teamName, class: String.class)

    String pipelineType = 'multibranch_pipeline'
    String data = "{\"display_name\": \"${params.teamName}\", \"bitbucket_url\":\"${params.bitbucketUrl}\", \"credentials_id\":\"${params.bitbucketROCredentialsId}\", \"repo_owner\": \"${params.bitbucketProjectKey}\", \"repository_name\": \"${params.bitbucketRepoName}\"}"

    String filePath = parseMultibranchPipelineTpl(
        data: data,
        pipelineType: pipelineType
    )
    createJenkinsPipeline(
        cjocCredentialsId: params.cjocCredentialsId,
        cloudbeesK8sUrl: params.cloudbeesK8sUrl,
        filePath: filePath,
        jobFolderPath: params.jobFolderPath,
        pipelineName: "${params.teamName}-approved-pr"
    )
    enaLogger.debug(msg: 'End of method createApprovedPRPipeline()')
}

/**
 * This will setup the merged PR jenkins pipeline.
 *
 * @param cjocCredentialsId String containing the cjoc jenkins credentials id
 * @param cloudbeesK8sUrl String containing the internal k8s url to cloudbees
 * @param jobFolderPath String containing the cjoc folder path in cloudbees where the job should be created
 * @param prTokenCredentialsId String containing the pr token jenkins credentials id
 * @param teamName String containing the team name being on boarded
 * @example <pre> {@code
 *   createMergedPRPipeline(
 *     cjocCredentialsId: 'cjoc_cloudbees_jenkins_cred_id',
 *     cloudbeesK8sUrl: 'http://cloudbees.k8s.local',
 *     jobFolderPath: 'controller-pr-jobs',
 *     prTokenCredentialsId: 'pr_token_jenkins_cred_id',
 *     teamName: 'team-name'
 *   )
 * }</pre>
 */
void createMergedPRPipeline(Map params = [:]) {
    enaLogger.debug(msg: 'Start of method createMergedPRPipeline()')
    enaLogger.debug(msg: "Parameter cjocCredentialsId passed in value: ${params.cjocCredentialsId}")
    enaLogger.debug(msg: "Parameter cloudbeesK8sUrl passed in value: ${params.cloudbeesK8sUrl}")
    enaLogger.debug(msg: "Parameter jobFolderPath passed in value: ${params.jobFolderPath}")
    enaLogger.debug(msg: "Parameter prTokenCredentialsId passed in value: ${params.prTokenCredentialsId}")
    enaLogger.debug(msg: "Parameter teamName passed in value: ${params.teamName}")

    enaParams.required(name: 'cjocCredentialsId', value: params.cjocCredentialsId, class: String.class)
    enaParams.required(name: 'cloudbeesK8sUrl', value: params.cloudbeesK8sUrl, class: String.class)
    enaParams.required(name: 'jobFolderPath', value: params.jobFolderPath, class: String.class)
    enaParams.required(name: 'prTokenCredentialsId', value: params.prTokenCredentialsId, class: String.class)
    enaParams.required(name: 'teamName', value: params.teamName, class: String.class)

    withCredentials([usernamePassword(credentialsId: params.prTokenCredentialsId, passwordVariable: 'approved_pr_token', usernameVariable: 'not_used')]) {
        String pipelineType = 'generic_webhook_trigger_pipeline'
        String data = "{\"display_name\": \"${params.teamName}\", \"cloudbees_url\":\"${params.cloudbeesK8sUrl}\", \"pr_token\":\"${approved_pr_token}-${params.teamName}\"}"

        String filePath = parseMultibranchPipelineTpl(
            data: data,
            pipelineType: pipelineType
        )
        createJenkinsPipeline(
            cjocCredentialsId: params.cjocCredentialsId,
            cloudbeesK8sUrl: params.cloudbeesK8sUrl,
            filePath: filePath,
            jobFolderPath: params.jobFolderPath,
            pipelineName: "${params.teamName}-merged-pr"
        )
    }
  enaLogger.debug(msg: 'End of method createMergedPRPipeline()')
}

/**
 * This will create a jenkins pipeline.
 *
 * @param cjocCredentialsId String containing the cjoc jenkins credentials id
 * @param cloudbeesK8sUrl String containing the internal k8s url to cloudbees
 * @param filePath String containing the file path that has the configuration for the pipeline being created
 * @param jobFolderPath String containing the cjoc folder path in cloudbees where the job should be created
 * @param pipelineName String the name of the pipeline to be create
 * @example <pre> {@code
 *   createJenkinsPipeline(
 *     cjocCredentialsId: 'cjoc_cloudbees_jenkins_cred_id',
 *     cloudbeesK8sUrl: 'http://cloudbees.k8s.local',
 *     filePath: './test.xml',
 *     jobFolderPath: 'controller-pr-jobs',
 *     pipelineName: 'pipeline_name'
 *   )
 * }</pre>
 */
void createJenkinsPipeline(Map params = [:]) {
    enaLogger.debug(msg: 'Start of method createJenkinsPipeline()')
    enaLogger.debug(msg: "Parameter cjocCredentialsId passed in value: ${params.cjocCredentialsId}")
    enaLogger.debug(msg: "Parameter cloudbeesK8sUrl passed in value: ${params.cloudbeesK8sUrl}")
    enaLogger.debug(msg: "Parameter filePath passed in value: ${params.filePath}")
    enaLogger.debug(msg: "Parameter jobFolderPath passed in value: ${params.jobFolderPath}")
    enaLogger.debug(msg: "Parameter pipelineName passed in value: ${params.pipelineName}")

    enaParams.required(name: 'cjocCredentialsId', value: params.cjocCredentialsId, class: String.class)
    enaParams.required(name: 'cloudbeesK8sUrl', value: params.cloudbeesK8sUrl, class: String.class)
    enaParams.required(name: 'filePath', value: params.filePath, class: String.class)
    enaParams.required(name: 'jobFolderPath', value: params.jobFolderPath, class: String.class)
    enaParams.required(name: 'pipelineName', value: params.pipelineName, class: String.class)

    String commonRestApiUrl = "${params.cloudbeesK8sUrl}/${CAAS_CONTROLLER_DOMAIN_NAME}/job/${params.jobFolderPath}/createItem?name=${params.pipelineName}"
    enaLogger.log(msg: "CloudBees API Request endpoint: ${commonRestApiUrl}")

    withCredentials([usernameColonPassword(credentialsId: params.cjocCredentialsId, variable: 'cloudbees_auth')]) {
        String cmd = "curl --silent --location --write-out '%{http_code}' --request POST '${commonRestApiUrl}'"
        cmd += " --header 'content-type: application/xml' --data-binary '@${params.filePath}'"
        cmd += ' -u "${cloudbees_auth}"' //Proper string interpolation for secrets

        retry(10) {
            sleep(time: 5, unit: 'SECONDS')  // likely unneeded when using k8s internal service urls

            def response = sh(returnStdout: true, script: cmd).trim()
            enaLogger.log(msg: "Response from API: ${response}")

            int status = response.drop(response.size() - 3).toInteger()
            if (status != 200) {
                throw new Exception("There was an error while trying to create a jenkins pipeline. The http response code returned from cloudbees was ${status}")
            } else {
                enaLogger.log(msg: "The creation of Jenkins Pipeline was successful")
            }
        }
    }
    enaLogger.debug(msg: 'End of method createJenkinsPipeline()')
}

/**
 * This creates a multibracnh pipeline configuration from a template.
 *
 * @param data String containing the cjoc jenkins credentials id
 * @param pipelineType String containing the internal k8s url to cloudbees
 * @return String returns the pipeline configuration xml file path
 * @example <pre> {@code
 *   String filePath = parseMultibranchPipelineTpl(
 *     data: 'cjoc_cloudbees_jenkins_cred_id',
 *     pipelineType: 'http://cloudbees.k8s.local'
 *   )
 * }</pre>
 */
String parseMultibranchPipelineTpl(Map params = [:]) {
    enaLogger.debug(msg: 'Start of method parseMultibranchPipelineTpl()')
    enaLogger.debug(msg: "Parameter data passed in value: ${params.data}")
    enaLogger.debug(msg: "Parameter pipelineType passed in value: ${params.pipelineType}")

    enaParams.required(name: 'data', value: params.data, class: String.class)
    enaParams.required(name: 'pipelineType', value: params.pipelineType, class: String.class)

    String templatePath = "./controller-creation/template_jenkins/${params.pipelineType}.xml.tpl"
    String outputPath = "./controller-creation/${params.pipelineType}.xml"

    enaLogger.debug(msg: "${params.data}")

    sh """
        python3 ./controller-creation/template_jenkins/parse_multibranch_pipeline.py ${templatePath} ${outputPath} \'${params.data}\'
        cat ${outputPath}
       """

    enaLogger.debug(msg: 'End of method parseMultibranchPipelineTpl()')
    return outputPath
}

/**
 * Onboards a tenant to SonarQube.
 *
 * @param tenant String name of tenant
 * @param edition String edition of Sonarqube (options: SonarQube_Community_Edition, SonarQube_Enterprise_Edition)
 * @param branch String branch of sonarqube-onboard repo to use
 * @param cloudbeesUrl String url of cloudbees
 * @example <pre> {@code
 *   sonarqubeOnboard(
 *     tenant: 'zebra',
 *     edition: 'SonarQube_Community_Edition',
 *     branch: 'main',
 *     cloudbeesUrl: 'http://zebra.lob-zebra.svc.cluster.local'
 *   )
 * }</pre>
 */
def sonarqubeOnboard(Map params = [:]) {
    enaLogger.debug(msg: 'Start of method sonarqubeOnboard()')
    enaLogger.debug(msg: "Parameter tenant passed in value: ${params.tenant}")
    enaLogger.debug(msg: "Parameter edition passed in value: ${params.edition}")
    enaLogger.debug(msg: "Parameter branch passed in value: ${params.branch}")
    enaLogger.debug(msg: "Parameter cloudbeesUrl passed in value: ${params.cloudbeesUrl}")

    enaParams.required(name: 'tenant', value: params.tenant, class: String.class)
    enaParams.required(name: 'edition', value: params.edition, class: String.class)
    enaParams.required(name: 'branch', value: params.branch, class: String.class)
    enaParams.required(name: 'cloudbeesUrl', value: params.cloudbeesUrl, class: String.class)

    enaLogger.debug(msg: "Sonarqube - onboarding tenant: ${params.tenant}, edition: ${params.edition}")
    String sonarqubeUrl = SONARQUBE_COMMUNITY_URL  // default to community (no license charge)
    if (params.edition.equals('SonarQube_Enterprise_Edition')) {
        sonarqubeUrl = SONARQUBE_ENTERPRISE_URL
    }

    dir('sonarqube') {
        checkout([$class: 'GitSCM', branches: [[name: params.branch]], extensions: [], userRemoteConfigs: [[credentialsId: 'SVCACCT_CICDLIBS_RO', url: 'https://bitbucket.fis.dev/scm/caas/sonarqube-onboard.git']]])

        withCredentials([
            usernamePassword(credentialsId: 'cloudbees-api-token', passwordVariable: 'jenkins_admin_token', usernameVariable: 'jenkins_user'),
            usernamePassword(credentialsId: 'service-acct-admin', passwordVariable: 'sonar_password', usernameVariable: 'sonar_user')
        ]) {
            sh 'ansible-playbook main.yml \
                --extra-vars "onboard_action=present" \
                --extra-vars "team_name=' + params.tenant + '" \
                --extra-vars "sonar_server=' + sonarqubeUrl +'" \
                --extra-vars "cloudbees_url=' + params.cloudbeesUrl + '" \
                --extra-vars "sonarqube_credential_id=sonarqube" \
                --extra-vars "jenkins_admin_user=${jenkins_user}" \
                --extra-vars "jenkins_admin_token=${jenkins_admin_token}" \
                --extra-vars "sonar_password=${sonar_password}"'
        }
    }

    enaLogger.debug(msg: 'End of method sonarqubeOnboard()')
}
