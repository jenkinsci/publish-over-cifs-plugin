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

package jenkins.plugins.publish_over_cifs;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.util.FormValidation;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPPlugin;
import jenkins.plugins.publish_over.BPPluginDescriptor;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;

@SuppressWarnings("PMD.LooseCoupling") // serializable
public class CifsPublisherPlugin extends BPPlugin<CifsPublisher, CifsClient, Object> {

    private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    public CifsPublisherPlugin(final ArrayList<CifsPublisher> publishers, final boolean continueOnError, final boolean failOnError,
                               final boolean alwaysPublishFromMaster, final String masterNodeName, final boolean verbose) {
        super(Messages.console_message_prefix(), publishers, continueOnError, failOnError, alwaysPublishFromMaster, masterNodeName,
                                                                                                                                verbose);
    }

    @Override
    protected void fixup(final AbstractBuild<?, ?> build, final BPBuildInfo buildInfo) {
        final Hudson hudson = Hudson.getInstance();
        final CifsNodeProperties defaults = hudson.getGlobalNodeProperties().get(CifsNodeProperties.class);
        if (defaults != null) buildInfo.put(CifsPublisher.CTX_KEY_NODE_PROPERTIES_DEFAULT, map(defaults));
        final String currNodeName = buildInfo.getCurrentBuildEnv().getEnvVars().get(BPBuildInfo.ENV_NODE_NAME);
        storeProperties(buildInfo, hudson, currNodeName, CifsPublisher.CTX_KEY_NODE_PROPERTIES_CURRENT);
    }

    private void storeProperties(final BPBuildInfo buildInfo, final Hudson hudson, final String nodeName, final String contextKey) {
        if (Util.fixEmptyAndTrim(nodeName) == null) return;
        final Node node = hudson.getNode(nodeName);
        if (node == null) return;
        final CifsNodeProperties currNodeProps = node.getNodeProperties().get(CifsNodeProperties.class);
        if (currNodeProps != null) buildInfo.put(contextKey, map(currNodeProps));
    }

    private CifsCleanNodeProperties map(final CifsNodeProperties nodeProperties) {
        if (nodeProperties == null) return null;
        return new CifsCleanNodeProperties(Util.fixEmptyAndTrim(nodeProperties.getWinsServer()));
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (CifsPublisherPlugin) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    public CifsHostConfiguration getConfiguration(final String name) {
        return Hudson.getInstance().getDescriptorByType(Descriptor.class).getConfiguration(name);
    }

    @Extension
    public static class Descriptor extends BPPluginDescriptor<CifsHostConfiguration, Object> {
        public Descriptor() {
            super(new DescriptorMessages(), CifsPublisherPlugin.class, CifsHostConfiguration.class, null);
        }
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return !BPPlugin.PROMOTION_JOB_TYPE.equals(aClass.getCanonicalName());
        }
        public FormValidation doCheckSourceFiles(@QueryParameter final String value) {
            return FormValidation.validateRequired(value);
        }
        public CifsPublisherPlugin.Descriptor getPublisherDescriptor() {
            return this;
        }
        public FormValidation doCheckRemoteRootDir(@QueryParameter final String value) {
            return FormValidation.validateRequired(value);
        }
        @Override
        protected BPBuildInfo createDummyBuildInfo(final StaplerRequest request) {
            final BPBuildInfo buildInfo = super.createDummyBuildInfo(request);
            final CifsNodeProperties defaults = request.bindParameters(CifsNodeProperties.class, CifsNodeProperties.FORM_PREFIX);
            if (defaults != null && Util.fixEmptyAndTrim(defaults.getWinsServer()) != null)
                buildInfo.put(CifsPublisher.CTX_KEY_WINS_SERVER, defaults.getWinsServer().trim());
            return buildInfo;
        }
    }

    public static class DescriptorMessages implements BPPluginDescriptor.BPDescriptorMessages {
        public String displayName() {
            return Messages.descriptor_displayName();
        }
        public String connectionOK() {
            return Messages.descriptor_testConnection_ok();
        }
        public String connectionErr() {
            return Messages.descriptor_testConnection_error();
        }
    }

}
