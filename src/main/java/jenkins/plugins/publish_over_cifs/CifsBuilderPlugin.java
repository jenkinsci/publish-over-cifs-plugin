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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.BPInstanceConfig;
import jenkins.plugins.publish_over.BPPlugin;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("PMD.LooseCoupling") // serializable
public class CifsBuilderPlugin extends Builder {

    private final CifsPublisherPlugin delegate;

    @DataBoundConstructor
    public CifsBuilderPlugin(final ArrayList<CifsPublisher> publishers, final boolean continueOnError, final boolean failOnError,
                             final boolean alwaysPublishFromMaster, final String masterNodeName, final CifsParamPublish paramPublish) {
        this.delegate = new CifsPublisherPlugin(publishers, continueOnError, failOnError, alwaysPublishFromMaster, masterNodeName,
                                                paramPublish);
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
                    throws InterruptedException, IOException {
        return delegate.perform(build, launcher, listener);
    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return builder.append(delegate);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final CifsBuilderPlugin that) {
        return builder.append(delegate, that.delegate);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return builder.append("delegate", delegate);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (CifsBuilderPlugin) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    public BPInstanceConfig getInstanceConfig() {
        return delegate.getInstanceConfig();
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return !BPPlugin.PROMOTION_JOB_TYPE.equals(aClass.getCanonicalName());
        }
        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.builder_descriptor_displayName();
        }
        public String getConfigPage() {
            return getViewPage(CifsPublisherPlugin.class, "config.jelly");
        }
        public CifsPublisherPlugin.Descriptor getPublisherDescriptor() {
            return  Jenkins.get().getDescriptorByType(CifsPublisherPlugin.Descriptor.class);
        }
    }

}
