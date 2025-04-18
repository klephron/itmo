module "pools" {
  source = "./pools"

  images = {
    name = var.config.pools.images.name
    path = var.config.pools.images.path
  }
  disks = {
    name = var.config.pools.disks.name
    path = var.config.pools.disks.path
  }
}

module "ubuntu_20_04" {
  source = "./ubuntu_20_04"

  images = {
    name = module.pools.images.name
  }
  disks = {
    name = module.pools.disks.name
  }
  base = {
    source = var.config.vms.ubuntu_20_04.base
  }
  settings = var.config.vms.ubuntu_20_04.settings
}
