def testBuild(String subdir, String image, String dockerfile) {
  docker.build(image, "${WORKSPACE}/${subdir} -f ${WORKSPACE}/${subdir}/${dockerfile}")
}

def testUnit(String subdir, String image) {
  docker.image("${image}").inside("--user root") {
    sh """
      cd /app
      mvn test -Dgroups=unit
      cp -r /app/target ${WORKSPACE}/${subdir}/target
      chown 1000:1000 -R ${WORKSPACE}/${subdir}/target
    """
  }
  archiveArtifacts(artifacts: "${subdir}/target/surefire-reports/*.xml", allowEmptyArchive: true)
  archiveArtifacts(artifacts: "${subdir}/target/site/jacoco/jacoco.xml", allowEmptyArchive: true)
  junit("${subdir}/target/surefire-reports/*.xml", )
  recordCoverage(tools: [[parser: 'JACOCO', pattern: "${subdir}/target/site/jacoco/jacoco.xml"]],
    id: 'account-jacoco-unit',
    name: 'Account JaCoCo Unit Coverage',
    sourceDirectories: [[path: "${subdir}/src/main/java"]])
}

def testIntegration(String subdir, String image) {
  docker.image("${image}").inside("--user root -v /var/run/docker.sock:/var/run/docker.sock") {
    sh """
      cd /app
      mvn test -Dgroups=integration
      cp -r /app/target ${WORKSPACE}/${subdir}/target
      chown 1000:1000 -R ${WORKSPACE}/${subdir}/target
    """
  }
  archiveArtifacts(artifacts: "${subdir}/target/surefire-reports/*.xml", allowEmptyArchive: true)
  archiveArtifacts(artifacts: "${subdir}/target/site/jacoco/jacoco.xml", allowEmptyArchive: true)
  junit("${subdir}/target/surefire-reports/*.xml")
  recordCoverage(tools: [[parser: 'JACOCO', pattern: "${subdir}/target/site/jacoco/jacoco.xml"]],
    id: 'account-jacoco-integration',
    name: 'Account JaCoCo Integration Coverage',
    sourceDirectories: [[path: "${subdir}/src/main/java"]])
}

def build(String subdir, String image, String tag) {
  docker.build("${image}:${tag}", "${WORKSPACE}/${subdir}")
}

def registryPush(String registryURL, String credId, String image, String tag, List<String> targets) {
  docker.withRegistry(registryURL, credId) {
    for (target in targets) {
      docker.image("${image}:${tag}").push("${target}")
    }
  }
}

return this
