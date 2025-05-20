vms = {
  pools = {
    images = {
      name = "ahla-single-images"
      path = "/opt/ahla/vms-single/images"
    }
    disks = {
      name = "ahla-single-disks"
      path = "/opt/ahla/vms-single/disks"
    }
  }
  vms = {
    ubuntu = {
      base = "/home/nikit/vms/images/noble-server-cloudimg-amd64.img"
      settings = {
        "ahla-single-1" = {
          memory    = 1024 * 6
          vcpu      = 8
          disk_size = 30
          hostfwd   = []
        }
      }
    }
  }
}
