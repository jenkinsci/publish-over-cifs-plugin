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
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.FormValidation;
import jenkins.plugins.publish_over.BPValidators;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class CifsNodeProperties extends NodeProperty<Node> {

    public static final String FORM_PREFIX = "poc-np.";
    private String winsServer;

    @DataBoundConstructor
    public CifsNodeProperties(final String winsServer) {
        this.winsServer = winsServer;
    }

    public String getWinsServer() { return winsServer; }
    public void setWinsServer(final String winsServer) { this.winsServer = winsServer; }

    @Extension
    public static class CifsWinsNodePropertyDescriptor extends NodePropertyDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.winsNodeProperty_descriptor_displayName();
        }
        public FormValidation doCheckWinsServer(@QueryParameter final String value) {
            return BPValidators.validateOptionalIp(value);
        }
    }

}
