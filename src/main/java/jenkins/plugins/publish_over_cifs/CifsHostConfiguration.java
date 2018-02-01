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

import hudson.Util;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.NtlmPasswordAuthentication;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPHostConfiguration;
import jenkins.plugins.publish_over.BapPublisherException;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;

import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

@SuppressWarnings("PMD.CyclomaticComplexity") // yeah that encode method aint great, but we want it to be reasonably quick
public class CifsHostConfiguration extends BPHostConfiguration<CifsClient, Object> {

    private static final long serialVersionUID = 1L;

    public static final String SMB_URL_PREFIX = "smb://";
    @SuppressWarnings("PMD.ShortVariable")
    private static final String FS = "/";
    private static final String PASSWORD_PLACEHOLDER = "****";
    public static final int DEFAULT_PORT = SmbFile.DEFAULT_PORT;
    public static final int DEFAULT_TIMEOUT = SmbFile.DEFAULT_RESPONSE_TIMEOUT;
    public static final int SO_TIMEOUT_AFTER = SmbFile.DEFAULT_SO_TIMEOUT - SmbFile.DEFAULT_RESPONSE_TIMEOUT;
    private static final int URL_BUILDER_INITIAL_SIZE = 60;
    private static final int ESCAPED_BUILDER_SIZE_MULTIPLIER = 3;

    //hard coded value because apache IOUtils doesn't provide any method to get the default value of buffer size
    public static final int DEFAULT_BUFFER_SIZE = 4 * 1024;

    private static final String RESOLVE_WITH_WINS = "LMHOSTS,WINS,DNS,BCAST";
    private static final String RESOLVE_WITHOUT_WINS = "LMHOSTS,DNS,BCAST";
    public static final String CONFIG_PROPERTY_TIMEOUT = "jcifs.smb.client.responseTimeout";
    public static final String CONFIG_PROPERTY_SO_TIMEOUT = "jcifs.smb.client.soTimeout";
    public static final String CONFIG_PROPERTY_WINS = "jcifs.netbios.wins";
    public static final String CONFIG_PROPERTY_RESOLVE_ORDER = "jcifs.resolveOrder";
    private static final int LOW_NIBBLE_BIT_MASK = 0xF;
    private static final int HEX_LETTERS_START_AT = 10;
    private static final int HI_TO_LOW_NIBBLE_BIT_SHIFT = 4;

    public static int getDefaultPort() { return DEFAULT_PORT; }
    public static int getDefaultTimeout() { return DEFAULT_TIMEOUT; }
    public static int getDefaultBufferSize() { return DEFAULT_BUFFER_SIZE; }

    private int timeout;
    private int bufferSize;

    @DataBoundConstructor
    public CifsHostConfiguration(final String name, final String hostname, final String username, final String password,
                                 final String remoteRootDir, final int port, final int timeout, final int bufferSize) {
        super(name, hostname, username, password, remoteRootDir, port);
        this.timeout = timeout;
        this.bufferSize = bufferSize;
    }

    protected final String getPassword() {
        return super.getPassword();
    }

    public int getTimeout() { return timeout; }
    public void setTimeout(final int timeout) { this.timeout = timeout; }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(final int bufferSize) {
        if(bufferSize > 0) {
            this.bufferSize = bufferSize;
        }
    }

    @Override
    public CifsClient createClient(final BPBuildInfo buildInfo) {
        assertRequiredOptions();
        configureJcifs(buildInfo);
        final String url = buildUrl(false);
        final NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(getDomain(), getUsername(false), getPassword()); 
        testConfig(url, auth);
        return new CifsClient(buildInfo, url, auth, bufferSize);
    }

    private void configureJcifs(final BPBuildInfo buildInfo) {
        // freaking statics/sys props - what could possibly go wrong? aaargh!
        // will it even have any effect if the config changes after we have used jCIFS and have not rebooted?
        final String winsServer = (String) buildInfo.get(CifsPublisher.CTX_KEY_WINS_SERVER);
        final int soTimeout = timeout + SO_TIMEOUT_AFTER;
        if (winsServer == null) {
            buildInfo.printIfVerbose(Messages.console_config_noWins());
            System.clearProperty(CONFIG_PROPERTY_WINS);
            System.setProperty(CONFIG_PROPERTY_RESOLVE_ORDER, RESOLVE_WITHOUT_WINS);
        } else {
            buildInfo.printIfVerbose(Messages.console_config_wins(winsServer));
            System.setProperty(CONFIG_PROPERTY_WINS, winsServer);
            System.setProperty(CONFIG_PROPERTY_RESOLVE_ORDER, RESOLVE_WITH_WINS);
        }
        if (buildInfo.isVerbose()) {
            buildInfo.println(Messages.console_config_timout(timeout));
            buildInfo.println(Messages.console_config_soTimeout(soTimeout));
            buildInfo.println(Messages.console_config_bufferSize(bufferSize));
        }
        System.setProperty(CONFIG_PROPERTY_TIMEOUT, Integer.toString(timeout));
        System.setProperty(CONFIG_PROPERTY_SO_TIMEOUT, Integer.toString(soTimeout));
    }

    private String buildUrl(final boolean hidePassword) {
        final StringBuilder urlSB = new StringBuilder(URL_BUILDER_INITIAL_SIZE);
        urlSB.append(SMB_URL_PREFIX);
        //addCredentials(urlSB, hidePassword);
        addServer(urlSB);
        addSharename(urlSB);
        return urlSB.toString();
    }

    private void assertRequiredOptions() {
        if (getHostnameTrimmed() == null) throw new BapPublisherException(Messages.exception_hostnameRequired());
        if (Util.fixEmptyAndTrim(getRemoteRootDir()) == null) throw new BapPublisherException(Messages.exception_shareRequired());
    }

    private void addSharename(final StringBuilder urlSB) {
        final String share = getRemoteRootDir().replaceAll("\\\\", "/");
        if (!share.startsWith(FS)) urlSB.append(FS);
        urlSB.append(share);
        if (!share.endsWith(FS)) urlSB.append(FS);
    }

    private void addServer(final StringBuilder urlSB) {
        urlSB.append(getHostnameTrimmed());
        if (getPort() != DEFAULT_PORT)
            urlSB.append(":").append(getPort());
    }

    private void addCredentials(final StringBuilder urlSB, final boolean hidePassword) {
        if (Util.fixEmptyAndTrim(getUsername()) != null) {
            final String username = getUsername().trim();
            if (username.contains("\\")) {
                final String[] parts = username.split("\\\\", 2);
                urlSB.append(encode(parts[0])).append(";").append(encode(parts[1].trim()));
            } else {
                urlSB.append(encode(username));
            }
            if (Util.fixEmptyAndTrim(getPassword()) != null) {
                urlSB.append(":");
                urlSB.append(hidePassword ? PASSWORD_PLACEHOLDER : encode(getPassword().trim()));
            }
            urlSB.append('@');
        }
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
    private void testConfig(final String url, final NtlmPasswordAuthentication auth) {
        SmbFile file;
        try {
            file = createSmbFile(url, auth);
        } catch (final MalformedURLException mue) {
            throw new BapPublisherException(Messages.exception_maformedUrlException(buildUrl(true)));
        }
        try {
            if (!file.exists()) throw new BapPublisherException(Messages.exception_shareNotExist(buildUrl(true)));
            if (!file.canRead()) throw new BapPublisherException(Messages.exception_cannotReadShare(buildUrl(true)));
        } catch (final SmbException smbe) {
            throw new BapPublisherException(Messages.exception_jCifsException_testConfig(buildUrl(true), smbe.getLocalizedMessage()), smbe);
        }
    }

    protected SmbFile createSmbFile(final String url, NtlmPasswordAuthentication auth) throws MalformedURLException {
        if(auth != null) {
            return new SmbFile(url, auth);
        } else {
            return new SmbFile(url);
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private static String encode(final String raw) {
        if (raw == null) return null;
        final StringBuilder encoded = new StringBuilder(raw.length() * ESCAPED_BUILDER_SIZE_MULTIPLIER);
        final CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        final CharBuffer buffer = CharBuffer.allocate(1);
        for (final char c : raw.toCharArray()) {
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                encoded.append(c);
            } else {
                buffer.put(0, c);
                buffer.rewind();
                try {
                    final ByteBuffer bytes = encoder.encode(buffer);
                    while (bytes.hasRemaining()) {
                        final byte oneByte = bytes.get();
                        encoded.append('%');
                        encoded.append(toDigit((oneByte >> HI_TO_LOW_NIBBLE_BIT_SHIFT) & LOW_NIBBLE_BIT_MASK));
                        encoded.append(toDigit(oneByte & LOW_NIBBLE_BIT_MASK));
                    }
                } catch (final CharacterCodingException cce) {
                    throw new BapPublisherException(Messages.exception_encode_cce(cce.getLocalizedMessage()), cce);
                }
            }
        }
        return encoded.toString();
    }

    private static char toDigit(final int nibble) {
        return (char) (nibble < HEX_LETTERS_START_AT ? '0' + nibble : 'A' + nibble - HEX_LETTERS_START_AT);
    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return super.addToHashCode(builder)
            .append(timeout)
            .append(bufferSize);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final CifsHostConfiguration that) {
        return super.addToEquals(builder, that)
            .append(timeout, that.timeout)
            .append(bufferSize, that.bufferSize);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return super.addToToString(builder)
            .append("timeout", timeout)
            .append("bufferSize", bufferSize);
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
        if(bufferSize <= 0) {
            bufferSize = DEFAULT_BUFFER_SIZE;
        }
        return super.readResolve();
    }

}
