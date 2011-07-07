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

import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import hudson.util.VersionNumber;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPInstanceConfig;
import jenkins.plugins.publish_over.BPPlugin;
import jenkins.plugins.publish_over.BPTransfer;
import jenkins.plugins.publish_over.BPValidators;
import jenkins.plugins.publish_over.Retry;
import jenkins.plugins.publish_over_cifs.CifsHostConfiguration;
import jenkins.plugins.publish_over_cifs.CifsNodeProperties;
import jenkins.plugins.publish_over_cifs.CifsPublisher;
import jenkins.plugins.publish_over_cifs.CifsPublisherPlugin;
import jenkins.plugins.publish_over_cifs.Messages;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.List;

@SuppressWarnings("PMD.TooManyMethods")
public class CifsPublisherPluginDescriptor extends BuildStepDescriptor<Publisher> {

    /** null - prevent complaints from xstream */
    private CifsPublisherPlugin.DescriptorMessages msg;
    /** null - prevent complaints from xstream */
    private Class hostConfigClass;
    private final CopyOnWriteList<CifsHostConfiguration> hostConfigurations = new CopyOnWriteList<CifsHostConfiguration>();

    public CifsPublisherPluginDescriptor() {
        super(CifsPublisherPlugin.class);
        load();
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

    public boolean configure(final StaplerRequest request, final JSONObject formData) {
        hostConfigurations.replaceBy(request.bindJSONToList(CifsHostConfiguration.class, formData.get("instance")));
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
    public boolean canUseExcludes() {
        return BPTransfer.canUseExcludes();
    }
    public String getDefaultMasterNodeName() {
        return BPInstanceConfig.DEFAULT_MASTER_NODE_NAME;
    }
    public boolean canSetMasterNodeName() {
        return Hudson.getVersion().isOlderThan(new VersionNumber(BPInstanceConfig.MASTER_GETS_NODE_NAME_IN_VERSION));
    }
    public int getDefaultRetries() {
        return Retry.DEFAULT_RETRIES;
    }    
    public long getDefaultRetryDelay() {
        return Retry.DEFAULT_RETRY_DELAY;
    }    
    public CifsPublisherPluginDescriptor getPublisherDescriptor() {
        return this;
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
        final BPBuildInfo buildInfo = new BPBuildInfo(
            TaskListener.NULL,
            "",
            Hudson.getInstance().getRootPath(),
            null,
            null
        );
        final CifsNodeProperties defaults = request.bindParameters(CifsNodeProperties.class, CifsNodeProperties.FORM_PREFIX);
        if (defaults != null && Util.fixEmptyAndTrim(defaults.getWinsServer()) != null)
            buildInfo.put(CifsPublisher.CTX_KEY_WINS_SERVER, defaults.getWinsServer().trim());
        return buildInfo;
    }

    public Object readResolve() {
        // nuke the legacy config
        msg = null;
        hostConfigClass = null;
        return this;
    }

}
