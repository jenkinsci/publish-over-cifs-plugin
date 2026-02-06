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

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.util.SecretHelper;
import jcifs.CIFSContext;
import jcifs.Configuration;
import jcifs.ResolverType;
import jcifs.smb.SmbFile;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BapPublisherException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.Serial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({ "PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods", "PMD.AvoidUsingHardCodedIP" })
@ExtendWith(MockitoExtension.class)
@WithJenkins
class CifsHostConfigurationTest {

    private static final String CFG_NAME = "xxx";
    private static final String SERVER = "myServer";
    private static final String SHARE = "myShare";
    private static final String SIMPLEST_URL = "smb://myServer/myShare/";
    private static String origWinsServer;
    private static String origTimeout;
    private static String origSoTimeout;
    private static String origResolveOrder;

    private final BPBuildInfo buildInfo = new BPBuildInfo(TaskListener.NULL, "", new FilePath(new File("")), null, null);

    @Mock
    private SmbFile mockSmbFile;

    @SuppressWarnings("unused")
    private JenkinsRule jenkinsRule;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        jenkinsRule = rule;
        SecretHelper.setSecretKey();
        origWinsServer = System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_WINS);
        origTimeout = System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_TIMEOUT);
        origSoTimeout = System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_SO_TIMEOUT);
        origResolveOrder = System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_RESOLVE_ORDER);
    }

    @AfterEach
    void afterEach() {
        SecretHelper.clearSecretKey();
        restoreSysProp(CifsHostConfiguration.CONFIG_PROPERTY_WINS, origWinsServer);
        restoreSysProp(CifsHostConfiguration.CONFIG_PROPERTY_TIMEOUT, origTimeout);
        restoreSysProp(CifsHostConfiguration.CONFIG_PROPERTY_SO_TIMEOUT, origSoTimeout);
        restoreSysProp(CifsHostConfiguration.CONFIG_PROPERTY_RESOLVE_ORDER, origResolveOrder);
    }

    private static void restoreSysProp(final String key, final String orig) {
        if (orig == null) System.clearProperty(key);
        else System.setProperty(key, orig);
    }

//    from SmbFile, this is what we are looking for:
//    smb://[[[domain;]username[:password]@]server[:port]/[[share/[dir/]file]]][?[param=value[param2=value2[...]]]

    @Test
    void createSimplestUrl() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, "myShare/", mockSmbFile);
        final String expectedUrl = SIMPLEST_URL;
        assertUrl(expectedUrl, config);
    }

    @Test
    void shareAlwaysEndsWithSeparator() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, SHARE, mockSmbFile);
        final String expectedUrl = SIMPLEST_URL;
        assertUrl(expectedUrl, config);
    }

    @Test
    void shareCanBeAbsolute() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, "/myShare", mockSmbFile);
        final String expectedUrl = SIMPLEST_URL;
        assertUrl(expectedUrl, config);
    }

    @Test
    void hostnameIsRequired() {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, "", null, null, SHARE, mockSmbFile);
        BapPublisherException bpe = assertThrows(BapPublisherException.class, () -> config.createClient(buildInfo).getContext());
        assertTrue(bpe.getLocalizedMessage().contains(Messages.exception_hostnameRequired()));
    }

    @Test
    void sharenameIsRequired() {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, "hello", null, null, "", mockSmbFile);
        BapPublisherException bpe = assertThrows(BapPublisherException.class, () -> config.createClient(buildInfo).getContext());
        assertTrue(bpe.getLocalizedMessage().contains(Messages.exception_shareRequired()));
    }

    @Test
    void canHazPort() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, "/myShare", 123, 100000, mockSmbFile);
        final String expectedUrl = "smb://myServer:123/myShare/";
        assertUrl(expectedUrl, config);
    }

    @Test
    void fixupWindowsSeparatorsInShare() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, "myShare\\and\\subDirs", mockSmbFile);
        final String expectedUrl = "smb://myServer/myShare/and/subDirs/";
        assertUrl(expectedUrl, config);
    }

    private void assertUrl(final String expectedUrl, final CifsHostConfiguration hostConfig) throws Exception {
        when(mockSmbFile.exists()).thenReturn(true);
        when(mockSmbFile.canRead()).thenReturn(true);
        assertEquals(expectedUrl, hostConfig.createClient(buildInfo).getContext());
        assertEquals(expectedUrl, ((ConfigWithMockFile) hostConfig).url);
        verify(mockSmbFile).exists();
        verify(mockSmbFile).canRead();
    }

    @Test
    void testWinsServerIsSetFromBuildInfo() throws Exception {
        System.setProperty(CifsHostConfiguration.CONFIG_PROPERTY_WINS, "1.2.3.4");
        System.setProperty(CifsHostConfiguration.CONFIG_PROPERTY_TIMEOUT, "20000");
        System.setProperty(CifsHostConfiguration.CONFIG_PROPERTY_SO_TIMEOUT, "300000");
        System.setProperty(CifsHostConfiguration.CONFIG_PROPERTY_RESOLVE_ORDER, "DNS");
        final String wins = "5.6.7.8";
        final int timeout = 45000;
        buildInfo.put(CifsPublisher.CTX_KEY_WINS_SERVER, wins);
        when(mockSmbFile.exists()).thenReturn(true);
        when(mockSmbFile.canRead()).thenReturn(true);
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, SHARE, 99, timeout, mockSmbFile);
        Configuration c = config.createClient(buildInfo).getCifsContext().getConfig();
        assertEquals(1, c.getWinsServers().length);
        assertEquals("/" + wins, c.getWinsServers()[0].toString());
        assertEquals(timeout, c.getResponseTimeout());
        assertTrue(c.getResolveOrder().contains(ResolverType.RESOLVER_WINS));
        assertTrue(c.getSoTimeout() > c.getResponseTimeout());
        verify(mockSmbFile).exists();
        verify(mockSmbFile).canRead();
    }

    @Test
    void testWinsServerRemovedFromResolveOrderWhenNotSet() throws Exception {
        System.setProperty(CifsHostConfiguration.CONFIG_PROPERTY_WINS, "1.2.3.4");
        System.setProperty(CifsHostConfiguration.CONFIG_PROPERTY_RESOLVE_ORDER, "WINS");
        final int timeout = 45000;
        when(mockSmbFile.exists()).thenReturn(true);
        when(mockSmbFile.canRead()).thenReturn(true);
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, SHARE, 99, timeout, mockSmbFile);
        Configuration c = config.createClient(buildInfo).getCifsContext().getConfig();
        assertEquals(0, c.getWinsServers().length);
        assertFalse(c.getResolveOrder().contains(ResolverType.RESOLVER_WINS));
        verify(mockSmbFile).exists();
        verify(mockSmbFile).canRead();
    }

    private static class ConfigWithMockFile extends CifsHostConfiguration {
        @Serial
        private static final long serialVersionUID = 1L;
        private final transient SmbFile smbFile;
        private String url;
        ConfigWithMockFile(final String configName,
                           final String hostname,
                           final String username,
                           final String password,
                           final String remoteDirectory,
                           final SmbFile smbFile) {
            this(configName, hostname, username, password, remoteDirectory, DEFAULT_PORT, DEFAULT_TIMEOUT, smbFile);
        }
        ConfigWithMockFile(final String configName,
                           final String hostname,
                           final String username,
                           final String password,
                           final String remoteDirectory,
                           final int port,
                           final int timeout,
                           final SmbFile smbFile) {
            super(configName, hostname, username, password, remoteDirectory, port, timeout, DEFAULT_BUFFER_SIZE);
            this.smbFile = smbFile;
        }
        @Override
        public SmbFile createSmbFile(final CIFSContext context, String url) {
            this.url = url;
            return smbFile;
        }
    }

}
