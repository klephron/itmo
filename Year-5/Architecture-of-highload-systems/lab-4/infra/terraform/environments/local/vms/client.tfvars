vms = {
  pools = {
    images = {
      name = "ahla-client-images"
      path = "/opt/ahla/vms-client/images"
    }
    disks = {
      name = "ahla-client-disks"
      path = "/opt/ahla/vms-client/disks"
    }
  }
  vms = {
    ubuntu = {
      base = "/home/nikit/vms/images/noble-server-cloudimg-amd64.img"
      settings = {
        "ahla-client-1" = {
          memory    = 1024 * 2
          vcpu      = 4
          disk_size = 30
          hostfwd   = []
        }
      }
    }
  }
}
