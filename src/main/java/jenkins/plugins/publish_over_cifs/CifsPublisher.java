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

import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPHostConfiguration;
import jenkins.plugins.publish_over.BapPublisher;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;

@SuppressWarnings("PMD.LooseCoupling") // serializable
public class CifsPublisher extends BapPublisher<CifsTransfer> {

    private static final long serialVersionUID = 1L;
    public static final String CTX_KEY_NODE_PROPERTIES_DEFAULT = "cifs.np.default";
    public static final String CTX_KEY_NODE_PROPERTIES_CURRENT = "cifs.np.current";
    public static final String CTX_KEY_WINS_SERVER = "cifs.winsServer";

    @DataBoundConstructor
    public CifsPublisher(final String configName, final boolean verbose, final ArrayList<CifsTransfer> transfers,
                         final boolean useWorkspaceInPromotion, final boolean usePromotionTimestamp, final CifsRetry retry,
                         final CifsPublisherLabel label) {
        super(configName, verbose, transfers, useWorkspaceInPromotion, usePromotionTimestamp, retry, label, null);
    }

    @Override
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void perform(final BPHostConfiguration hostConfig, final BPBuildInfo buildInfo) throws Exception {
        final CifsCleanNodeProperties defaults = (CifsCleanNodeProperties) buildInfo.get(CTX_KEY_NODE_PROPERTIES_DEFAULT);
        if (buildInfo.onMaster()) {
            storeWinsServer(buildInfo, defaults);
        } else {
            final CifsCleanNodeProperties current = (CifsCleanNodeProperties) buildInfo.get(CTX_KEY_NODE_PROPERTIES_CURRENT);
            if (!storeWinsServer(buildInfo, current)) storeWinsServer(buildInfo, defaults);
        }
        super.perform(hostConfig, buildInfo);
    }

    private boolean storeWinsServer(final BPBuildInfo buildInfo, final CifsCleanNodeProperties nodeProperties) {
        if (nodeProperties != null) {
            if (nodeProperties.getWinsServer() != null)
                buildInfo.put(CTX_KEY_WINS_SERVER, nodeProperties.getWinsServer());
            return true;
        }
        return false;
    }

    public CifsRetry getRetry() {
        return (CifsRetry) super.getRetry();
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (CifsPublisher) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }
}
