<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:qemu="http://libvirt.org/schemas/domain/qemu/1.0">
  <xsl:output omit-xml-declaration="yes" indent="yes" />
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*" />
    </xsl:copy>
  </xsl:template>
  <xsl:template match="/domain">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
      <qemu:commandline>
        <qemu:arg value="-netdev" />
        <qemu:arg value="user,id=net1,hostfwd=tcp::8100-:8100,hostfwd=tcp::8101-:8101" />
        <qemu:arg value="-device" />
        <qemu:arg value="virtio-net-pci,netdev=net1,bus=pci.0,addr=0x10" />
      </qemu:commandline>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
