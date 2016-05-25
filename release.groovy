#!/usr/bin/groovy
def updateDependencies(source){

  def properties = []
  properties << ['<fabric8.version>','io/fabric8/kubernetes-api']
  properties << ['<docker.maven.plugin.version>','io/fabric8/docker-maven-plugin']

  updatePropertyVersion{
    updates = properties
    repository = source
    project = 'fabric8io/ipaas-quickstarts'
  }
}

def stage(){
  return stageProject{
    project = 'fabric8io/ipaas-quickstarts'
    useGitTagForNextVersion = true
  }
}

def approveRelease(project){
  def releaseVersion = project[1]
  approve{
    room = null
    version = releaseVersion
    console = null
    environment = 'fabric8'
  }
}

def release(project){
  releaseProject{
    stagedProject = project
    useGitTagForNextVersion = true
    helmPush = false
    groupId = 'io.fabric8.archetypes'
    githubOrganisation = 'fabric8io'
    artifactIdToWatchInCentral = 'archetypes-catalog'
    artifactExtensionToWatchInCentral = 'jar'
  }
}

def mergePullRequest(prId){
  mergeAndWaitForPullRequest{
    project = 'fabric8io/ipaas-quickstarts'
    pullRequestId = prId
  }
}

def drop(project, prId){
  dropProject{
    stagedProject = project
    pullRequestId = prId
  }
}
return this;
