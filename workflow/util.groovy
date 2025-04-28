def gitBranchName() {
  def branch = sh(script: 'git symbolic-ref HEAD || git name-rev --name-only HEAD', returnStdout: true).trim()

  if (branch.contains('remotes/origin/')) {
    return branch.replaceAll('^remotes/origin/', '')
  } else if (branch.contains('refs/heads/')) {
    return branch.replaceAll('^refs/heads/', '')
  }
  return branch
}

def gitCommitSha() {
  return sh(script: 'git rev-parse HEAD', returnStdout: true)?.trim()
}

def gitRepoURL() {
  return sh(script: 'git config --get remote.origin.url', returnStdout: true)?.trim()
}

def isTag(String desc) {
    def match = desc =~ /.+-[0-9]+-g[0-9A-Fa-f]{6,}$/
    def result = !match
    match = null // prevent serialization
    return result
}

def gitTagName() {
  def commit = gitCommitSha()
  if (commit) {
      def desc = sh(script: "git describe --tags ${commit}", returnStdout: true)?.trim()
      if (isTag(desc)) {
          return desc
      }
  }
  return null
}

def getBranchName() {
  if (fileExists("jenkins-branch-name")) {
    return readFile("jenkins-branch-name").trim()
  }
  return null
}

def getCommitSha() {
  if (fileExists("jenkins-commit-sha")) {
    return readFile("jenkins-commit-sha").trim()
  }
  return null
}

def getRepoURL() {
  if (fileExists("jenkins-repository-url")) {
    return readFile("jenkins-repository-url").trim()
  }
  return null
}

def getTagName() {
  if (fileExists("jenkins-tag-name")) {
    return readFile("jenkins-tag-name").trim()
  }
  return null
}

def setPipelineStatus(String message, String state) {
  def repoURL = getRepoURL()
  def commitSha = getCommitSha()
  step([
    $class: 'GitHubCommitStatusSetter',
    reposSource: [$class: "ManuallyEnteredRepositorySource", url: repoURL],
    commitShaSource: [$class: "ManuallyEnteredShaSource", sha: commitSha],
    errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
    statusResultSource: [$class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]]]
  ])
}

return this
