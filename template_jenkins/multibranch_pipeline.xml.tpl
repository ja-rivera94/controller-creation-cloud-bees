<?xml version='1.1' encoding='UTF-8'?>
<org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject plugin="workflow-multibranch">
    <actions/>
    <description></description>
    <displayName>{{ display_name }} - Pull Request approved</displayName>
    <properties>
        <com.cloudbees.hudson.plugins.folder.properties.EnvVarsFolderProperty plugin="cloudbees-folders-plus">
            <properties></properties>
        </com.cloudbees.hudson.plugins.folder.properties.EnvVarsFolderProperty>
    </properties>
    <folderViews class="jenkins.branch.MultiBranchProjectViewHolder" plugin="branch-api">
        <owner class="org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject" reference="../.."/>
    </folderViews>
    <healthMetrics/>
    <icon class="jenkins.branch.MetadataActionFolderIcon" plugin="branch-api">
        <owner class="org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject" reference="../.."/>
    </icon>
    <orphanedItemStrategy class="com.cloudbees.hudson.plugins.folder.computed.DefaultOrphanedItemStrategy" plugin="cloudbees-folder">
        <pruneDeadBranches>true</pruneDeadBranches>
        <daysToKeep>-1</daysToKeep>
        <numToKeep>-1</numToKeep>
        <abortBuilds>false</abortBuilds>
    </orphanedItemStrategy>
    <triggers/>
    <disabled>false</disabled>
    <sources class="jenkins.branch.MultiBranchProject$BranchSourceList" plugin="branch-api">
        <data>
            <jenkins.branch.BranchSource>
                <source class="com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSource" plugin="cloudbees-bitbucket-branch-source">
                    <serverUrl>{{ bitbucket_url }}</serverUrl>
                    <credentialsId>{{ credentials_id }}</credentialsId>
                    <repoOwner>{{ repo_owner }}</repoOwner>
                    <repository>{{ repository_name }}</repository>
                    <traits>
                        <com.cloudbees.jenkins.plugins.bitbucket.BranchDiscoveryTrait>
                            <strategyId>1</strategyId>
                        </com.cloudbees.jenkins.plugins.bitbucket.BranchDiscoveryTrait>
                        <com.cloudbees.jenkins.plugins.bitbucket.OriginPullRequestDiscoveryTrait>
                            <strategyId>1</strategyId>
                        </com.cloudbees.jenkins.plugins.bitbucket.OriginPullRequestDiscoveryTrait>
                        <com.cloudbees.jenkins.plugins.bitbucket.ForkPullRequestDiscoveryTrait>
                            <strategyId>1</strategyId>
                            <trust class="com.cloudbees.jenkins.plugins.bitbucket.ForkPullRequestDiscoveryTrait$TrustTeamForks"/>
                        </com.cloudbees.jenkins.plugins.bitbucket.ForkPullRequestDiscoveryTrait>
                    </traits>
                </source>
                <strategy class="jenkins.branch.DefaultBranchPropertyStrategy">
                    <properties class="empty-list"/>
                </strategy>
            </jenkins.branch.BranchSource>
        </data>
        <owner class="org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject" reference="../.."/>
    </sources>
    <factory class="org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory">
        <owner class="org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject" reference="../.."/>
        <scriptPath>Jenkinsfile</scriptPath>
    </factory>
</org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject>