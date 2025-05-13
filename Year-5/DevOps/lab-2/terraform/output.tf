output "ubuntu_20_04_1_name" {
  value       = libvirt_domain.ubuntu_20_04_1.name
  description = "VM name"
}

output "ubuntu_20_04_1_ip" {
  value       = libvirt_domain.ubuntu_20_04_1.network_interface[0].addresses.0
  description = "Interface IPs"
}
