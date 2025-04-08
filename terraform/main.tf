resource "libvirt_pool" "images" {
  name = "devops-images"
  type = "dir"
  target {
    path = "${var.pool_path_prefix}/images"
  }
}

resource "libvirt_pool" "disks" {
  name = "devops-disks"
  type = "dir"
  target {
    path = "${var.pool_path_prefix}/disks"
  }
}

resource "libvirt_volume" "ubuntu_20_04" {
  name   = "ubuntu-20.04.qcow2"
  format = "qcow2"
  pool   = libvirt_pool.images.name
  source = "https://cloud-images.ubuntu.com/focal/current/focal-server-cloudimg-amd64-disk-kvm.img"
}

data "template_file" "ubuntu_20_04_user_data" {
  template = file("${path.module}/ubuntu_20_04/cloud_init.cfg")
}

data "template_file" "ubuntu_20_04_network_config" {
  template = file("${path.module}/ubuntu_20_04/network_config.cfg")
}

resource "libvirt_cloudinit_disk" "ubuntu_20_04_commoninit" {
  name           = "commoninit.iso"
  user_data      = data.template_file.ubuntu_20_04_user_data.rendered
  network_config = data.template_file.ubuntu_20_04_network_config.rendered
  pool           = libvirt_pool.images.name
}

resource "libvirt_volume" "ubuntu_20_04_1" {
  name           = "devops-ubuntu-20.04-1.qcow2"
  pool           = libvirt_pool.disks.name
  base_volume_id = libvirt_volume.ubuntu_20_04.id
  size           = 30 * 1024 * 1024 * 1024
}

resource "libvirt_domain" "ubuntu_20_04_1" {
  name   = "devops-ubuntu-20.04-1"
  vcpu   = 4
  memory = 2048

  cpu {
    mode = "host-passthrough"
  }

  cloudinit = libvirt_cloudinit_disk.ubuntu_20_04_commoninit.id

  disk {
    volume_id = libvirt_volume.ubuntu_20_04_1.id
  }

  network_interface {
    network_name   = "default"
    wait_for_lease = true
  }

  console {
    type        = "pty"
    target_type = "serial"
    target_port = "0"
  }

  graphics {
    type        = "vnc"
    listen_type = "address"
  }

  xml {
    xslt = file("${path.module}/ubuntu_20_04/hostfwd.xsl")
  }
}
