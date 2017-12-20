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
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BapPublisherException;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jvnet.hudson.test.JenkinsRule;

@SuppressWarnings({ "PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods", "PMD.AvoidUsingHardCodedIP" })
public class CifsHostConfigurationTest {

    private static final String CFG_NAME = "xxx";
    private static final String SERVER = "myServer";
    private static final String SHARE = "myShare";
    private static final String SIMPLEST_URL = "smb://myServer/myShare/";
    private static String origWinsServer;
    private static String origTimeout;
    private static String origSoTimeout;
    private static String origResolveOrder;

    @Rule 
    public JenkinsRule jenkinsRule = new JenkinsRule() {
        @Override 
        public void before() throws Throwable {
            super.before();
            SecretHelper.setSecretKey();
            origWinsServer = System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_WINS);
            origTimeout = System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_TIMEOUT);
            origSoTimeout = System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_SO_TIMEOUT);
            origResolveOrder = System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_RESOLVE_ORDER);
        }

        @Override
        public void after() throws Exception {
            super.after();
            SecretHelper.clearSecretKey();
            restoreSysProp(CifsHostConfiguration.CONFIG_PROPERTY_WINS, origWinsServer);
            restoreSysProp(CifsHostConfiguration.CONFIG_PROPERTY_TIMEOUT, origTimeout);
            restoreSysProp(CifsHostConfiguration.CONFIG_PROPERTY_SO_TIMEOUT, origSoTimeout);
            restoreSysProp(CifsHostConfiguration.CONFIG_PROPERTY_RESOLVE_ORDER, origResolveOrder);
        }
    };

    private static void restoreSysProp(final String key, final String orig) {
        if (orig == null) System.clearProperty(key);
        else System.setProperty(key, orig);
    }

    private final transient BPBuildInfo buildInfo = new BPBuildInfo(TaskListener.NULL, "", new FilePath(new File("")), null, null);
    private final transient IMocksControl mockControl = EasyMock.createStrictControl();
    private final transient SmbFile mockSmbFile = mockControl.createMock(SmbFile.class);

//    from SmbFile, this is what we are looking for:
//    smb://[[[domain;]username[:password]@]server[:port]/[[share/[dir/]file]]][?[param=value[param2=value2[...]]]

    @Test public void createSimplestUrl() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, "myShare/", mockSmbFile);
        final String expectedUrl = SIMPLEST_URL;
        assertUrl(expectedUrl, config);
    }

    @Test public void shareAlwaysEndsWithSeparator() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, SHARE, mockSmbFile);
        final String expectedUrl = SIMPLEST_URL;
        assertUrl(expectedUrl, config);
    }

    @Test public void shareCanBeAbsolute() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, "/myShare", mockSmbFile);
        final String expectedUrl = SIMPLEST_URL;
        assertUrl(expectedUrl, config);
    }

    @Test public void hostnameIsRequired() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, "", null, null, SHARE, mockSmbFile);
        try {
            config.createClient(buildInfo).getContext();
            fail();
        } catch (final BapPublisherException bpe) {
            assertTrue(bpe.getLocalizedMessage().contains(Messages.exception_hostnameRequired()));
        }
    }

    @Test public void sharenameIsRequired() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, "hello", null, null, "", mockSmbFile);
        try {
            config.createClient(buildInfo).getContext();
            fail();
        } catch (final BapPublisherException bpe) {
            assertTrue(bpe.getLocalizedMessage().contains(Messages.exception_shareRequired()));
        }
    }

    @Test public void canHazPort() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, "/myShare", 123, 100000, mockSmbFile);
        final String expectedUrl = "smb://myServer:123/myShare/";
        assertUrl(expectedUrl, config);
    }

    @Test public void fixupWindowsSeparatorsInShare() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, "myShare\\and\\subDirs", mockSmbFile);
        final String expectedUrl = "smb://myServer/myShare/and/subDirs/";
        assertUrl(expectedUrl, config);
    }

    private void assertUrl(final String expectedUrl, final CifsHostConfiguration hostConfig) throws Exception {
        expect(mockSmbFile.exists()).andReturn(true);
        expect(mockSmbFile.canRead()).andReturn(true);
        mockControl.replay();
        assertEquals(expectedUrl, hostConfig.createClient(buildInfo).getContext());
        assertEquals(expectedUrl, ((ConfigWithMockFile) hostConfig).url);
        mockControl.verify();
    }

    @Test public void testWinsServerIsSetFromBuildInfo() throws Exception {
        System.setProperty(CifsHostConfiguration.CONFIG_PROPERTY_WINS, "1.2.3.4");
        System.setProperty(CifsHostConfiguration.CONFIG_PROPERTY_TIMEOUT, "20000");
        System.setProperty(CifsHostConfiguration.CONFIG_PROPERTY_SO_TIMEOUT, "300000");
        System.setProperty(CifsHostConfiguration.CONFIG_PROPERTY_RESOLVE_ORDER, "DNS");
        final String wins = "5.6.7.8";
        final int timeout = 45000;
        buildInfo.put(CifsPublisher.CTX_KEY_WINS_SERVER, wins);
        expect(mockSmbFile.exists()).andReturn(true);
        expect(mockSmbFile.canRead()).andReturn(true);
        mockControl.replay();
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, SHARE, 99, timeout, mockSmbFile);
        config.createClient(buildInfo);
        assertEquals(wins, System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_WINS));
        assertEquals(Integer.toString(timeout), System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_TIMEOUT));
        assertTrue(System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_RESOLVE_ORDER).contains("WINS"));
        assertTrue(Integer.parseInt(System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_SO_TIMEOUT))
                   > Integer.parseInt(System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_TIMEOUT)));
    }

    @Test public void testWinsServerRemovedFromResolveOrderWhenNotSet() throws Exception {
        System.setProperty(CifsHostConfiguration.CONFIG_PROPERTY_WINS, "1.2.3.4");
        System.setProperty(CifsHostConfiguration.CONFIG_PROPERTY_RESOLVE_ORDER, "WINS");
        final int timeout = 45000;
        expect(mockSmbFile.exists()).andReturn(true);
        expect(mockSmbFile.canRead()).andReturn(true);
        mockControl.replay();
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, SHARE, 99, timeout, mockSmbFile);
        config.createClient(buildInfo);
        assertNull(System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_WINS));
        assertFalse(System.getProperty(CifsHostConfiguration.CONFIG_PROPERTY_RESOLVE_ORDER).contains("WINS"));
    }

    private static class ConfigWithMockFile extends CifsHostConfiguration {
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
        public SmbFile createSmbFile(final String url, NtlmPasswordAuthentication auth) {
            this.url = url;
            return smbFile;
        }
    }

}
