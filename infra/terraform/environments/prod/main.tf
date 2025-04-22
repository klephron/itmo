module "vm" {
  source = "../../modules/vm"
  vms = {
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
      ubuntu = {
        base = "/home/nikit/vms/images/noble-server-cloudimg-amd64.img"
        settings = {
          "devops-control-1" = {
            memory    = 3072
            vcpu      = 4
            disk_size = 30
            hostfwd   = []
          }
          "devops-worker-1" = {
            memory    = 2048
            vcpu      = 2
            disk_size = 30
            hostfwd   = []
          }
          "devops-worker-2" = {
            memory    = 2048
            vcpu      = 2
            disk_size = 30
            hostfwd   = []
          }
          "devops-metrics-1" = {
            memory    = 1024
            vcpu      = 2
            disk_size = 30
            hostfwd = [
              {
                from = 8113
                to   = 8113
              },
              {
                from = 8114
                to   = 8114
              }
            ]
          }
        }
      }
    }
  }
}
