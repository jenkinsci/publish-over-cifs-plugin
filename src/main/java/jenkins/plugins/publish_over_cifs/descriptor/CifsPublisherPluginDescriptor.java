/*
 * The MIT License
 *
 * Copyright (C) 2010-2011 by Anthony Robinson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.publish_over_cifs.descriptor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPInstanceConfig;
import jenkins.plugins.publish_over.BPPlugin;
import jenkins.plugins.publish_over.BPValidators;
import jenkins.plugins.publish_over.JenkinsCapabilities;
import jenkins.plugins.publish_over_cifs.CifsHostConfiguration;
import jenkins.plugins.publish_over_cifs.CifsNodeProperties;
import jenkins.plugins.publish_over_cifs.CifsPublisher;
import jenkins.plugins.publish_over_cifs.CifsPublisherPlugin;
import jenkins.plugins.publish_over_cifs.Messages;
import jenkins.plugins.publish_over_cifs.options.CifsDefaults;
import jenkins.plugins.publish_over_cifs.options.CifsPluginDefaults;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.List;

@SuppressWarnings("PMD.TooManyMethods")
public class CifsPublisherPluginDescriptor extends BuildStepDescriptor<Publisher> {

    /** null - prevent complaints from xstream */
    @SuppressFBWarnings("URF_UNREAD_FIELD")
    private CifsPublisherPlugin.DescriptorMessages msg;
    /** null - prevent complaints from xstream */
    @SuppressFBWarnings("URF_UNREAD_FIELD")
    private Class hostConfigClass;
    private final CopyOnWriteList<CifsHostConfiguration> hostConfigurations = new CopyOnWriteList<CifsHostConfiguration>();
    private CifsDefaults defaults;

    public CifsPublisherPluginDescriptor() {
        super(CifsPublisherPlugin.class);
        load();
        if (defaults == null)
            defaults = new CifsPluginDefaults();
    }

    public CifsDefaults getDefaults() {
        return defaults;
    }

    public String getDisplayName() {
        return Messages.descriptor_displayName();
    }

    public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
        return !BPPlugin.PROMOTION_JOB_TYPE.equals(aClass.getCanonicalName());
    }

    public List<CifsHostConfiguration> getHostConfigurations() {
        return hostConfigurations.getView();
    }

    public CifsHostConfiguration getConfiguration(final String name) {
        for (CifsHostConfiguration configuration : hostConfigurations) {
            if (configuration.getName().equals(name)) {
                return configuration;
            }
        }
        return null;
    }

    /**
     * Adds the given CifsHostConfiguration to the current collection of host configurations.
     * @param configuration The CifsHostConfiguration to add.
     */
    public void addHostConfiguration(final CifsHostConfiguration configuration) {
        hostConfigurations.add(configuration);
    }

    /**
     * Removes the host configuration with the given name from the current collection of host configurations.
     * @param name The name of the host configuration to remove.
     */
    public void removeHostConfiguration(final String name) {
        CifsHostConfiguration configuration = getConfiguration(name);
        if (configuration != null) {
            hostConfigurations.remove(configuration);
        }
    }

    public boolean configure(final StaplerRequest request, final JSONObject formData) {
        hostConfigurations.replaceBy(request.bindJSONToList(CifsHostConfiguration.class, formData.get("instance")));
        if (isEnableOverrideDefaults())
            defaults = request.bindJSON(CifsDefaults.class, formData.getJSONObject("defaults"));
        save();
        return true;
    }

    public FormValidation doCheckName(@QueryParameter final String value) {
        return BPValidators.validateName(value);
    }
    public FormValidation doCheckHostname(@QueryParameter final String value) {
        return FormValidation.validateRequired(value);
    }
    public FormValidation doCheckUsername(@QueryParameter final String value) {
        return FormValidation.validateRequired(value);
    }
    public FormValidation doCheckPort(@QueryParameter final String value) {
        return FormValidation.validatePositiveInteger(value);
    }
    public FormValidation doCheckTimeout(@QueryParameter final String value) {
        return FormValidation.validateNonNegativeInteger(value);
    }
    public FormValidation doCheckBufferSize(@QueryParameter final String value) {
        return FormValidation.validatePositiveInteger(value);
    }
    public FormValidation doCheckSourceFiles(@QueryParameter final String value) {
        return FormValidation.validateRequired(value);
    }
    public FormValidation doCheckRemoteRootDir(@QueryParameter final String value) {
        return FormValidation.validateRequired(value);
    }
    public FormValidation doCheckRetries(@QueryParameter final String value) {
        return FormValidation.validateNonNegativeInteger(value);
    }
    public FormValidation doCheckRetryDelay(@QueryParameter final String value) {
        return FormValidation.validatePositiveInteger(value);
    }
    public String getDefaultMasterNodeName() {
        return BPInstanceConfig.DEFAULT_MASTER_NODE_NAME;
    }
    public boolean canSetMasterNodeName() {
        return JenkinsCapabilities.missing(JenkinsCapabilities.MASTER_HAS_NODE_NAME);
    }
    public boolean isEnableOverrideDefaults() {
        return JenkinsCapabilities.available(JenkinsCapabilities.SIMPLE_DESCRIPTOR_SELECTOR);
    }
    public CifsPublisherPluginDescriptor getPublisherDescriptor() {
        return this;
    }

    public jenkins.plugins.publish_over.view_defaults.manage_jenkins.Messages getCommonManageMessages() {
        return new jenkins.plugins.publish_over.view_defaults.manage_jenkins.Messages();
    }


    public FormValidation doTestConnection(final StaplerRequest request, final StaplerResponse response) {
        final CifsHostConfiguration hostConfig = request.bindParameters(CifsHostConfiguration.class, "");
        final BPBuildInfo buildInfo = createDummyBuildInfo(request);
        try {
            hostConfig.createClient(buildInfo).disconnect();
            return FormValidation.ok(Messages.descriptor_testConnection_ok());
        } catch (Exception e) {
            return FormValidation.errorWithMarkup("<p>"
                    + Messages.descriptor_testConnection_error() + "</p><p><pre>"
                    + Util.escape(e.getClass().getCanonicalName() + ": " + e.getLocalizedMessage())
                    + "</pre></p>");
        }
    }

    protected BPBuildInfo createDummyBuildInfo(final StaplerRequest request) {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            final BPBuildInfo buildInfo = new BPBuildInfo(
                    TaskListener.NULL,
                    "",
                    jenkins.getRootPath(),
                    null,
                    null
            );
            final CifsNodeProperties defaults = request.bindParameters(CifsNodeProperties.class, CifsNodeProperties.FORM_PREFIX);
            if (defaults != null && Util.fixEmptyAndTrim(defaults.getWinsServer()) != null)
                buildInfo.put(CifsPublisher.CTX_KEY_WINS_SERVER, defaults.getWinsServer().trim());
            return buildInfo;
        } else {
            return null;
        }
    }

    public Object readResolve() {
        // nuke the legacy config
        msg = null;
        hostConfigClass = null;
        if (defaults == null)
            defaults = new CifsPluginDefaults();
        return this;
    }

    public jenkins.plugins.publish_over.view_defaults.HostConfiguration.Messages getHostConfigurationFieldNames() {
        return new jenkins.plugins.publish_over.view_defaults.HostConfiguration.Messages();
    }

    public jenkins.plugins.publish_over.view_defaults.BapPublisher.Messages getPublisherFieldNames() {
        return new jenkins.plugins.publish_over.view_defaults.BapPublisher.Messages();
    }

    public jenkins.plugins.publish_over.view_defaults.BPInstanceConfig.Messages getPluginFieldNames() {
        return new jenkins.plugins.publish_over.view_defaults.BPInstanceConfig.Messages();
    }

    public jenkins.plugins.publish_over.view_defaults.BPTransfer.Messages getTransferFieldNames() {
        return new jenkins.plugins.publish_over.view_defaults.BPTransfer.Messages();
    }

    public jenkins.plugins.publish_over.view_defaults.ParamPublish.Messages getParamPublishFieldNames() {
        return new jenkins.plugins.publish_over.view_defaults.ParamPublish.Messages();
    }

    public jenkins.plugins.publish_over.view_defaults.PublisherLabel.Messages getPublisherLabelFieldNames() {
        return new jenkins.plugins.publish_over.view_defaults.PublisherLabel.Messages();
    }

    public jenkins.plugins.publish_over.view_defaults.Retry.Messages getRetryFieldNames() {
        return new jenkins.plugins.publish_over.view_defaults.Retry.Messages();
    }

}
