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

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.share.DiskShare;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.util.SecretHelper;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BapPublisherException;
import jenkins.plugins.publish_over_cifs.CifsClient.Consumer;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jvnet.hudson.test.JenkinsRule;

@SuppressWarnings({ "PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods", "PMD.AvoidUsingHardCodedIP" })
public class CifsHostConfigurationTest {

    private static final String CFG_NAME = "xxx";
    private static final String SERVER = "myServer";
    private static final String SHARE = "myShare";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule() {
        @Override
        public void before() throws Throwable {
            super.before();
            SecretHelper.setSecretKey();
        }

        @Override
        public void after() throws Exception {
            super.after();
            SecretHelper.clearSecretKey();
        }
    };

    private final transient BPBuildInfo buildInfo = new BPBuildInfo(TaskListener.NULL, "", new FilePath(new File("")), null, null);
    private final transient IMocksControl mockControl = EasyMock.createStrictControl();
    private final transient DiskShare mockSmbFile = mockControl.createMock(DiskShare.class);

    @Test public void createSimplestUrl() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, "myShare/", mockSmbFile);
        assertUrl("", config);
    }

    @Test public void shareAlwaysEndsWithSeparator() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, SHARE, mockSmbFile);
        assertUrl("", config);
    }

    @Test public void shareCanBeAbsolute() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, "/myShare", mockSmbFile);
        assertUrl("", config);
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

    @Test public void fixupUnixSeparatorsInShare() throws Exception {
        final CifsHostConfiguration config = new ConfigWithMockFile(CFG_NAME, SERVER, null, null, "myShare/and/subDirs", mockSmbFile);
        assertUrl("and\\subDirs", config);
    }

    private void assertUrl(final String expectedUrl, final CifsHostConfiguration hostConfig) throws Exception {
        expect(mockSmbFile.folderExists(expectedUrl)).andReturn(true);
        expect(mockSmbFile.list(expectedUrl)).andReturn(Collections.emptyList());
        mockControl.replay();
        assertEquals(expectedUrl, hostConfig.createClient(buildInfo).getContext());
        mockControl.verify();
    }

    private static class ConfigWithMockFile extends CifsHostConfiguration {
        private static final long serialVersionUID = 1L;
        private final transient DiskShare smbFile;
        ConfigWithMockFile(final String configName,
                           final String hostname,
                           final String username,
                           final String password,
                           final String remoteRootDir,
                           final DiskShare smbFile) {
            this(configName, hostname, username, password, remoteRootDir, DEFAULT_PORT, DEFAULT_TIMEOUT, smbFile);
        }
        ConfigWithMockFile(final String configName,
                           final String hostname,
                           final String username,
                           final String password,
                           final String remoteRootDir,
                           final int port,
                           final int timeout,
                           final DiskShare smbFile) {
            super(configName, hostname, username, password, remoteRootDir, port, timeout);
            this.smbFile = smbFile;
        }

        @Override
        protected void execute(AuthenticationContext auth, Consumer task) throws Exception {
            task.apply(this.smbFile);
        }
    }
}
