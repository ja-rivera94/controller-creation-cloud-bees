<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job">
    <actions>
        <org.jenkinsci.plugins.workflow.multibranch.JobPropertyTrackerAction plugin="workflow-multibranch">
            <jobPropertyDescriptors>
                <string>hudson.model.ParametersDefinitionProperty</string>
            </jobPropertyDescriptors>
        </org.jenkinsci.plugins.workflow.multibranch.JobPropertyTrackerAction>
    </actions>
    <description/>
    <displayName>{{ display_name }} - Pull Request merged</displayName>
    <keepDependencies>false</keepDependencies>
    <properties>
        <hudson.model.ParametersDefinitionProperty>
            <parameterDefinitions>
                <hudson.model.StringParameterDefinition>
                    <name>controller</name>
                    <description>The controller to restart</description>
                    <defaultValue>NONE</defaultValue>
                    <trim>true</trim>
                </hudson.model.StringParameterDefinition>
                <hudson.model.StringParameterDefinition>
                    <name>cloudbees_url</name>
                    <defaultValue>{{ cloudbees_url }}</defaultValue>
                    <trim>false</trim>
                </hudson.model.StringParameterDefinition>
            </parameterDefinitions>
        </hudson.model.ParametersDefinitionProperty>
        <org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
            <triggers>
                <org.jenkinsci.plugins.gwt.GenericTrigger plugin="generic-webhook-trigger">
                    <spec/>
                    <regexpFilterText/>
                    <regexpFilterExpression/>
                    <genericRequestVariables>
                        <org.jenkinsci.plugins.gwt.GenericRequestVariable>
                            <key>controller</key>
                            <regexpFilter/>
                        </org.jenkinsci.plugins.gwt.GenericRequestVariable>
                    </genericRequestVariables>
                    <printPostContent>false</printPostContent>
                    <printContributedVariables>false</printContributedVariables>
                    <causeString>PR merged</causeString>
                    <token>{{ pr_token }}</token>
                    <tokenCredentialId/>
                    <silentResponse>false</silentResponse>
                    <overrideQuietPeriod>false</overrideQuietPeriod>
                    <shouldNotFlattern>false</shouldNotFlattern>
                    <allowSeveralTriggersPerBuild>false</allowSeveralTriggersPerBuild>
                </org.jenkinsci.plugins.gwt.GenericTrigger>
            </triggers>
        </org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
    </properties>
    <definition class="org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition" plugin="workflow-cps">
        <script>@Library(['common-lib', 'caas-shared-lib']) _
        cloubeesRestartControllerPipeline(params)</script>
        <sandbox>true</sandbox>
    </definition>
    <triggers/>
    <disabled>false</disabled>
</flow-definition>