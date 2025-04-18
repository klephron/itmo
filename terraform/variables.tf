variable "config" {
  type = object({
    pools = object({
      images = object({
        name = string
        path = string
      })
      disks = object({
        name = string
        path = string
      })
    })
    vms = object({
      ubuntu_20_04 = object({
        base = string
        settings = map(object({
          memory    = number
          vcpu      = number
          disk_size = number
        }))
      })
    })
  })
}
