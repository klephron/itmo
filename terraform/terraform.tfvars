config = {
  pools = {
    images = {
      name = "devops-images"
      path = "/opt/devops/vms/images"
    }
    disks = {
      name = "devops-disks"
      path = "/opt/devops/vms/disks"
    }
  }
  vms = {
    ubuntu_20_04 = {
      base = "/home/nikit/vms/images/focal-server-cloudimg-amd64-disk-kvm.img"
      settings = {
        "devops-control-1" = {
          memory    = 3072
          vcpu      = 4
          disk_size = 30
        }
        "devops-worker-1" = {
          memory    = 2048
          vcpu      = 2
          disk_size = 30
        }
        "devops-worker-2" = {
          memory    = 2048
          vcpu      = 2
          disk_size = 30
        }
        "devops-metrics-1" = {
          memory    = 1024
          vcpu      = 2
          disk_size = 30
        }
      }
    }
  }
}
