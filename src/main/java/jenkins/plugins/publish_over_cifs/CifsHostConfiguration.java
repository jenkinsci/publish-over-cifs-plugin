/*
 * The MIT License
 *
 * Copyright (C) 2010-2011 by Anthony Robinson
 * Copyright (C) 2017 Xovis AG
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

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import hudson.Util;
import jenkins.bouncycastle.api.SecurityProviderInitializer;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPHostConfiguration;
import jenkins.plugins.publish_over.BapPublisherException;
import jenkins.plugins.publish_over_cifs.CifsClient.Consumer;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CifsHostConfiguration extends BPHostConfiguration<CifsClient, Object> {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PORT = 445;
    public static final int DEFAULT_TIMEOUT = 30000;

    public static int getDefaultPort() { return DEFAULT_PORT; }
    public static int getDefaultTimeout() { return DEFAULT_TIMEOUT; }

    private int timeout;

    @DataBoundConstructor
    public CifsHostConfiguration(final String name, final String hostname, final String username, final String password,
                                 final String remoteRootDir, final int port, final int timeout) {
        super(name, hostname, username, password, remoteRootDir, port);
        this.timeout = timeout;
    }

    protected final String getPassword() {
        return super.getPassword();
    }

    public int getTimeout() { return timeout; }
    public void setTimeout(final int timeout) { this.timeout = timeout; }

    @Override
    public CifsClient createClient(final BPBuildInfo buildInfo) {
        SecurityProviderInitializer.addSecurityProvider();
        assertRequiredOptions();
        final AuthenticationContext auth = new AuthenticationContext(getUsername(false), getPassword().toCharArray(), getDomain());
        testConfig(getRemoteRootDir(), auth);
        return new CifsClient(buildInfo, getHostnameTrimmed(), getRemoteRootDir(), auth);
    }

    private void assertRequiredOptions() {
        if (getHostnameTrimmed() == null) throw new BapPublisherException(Messages.exception_hostnameRequired());
        if (Util.fixEmptyAndTrim(getRemoteRootDir()) == null) throw new BapPublisherException(Messages.exception_shareRequired());
    }

    private String getDomain() {
        if (Util.fixEmptyAndTrim(getUsername()) != null) {
            final String username = getUsername().trim();
            if(username.contains("\\")) {
                final String[] parts = username.split("\\\\", 2);
                return parts[0];
            }
        }
        return "";
    }

    private String getUsername(boolean withDomain) {
        if(withDomain) {
            return getUsername();
        }
        if (Util.fixEmptyAndTrim(getUsername()) != null) {
            final String username = getUsername().trim();
            if(username.contains("\\")) {
                final String[] parts = username.split("\\\\", 2);
                return parts[1];
            } else {
                return username;
            }
        }
        return "";        
    }

    @SuppressWarnings({ "PMD.PreserveStackTrace", "PMD.JUnit4TestShouldUseTestAnnotation" }) // FFS
    private void testConfig(final String url, final AuthenticationContext auth) {
        try {
            String dir = getSubfolder(url);
            execute(auth, share -> {
                if (!share.folderExists(dir)) {
                    throw new BapPublisherException(Messages.exception_shareNotExist(dir));
                }

                if (share.list(getSubfolder(url)).size() < 0) {
                    throw new BapPublisherException(Messages.exception_cannotReadShare(dir));
                }
            });
        } catch (Exception smbe) {
            throw new BapPublisherException(Messages.exception_jCifsException_testConfig(getHostnameTrimmed() + "/" + getRemoteRootDir(),
                    smbe.getLocalizedMessage()), smbe);
        }
    }

    protected void execute(AuthenticationContext auth, Consumer task) throws Exception {
        SMBClient client = new SMBClient();
        try (Connection connection = client.connect(getHostnameTrimmed(), getPort())) {
            Session session = connection.authenticate(auth);

            // Connect to Share
            try (DiskShare share = (DiskShare) session.connectShare(getShare(getRemoteRootDir()))) {
                task.apply(share);
            }
        }
    }

    private static Pattern p = Pattern.compile("[\\\\/]?([^\\\\/]+)[\\\\/]?(.*)");
    private String getShare(String remoteRootDir) {
        Matcher m = p.matcher(fix(remoteRootDir));
        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    private String getSubfolder(String remoteRootDir) {
        Matcher m = p.matcher(fix(remoteRootDir));
        if (m.find()) {
            return m.group(2);
        }

        return "";
    }

    private String fix(String original) {
        return original.replace('/', '\\');
    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return super.addToHashCode(builder)
            .append(timeout);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final CifsHostConfiguration that) {
        return super.addToEquals(builder, that)
            .append(timeout, that.timeout);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return super.addToToString(builder)
            .append("timeout", timeout);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (CifsHostConfiguration) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    // While this looks redundant, it resolves some issues with XStream Reflection causing it
    // not to persist settings after a reboot
    @Override
    public Object readResolve() {
        return super.readResolve();
    }

}
