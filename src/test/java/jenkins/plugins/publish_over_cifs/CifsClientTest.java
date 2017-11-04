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
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import hudson.FilePath;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BapPublisherException;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({ "PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods" })
public class CifsClientTest {

    private static final String TEST_SERVER = "server";
    private static final String TEST_ROOT_SHARE = "share";
    private static final String NEW_DIR = "new\\dir";
    private final transient IMocksControl mockControl = EasyMock.createStrictControl();
    private final transient DiskShare mockShare = mockControl.createMock(DiskShare.class);
    private final BPBuildInfo buildInfo = CifsTestHelper.createEmpty();

    @Test public void initialContextIsEmpty() {
        assertEquals("", new CifsClient(buildInfo, TEST_SERVER, TEST_ROOT_SHARE, null).getContext());
    }

    @Test public void changeDirectoryUpdatesContext() throws Exception {
        final String expectedUrl = NEW_DIR + "\\";
        final CifsClient cifsClient = new CifsClientWithMockFiles(expectedUrl);
        expect(mockShare.folderExists(fix(expectedUrl))).andReturn(true);
        expect(mockShare.list(fix(expectedUrl))).andReturn(Collections.emptyList());
        mockControl.replay();
        assertTrue(cifsClient.changeDirectory(NEW_DIR));
        assertEquals(expectedUrl, cifsClient.getContext());
        mockControl.verify();
    }

    @Test public void changeDirectoryDoesNotUpdateContextIfDirNotExist() throws Exception {
        final String absUrl = NEW_DIR + "\\";
        final CifsClient cifsClient = new CifsClientWithMockFiles(absUrl);
        expect(mockShare.folderExists(fix(absUrl))).andReturn(false);
        mockControl.replay();
        assertFalse(cifsClient.changeDirectory(NEW_DIR));
        assertEquals("", cifsClient.getContext());
        mockControl.verify();
    }

    @Test public void changeDirectoryDoesNotUpdateContextIfCannotReadDir() throws Exception {
        final String absUrl = NEW_DIR + "/";
        final CifsClient cifsClient = new CifsClientWithMockFiles(absUrl);
        expect(mockShare.folderExists(fix(absUrl))).andReturn(true);
        expect(mockShare.list(fix(absUrl))).andThrow(new TransportException("cannot list"));
        mockControl.replay();
        assertFalse(cifsClient.changeDirectory(NEW_DIR));
        assertEquals("", cifsClient.getContext());
        mockControl.verify();
    }

    @Test public void changeToInitialDirectoryResetsContext() throws Exception {
        final CifsClient cifsClient = new CifsClientWithMockFiles(NEW_DIR);
        expect(mockShare.folderExists(fix(NEW_DIR + "/"))).andReturn(true);
        expect(mockShare.list(fix(NEW_DIR + "/"))).andReturn(Collections.emptyList());
        mockControl.replay();
        assertTrue(cifsClient.changeDirectory(NEW_DIR));
        cifsClient.changeToInitialDirectory();
        assertEquals("", cifsClient.getContext());
        mockControl.verify();
    }

    @Test public void testMakeDirectory() throws Exception {
        final String absUrl = NEW_DIR + "/";
        final CifsClient cifsClient = new CifsClientWithMockFiles(absUrl);
        expect(mockShare.folderExists(fix(absUrl))).andReturn(false);
        mockShare.mkdir(fix(absUrl));
        mockControl.replay();
        assertTrue(cifsClient.makeDirectory(NEW_DIR));
        assertEquals("", cifsClient.getContext());
        mockControl.verify();
    }

    @Test public void testMakeDirectoryDoesNotAddAnExtraFS() throws Exception {
        final String directory = "new/dir/";
        final CifsClient cifsClient = new CifsClientWithMockFiles(directory);
        expect(mockShare.folderExists(fix(directory))).andReturn(false);
        mockShare.mkdir(fix(directory));
        mockControl.replay();
        assertTrue(cifsClient.makeDirectory(directory));
        assertEquals("", cifsClient.getContext());
        mockControl.verify();
    }

    @Test
    public void makeDirectoryThrowsExceptionIfDirectoryExists() throws Exception {
        final String absUrl = NEW_DIR + "\\";
        final CifsClient cifsClient = new CifsClientWithMockFiles(absUrl);
        expect(mockShare.folderExists(fix(absUrl))).andReturn(true);
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
        final CifsClient cifsClient = new CifsClientWithMockFiles(fileName);
        File mockFile = mockControl.createMock(File.class);
        expect(mockShare.openFile(fileName,
                EnumSet.of(AccessMask.GENERIC_WRITE),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                null)).andReturn(mockFile);
        expect(mockFile.getOutputStream()).andReturn(baos);
        mockFile.closeSilently();
        mockControl.replay();
        cifsClient.transferFile(null, new FilePath(new java.io.File(fileName)), new ByteArrayInputStream(fileContents.getBytes()));
        assertEquals(fileContents, baos.toString());
        mockControl.verify();
    }

    @Test public void testBeginTransfersFailIfNoSourceFiles() throws Exception {
        try {
            new CifsClient(buildInfo, TEST_SERVER, TEST_ROOT_SHARE, null)
                    .beginTransfers(new CifsTransfer("", "", "", "", false, false, false, false, false, ","));
            fail();
        } catch (BapPublisherException bpe) {
            assertEquals(Messages.exception_noSourceFiles(), bpe.getMessage());
        }
    }

    @Test public void testDeleteTree() throws Exception {
        final CifsClient client = new CifsClientWithMockFiles(NEW_DIR);
        mockShare.rmdir("", true);
        mockControl.replay();
        client.deleteTree();
        mockControl.verify();
    }

    private String fix(String expectedUrl) {
        return expectedUrl.replace('/', '\\');
    }

    private class CifsClientWithMockFiles extends CifsClient {
        private final Iterator<String> expectedUrls;
        public CifsClientWithMockFiles(final String... expectedUrls) {
            super(buildInfo, TEST_SERVER, TEST_ROOT_SHARE, null);
            this.expectedUrls = Arrays.asList(expectedUrls).iterator();
        }

        @Override
        protected <T> T execute(Function<T> task) throws Exception {
//            assertTrue(expectedUrls.hasNext());
//            assertEquals(expectedUrls.next(), url);
            return task.apply(mockShare);
        }
    }
}
