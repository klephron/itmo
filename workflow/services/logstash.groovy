def build(String subdir, String image, String tag) {
  docker.build("${image}:${tag}", "${WORKSPACE}/${subdir}")
}

def registryPush(String registryURL, String credId, String image, String tag, String[] targets) {
  docker.withRegistry(registryURL, credId) {
    for (target in targets) {
      docker.image("${image}:${tag}").push("${target}")
    }
  }
}

return this
