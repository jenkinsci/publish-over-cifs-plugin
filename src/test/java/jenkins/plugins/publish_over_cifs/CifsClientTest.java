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
import jcifs.smb.SmbFile;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BapPublisherException;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Iterator;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({ "PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods" })
public class CifsClientTest {

    private static final int BUFFER_SIZE = 4096;
    private static final String TEST_ROOT_URL = "smb://server/share/";
    private static final String NEW_DIR = "new/dir";
    private final transient IMocksControl mockControl = EasyMock.createStrictControl();
    private final transient SmbFile mockSmbFile = mockControl.createMock(SmbFile.class);
    private final BPBuildInfo buildInfo = CifsTestHelper.createEmpty();

    @Test public void initialContextIsRootUrl() {
        assertEquals(TEST_ROOT_URL, new CifsClient(buildInfo, TEST_ROOT_URL, null, BUFFER_SIZE).getContext());
    }

    @Test public void changeDirectoryUpdatesContext() throws Exception {
        final String expectedUrl = TEST_ROOT_URL + NEW_DIR + "/";
        final CifsClient cifsClient = new CifsClientWithMockFiles(expectedUrl);
        expect(mockSmbFile.exists()).andReturn(true);
        expect(mockSmbFile.canRead()).andReturn(true);
        mockControl.replay();
        assertTrue(cifsClient.changeDirectory(NEW_DIR));
        assertEquals(expectedUrl, cifsClient.getContext());
        mockControl.verify();
    }

    @Test public void changeDirectoryDoesNotUpdateContextIfDirNotExist() throws Exception {
        final String absUrl = TEST_ROOT_URL + NEW_DIR + "/";
        final CifsClient cifsClient = new CifsClientWithMockFiles(absUrl);
        expect(mockSmbFile.exists()).andReturn(false);
        mockControl.replay();
        assertFalse(cifsClient.changeDirectory(NEW_DIR));
        assertEquals(TEST_ROOT_URL, cifsClient.getContext());
        mockControl.verify();
    }

    @Test public void changeDirectoryDoesNotUpdateContextIfCannotReadDir() throws Exception {
        final String absUrl = TEST_ROOT_URL + NEW_DIR + "/";
        final CifsClient cifsClient = new CifsClientWithMockFiles(absUrl);
        expect(mockSmbFile.exists()).andReturn(true);
        expect(mockSmbFile.canRead()).andReturn(false);
        mockControl.replay();
        assertFalse(cifsClient.changeDirectory(NEW_DIR));
        assertEquals(TEST_ROOT_URL, cifsClient.getContext());
        mockControl.verify();
    }

    @Test public void changeToInitialDirectoryResetsContext() throws Exception {
        final CifsClient cifsClient = new CifsClientWithMockFiles(TEST_ROOT_URL + NEW_DIR + "/");
        expect(mockSmbFile.exists()).andReturn(true);
        expect(mockSmbFile.canRead()).andReturn(true);
        mockControl.replay();
        assertTrue(cifsClient.changeDirectory(NEW_DIR));
        cifsClient.changeToInitialDirectory();
        assertEquals(TEST_ROOT_URL, cifsClient.getContext());
        mockControl.verify();
    }

    @Test public void testMakeDirectory() throws Exception {
        final String absUrl = TEST_ROOT_URL + NEW_DIR + "/";
        final CifsClient cifsClient = new CifsClientWithMockFiles(absUrl);
        expect(mockSmbFile.exists()).andReturn(false);
        mockSmbFile.mkdirs();
        mockControl.replay();
        assertTrue(cifsClient.makeDirectory(NEW_DIR));
        assertEquals(TEST_ROOT_URL, cifsClient.getContext());
        mockControl.verify();
    }

    @Test public void testMakeDirectoryDoesNotAddAnExtraFS() throws Exception {
        final String directory = "new/dir/";
        final String absUrl = TEST_ROOT_URL + directory;
        final CifsClient cifsClient = new CifsClientWithMockFiles(absUrl);
        expect(mockSmbFile.exists()).andReturn(false);
        mockSmbFile.mkdirs();
        mockControl.replay();
        assertTrue(cifsClient.makeDirectory(directory));
        assertEquals(TEST_ROOT_URL, cifsClient.getContext());
        mockControl.verify();
    }

    @Test public void makeDirectoryThrowsExceptionIfDirectoryExists() throws Exception {
        final String absUrl = TEST_ROOT_URL + NEW_DIR + "/";
        final CifsClient cifsClient = new CifsClientWithMockFiles(absUrl);
        expect(mockSmbFile.exists()).andReturn(true);
        mockControl.replay();
        try {
            cifsClient.makeDirectory(NEW_DIR);
            fail();
        } catch (final BapPublisherException bpe) {
            assertTrue(bpe.getLocalizedMessage().contains(absUrl));
        }
        mockControl.verify();
    }

    @Test public void testTransferFile() throws Exception {
        final String fileContents = "Hello Mr. Windows share!";
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String fileName = "anAwesomeFile";
        final CifsClient cifsClient = new CifsClientWithMockFiles(TEST_ROOT_URL + fileName);
        expect(mockSmbFile.getOutputStream()).andReturn(baos);
        mockControl.replay();
        cifsClient.transferFile(null, new FilePath(new File(fileName)), new ByteArrayInputStream(fileContents.getBytes()));
        assertEquals(fileContents, baos.toString());
        mockControl.verify();
    }

    @Test public void testBeginTransfersFailIfNoSourceFiles() throws Exception {
        try {
            new CifsClient(buildInfo, TEST_ROOT_URL, null, BUFFER_SIZE).beginTransfers(new CifsTransfer("", "", "", "", false, false, false, false, false, ","));
            fail();
        } catch (BapPublisherException bpe) {
            assertEquals(Messages.exception_noSourceFiles(), bpe.getMessage());
        }
    }

    @Test public void testDeleteTree() throws Exception {
        final CifsClient client = new CifsClientWithMockFiles(TEST_ROOT_URL);
        final SmbFile toDelete1 = mockControl.createMock(SmbFile.class);
        final SmbFile toDelete2 = mockControl.createMock(SmbFile.class);
        final SmbFile toDelete3 = mockControl.createMock(SmbFile.class);
        expect(mockSmbFile.listFiles()).andReturn(new SmbFile[]{toDelete1, toDelete2, toDelete3});
        toDelete1.delete();
        toDelete2.delete();
        toDelete3.delete();
        mockControl.replay();
        client.deleteTree();
        mockControl.verify();
    }

    private class CifsClientWithMockFiles extends CifsClient {
        private final Iterator<String> expectedUrls;
        public CifsClientWithMockFiles(final String... expectedUrls) {
            super(buildInfo, TEST_ROOT_URL, null, BUFFER_SIZE);
            this.expectedUrls = Arrays.asList(expectedUrls).iterator();
        }
        @Override
        protected SmbFile createSmbFile(final String url) throws MalformedURLException {
            assertTrue(expectedUrls.hasNext());
            assertEquals(expectedUrls.next(), url);
            return mockSmbFile;
        }
    }
}
