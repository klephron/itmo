vms = {
  pools = {
    images = {
      name = "ahla-cluster-images"
      path = "/opt/ahla/vms-cluster/images"
    }
    disks = {
      name = "ahla-cluster-disks"
      path = "/opt/ahla/vms-cluster/disks"
    }
  }
  vms = {
    ubuntu = {
      base = "/home/nikit/vms/images/noble-server-cloudimg-amd64.img"
      settings = {
        "ahla-cluster-manager-1" = {
          memory    = 1024 * 4
          vcpu      = 4
          disk_size = 30
          hostfwd   = []
        }
        "ahla-cluster-worker-1" = {
          memory    = 1024 * 1
          vcpu      = 2
          disk_size = 30
          hostfwd   = []
        }
        "ahla-cluster-worker-2" = {
          memory    = 1024 * 1
          vcpu      = 2
          disk_size = 30
          hostfwd   = []
        }
      }
    }
  }
}
