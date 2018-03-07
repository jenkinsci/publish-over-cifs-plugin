// Namespaces
xml = namespace("http://www.w3.org/XML/1998/namespace")
j = namespace("jelly:core")
f = namespace("/lib/form")


def m = descriptor.hostConfigurationFieldNames
def helpUrl = "/plugin/publish-over-cifs/help/global/"
def defaultPort = jenkins.plugins.publish_over_cifs.CifsHostConfiguration.getDefaultPort()
def defaultTimeout = jenkins.plugins.publish_over_cifs.CifsHostConfiguration.getDefaultTimeout()
def defaultBufferSize = jenkins.plugins.publish_over_cifs.CifsHostConfiguration.getDefaultBufferSize()

f.section(description: _("hostconfig.section.description"), title: _("hostconfig.section.title")) {
  f.entry(title: _("hostconfig.entry")) {
    f.repeatable(var: "instance", header: _("hostconfig.dragAndDrop"), items: descriptor.hostConfigurations) {
      table(width: "100%") {
        f.entry(help: "${helpUrl}name.html", title: m.name()) {
          f.textbox(name: "_.name", checkUrl: "${descriptor.getCheckUrl('name')}+'?value='+escape(this.value)", value: instance?.name)
        }
        f.entry(help: "${helpUrl}hostname.html", title: m.hostname()) {
          f.textbox(name: "_.hostname", checkUrl: "${descriptor.getCheckUrl('hostname')}+'?value='+escape(this.value)", value: instance?.hostname)
        }
        f.entry(help: "${helpUrl}username.html", title: m.username()) {
          f.textbox(name: "_.username", value: instance?.username)
        }
        f.entry(help: "${helpUrl}password.html", title: m.password()) {
          input(name: "_.password", type: "password", value: instance?.encryptedPassword, class: "setting-input")
        }
        f.entry(help: "${helpUrl}remoteRootDir.html", title: _("remotePath")) {
          f.textbox(name: "_.remoteRootDir", checkUrl: "${descriptor.getCheckUrl('remoteRootDir')}+'?value='+escape(this.value)", value: instance?.remoteRootDir)
        }
        f.advanced() {
          f.entry(help: "${helpUrl}port.html", title: m.port()) {
            f.textbox(default: defaultPort, name: "_.port", checkUrl: "${descriptor.getCheckUrl('port')}+'?value='+escape(this.value)", value: instance?.port)
          }
          f.entry(help: "${helpUrl}timeOut.html", title: m.timeout()) {
            f.textbox(default: defaultTimeout, name: "_.timeout", checkUrl: "${descriptor.getCheckUrl('timeout')}+'?value='+escape(this.value)", value: instance?.timeout)
          }
          f.entry(help: "${helpUrl}bufferSize.html", title: _("hostconfig.field.bufferSize")) {
            f.textbox(default: defaultBufferSize, name: "_.bufferSize", checkUrl: "${descriptor.getCheckUrl('bufferSize')}+'?value='+escape(this.value)", value: instance?.bufferSize)
          }
          f.entry(help: "${helpUrl}smbVersion.html", title: _("hostconfig.field.smbVersion")) {
            select(name: "_.smbVersion", class: "setting-input") {
              jenkins.plugins.publish_over_cifs.CifsHostConfiguration.SmbVersions.values().each { ver ->
                 if(ver == instance?.smbVersion) {
                   option(value: ver.name(), ver.description, selected: '')
                 } else {
                   option(value: ver.name(), ver.description)
                 }
              }
            }
          }
        }
        f.validateButton(with: "name,hostname,username,password,remoteRootDir,port,timeout,bufferSize,poc-np.winsServer,smbVersion", method: "testConnection", progress: m.test_progress(), title: m.test_title())
        f.entry(title: "") {
          div(align: "right") {
            f.repeatableDeleteButton()
          }
        }
      }
    }
  }
/*
  if(descriptor.enableOverrideDefaults) {
    f.advanced() {
      f.entry() {
        f.dropdownDescriptorSelector(default: descriptor.pluginDefaultsDescriptor, field: "defaults", title: descriptor.commonManageMessages.defaults()) 
      }
    }
  }
*/
}
