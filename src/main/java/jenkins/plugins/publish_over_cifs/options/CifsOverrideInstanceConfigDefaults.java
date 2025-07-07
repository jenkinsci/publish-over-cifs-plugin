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

package jenkins.plugins.publish_over_cifs.options;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.options.InstanceConfigOptions;
import org.kohsuke.stapler.DataBoundConstructor;

public class CifsOverrideInstanceConfigDefaults implements InstanceConfigOptions, Describable<CifsOverrideInstanceConfigDefaults> {

    private final boolean continueOnError;
    private final boolean failOnError;
    private final boolean alwaysPublishFromMaster;
    private final boolean publishWhenFailed;

    @DataBoundConstructor
    public CifsOverrideInstanceConfigDefaults(final boolean alwaysPublishFromMaster, final boolean continueOnError,
                                              final boolean failOnError, final boolean publishWhenFailed) {
        this.alwaysPublishFromMaster = alwaysPublishFromMaster;
        this.continueOnError = continueOnError;
        this.failOnError = failOnError;
        this.publishWhenFailed = publishWhenFailed;
    }

    public boolean isContinueOnError() {
        return continueOnError;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public boolean isAlwaysPublishFromMaster() {
        return alwaysPublishFromMaster;
    }

    public boolean isPublishWhenFailed() {
        return publishWhenFailed;
    }
    public CifsOverrideInstanceConfigDefaultsDescriptor getDescriptor() {
        return Jenkins.get().getDescriptorByType(CifsOverrideInstanceConfigDefaultsDescriptor.class);
    }

    @Extension
    public static class CifsOverrideInstanceConfigDefaultsDescriptor extends Descriptor<CifsOverrideInstanceConfigDefaults> {

        @NonNull
        @Override
        public String getDisplayName() {
            return "CifsOverrideInstanceConfigDefaultsDescriptor - not visible ...";
        }

        public jenkins.plugins.publish_over.view_defaults.BPInstanceConfig.Messages getCommonFieldNames() {
            return new jenkins.plugins.publish_over.view_defaults.BPInstanceConfig.Messages();
        }

    }

}
