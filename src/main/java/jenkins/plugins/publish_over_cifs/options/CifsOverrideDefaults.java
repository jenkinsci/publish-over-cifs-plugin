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

import hudson.Extension;
import jenkins.plugins.publish_over.options.InstanceConfigOptions;
import jenkins.plugins.publish_over.options.ParamPublishOptions;
import jenkins.plugins.publish_over.options.PublisherLabelOptions;
import jenkins.plugins.publish_over.options.PublisherOptions;
import jenkins.plugins.publish_over.options.RetryOptions;
import jenkins.plugins.publish_over.options.TransferOptions;
import jenkins.plugins.publish_over.view_defaults.manage_jenkins.Messages;
import org.kohsuke.stapler.DataBoundConstructor;

public class CifsOverrideDefaults extends CifsDefaults {

    private final CifsOverrideInstanceConfigDefaults overrideInstanceConfig;
    private final CifsOverrideParamPublishDefaults overrideParamPublish;
    private final CifsOverridePublisherDefaults overridePublisher;
    private final CifsOverridePublisherLabelDefaults overridePublisherLabel;
    private final CifsOverrideRetryDefaults overrideRetry;
    private final CifsOverrideTransferDefaults overrideTransfer;

    @DataBoundConstructor
    public CifsOverrideDefaults(final CifsOverrideInstanceConfigDefaults overrideInstanceConfig,
                                final CifsOverrideParamPublishDefaults overrideParamPublish,
                                final CifsOverridePublisherDefaults overridePublisher,
                                final CifsOverridePublisherLabelDefaults overridePublisherLabel,
                                final CifsOverrideRetryDefaults overrideRetry,
                                final CifsOverrideTransferDefaults overrideTransfer) {
        this.overrideInstanceConfig = overrideInstanceConfig;
        this.overrideParamPublish = overrideParamPublish;
        this.overridePublisher = overridePublisher;
        this.overridePublisherLabel = overridePublisherLabel;
        this.overrideRetry = overrideRetry;
        this.overrideTransfer = overrideTransfer;
    }

    // prevent the property type being clobbered in the descriptor map by using different names from the IF
    public CifsOverrideInstanceConfigDefaults getOverrideInstanceConfig() {
        return overrideInstanceConfig;
    }

    public CifsOverrideParamPublishDefaults getOverrideParamPublish() {
        return overrideParamPublish;
    }

    public CifsOverridePublisherDefaults getOverridePublisher() {
        return overridePublisher;
    }

    public CifsOverridePublisherLabelDefaults getOverridePublisherLabel() {
        return overridePublisherLabel;
    }

    public CifsOverrideRetryDefaults getOverrideRetry() {
        return overrideRetry;
    }

    public CifsOverrideTransferDefaults getOverrideTransfer() {
        return overrideTransfer;
    }

    public InstanceConfigOptions getInstanceConfig() {
        return overrideInstanceConfig;
    }

    public ParamPublishOptions getParamPublish() {
        return overrideParamPublish;
    }

    public PublisherOptions getPublisher() {
        return overridePublisher;
    }

    public PublisherLabelOptions getPublisherLabel() {
        return overridePublisherLabel;
    }

    public RetryOptions getRetry() {
        return overrideRetry;
    }

    public TransferOptions getTransfer() {
        return overrideTransfer;
    }

    @Extension
    public static class CifsOverrideDefaultsDescriptor extends CifsDefaultsDescriptor {

        private static final CifsPluginDefaults PLUGIN_DEFAULTS = new CifsPluginDefaults();

        @Override
        public String getDisplayName() {
            return Messages.defaults_overrideDefaults();
        }

        public CifsPluginDefaults getPluginDefaults() {
            return PLUGIN_DEFAULTS;
        }

    }

}
