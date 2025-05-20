def gitRepoURL() {
  return sh(script: 'git config --get remote.origin.url', returnStdout: true)?.trim()
}

def gitBranchName() {
  try {
    return "${BRANCH_NAME}"
  } catch (Exception e) {
    def branch = sh(script: 'git symbolic-ref --short HEAD || git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()

    if (branch.contains('remotes/origin/')) {
      return branch.replaceAll('^remotes/origin/', '')
    } else if (branch.contains('refs/heads/')) {
      return branch.replaceAll('^refs/heads/', '')
    }
    return branch
  }
}

def gitCommitSha() {
  return sh(script: 'git rev-parse HEAD', returnStdout: true)?.trim()
}

def isTag(String desc) {
  def match = desc =~ /.+-[0-9]+-g[0-9A-Fa-f]{6,}$/
  def result = !match
  match = null // prevent serialization
  return result
}

def gitTagName() {
  try {
    return "${TAG_NAME}"
  } catch (Exception e) {
    def commit = gitCommitSha()
    if (commit) {
      try {
        def desc = sh(script: "git describe --tags ${commit}", returnStdout: true)?.trim()
        if (isTag(desc)) {
          return desc
        }
      } catch (Exception ee) {
        return null
      }
    }
    return null
  }
}

def getBranchName() {
  if (fileExists("jenkins-branch-name")) {
    return readFile("jenkins-branch-name").trim()
  }
  return null
}

def setBranchName(String data) {
  return writeFile(file: "jenkins-branch-name", text: data)
}

def getCommitSha() {
  if (fileExists("jenkins-commit-sha")) {
    return readFile("jenkins-commit-sha").trim()
  }
  return null
}

def setCommitSha(String data) {
  return writeFile(file: "jenkins-commit-sha", text: data)
}

def getRepoURL() {
  if (fileExists("jenkins-repository-url")) {
    return readFile("jenkins-repository-url").trim()
  }
  return null
}

def setRepoURL(String data) {
  return writeFile(file: "jenkins-repository-url", text: data)
}

def getTagName() {
  if (fileExists("jenkins-tag-name")) {
    return readFile("jenkins-tag-name").trim()
  }
  return null
}

def setTagName(String data) {
  return writeFile(file: "jenkins-tag-name", text: data)
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
