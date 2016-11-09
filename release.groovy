#!/usr/bin/groovy
def updateDependencies(source){

  def properties = []
  properties << ['<fabric8.version>','io/fabric8/kubernetes-api']

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

def updateDownstreamDependencies(stagedProject) {
  pushPomPropertyChangePR {
    propertyName = 'fabric8.archetypes.release.version'
    projects = [
            'fabric8io/fabric8-maven-dependencies',
            'fabric8io/fabric8-forge',
            'fabric8io/django'
    ]
    version = stagedProject[1]
  }
}


def drop(project, prId){
  dropProject{
    stagedProject = project
    pullRequestId = prId
  }
}
return this;
