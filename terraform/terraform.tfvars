config = {
  pools = {
    images = {
      name = "studies-images"
      path = "/opt/studies/vms/images"
    }
    disks = {
      name = "studies-disks"
      path = "/opt/studies/vms/disks"
    }
  }
  vms = {
    ubuntu_20_04 = {
      base = "/home/nikit/vms/images/focal-server-cloudimg-amd64-disk-kvm.img"
      settings = {
        "studies-control-1" = {
          memory    = 3072
          vcpu      = 4
          disk_size = 30
        }
        "studies-worker-1" = {
          memory    = 2048
          vcpu      = 2
          disk_size = 30
        }
        "studies-worker-2" = {
          memory    = 2048
          vcpu      = 2
          disk_size = 30
        }
      }
    }
  }
}
