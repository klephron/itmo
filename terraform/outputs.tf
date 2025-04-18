output "vms" {
  value = {
    ubuntu_20_04 = module.ubuntu_20_04.vms
  }
}
