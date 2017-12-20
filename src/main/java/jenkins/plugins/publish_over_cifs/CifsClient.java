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
import jcifs.smb.NtlmPasswordAuthentication;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPDefaultClient;
import jenkins.plugins.publish_over.BapPublisherException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

public class CifsClient extends BPDefaultClient<CifsTransfer> {

    private final CifsHelper helper = new CifsHelper();
    private final BPBuildInfo buildInfo;
    private final String baseUrl;
    private final NtlmPasswordAuthentication auth;
    private String context;
    private int bufferSize;

    public CifsClient(final BPBuildInfo buildInfo, final String baseUrl,
	NtlmPasswordAuthentication auth, final int bufferSize) {
        this.buildInfo = buildInfo;
        this.baseUrl = baseUrl;
        this.auth = auth;
        this.bufferSize = bufferSize;
        context = baseUrl;
    }

    protected String getContext() { return context; }

    @Override
    public boolean changeToInitialDirectory() {
        context = baseUrl;
        return true;
    }

    public boolean changeDirectory(final String directory) {
        final String newLocation = createUrlForSubDir(directory);
        final SmbFile dir = createFile(newLocation);
        if (helper.exists(dir, newLocation) && helper.canRead(dir, newLocation)) {
            context = newLocation;
            return true;
        } else {
            return false;
        }
    }

    private String createUrlForSubDir(final String directory) {
        return directory.endsWith("/") ? context + directory : context + directory + '/';
    }

    public boolean makeDirectory(final String directory) {
        final String newDirectoryUrl = createUrlForSubDir(directory);
        final SmbFile dir = createFile(newDirectoryUrl);
        if (helper.exists(dir, newDirectoryUrl)) throw new BapPublisherException(
                Messages.exception_mkdir_directoryExists(helper.hideUserInfo(newDirectoryUrl)));
        if (buildInfo.isVerbose()) buildInfo.println(Messages.console_mkdir(helper.hideUserInfo(newDirectoryUrl)));
        helper.mkdirs(dir, newDirectoryUrl);
        return true;
    }

    public void deleteTree() throws IOException {
        if (buildInfo.isVerbose()) buildInfo.println(Messages.console_clean(helper.hideUserInfo(context)));
        final SmbFile[] files = helper.listFiles(createFile(context), context);
        if (files == null) throw new BapPublisherException(Messages.exception_listFilesReturnedNull(helper.hideUserInfo(context)));
        for (final SmbFile file : files) {
            if (buildInfo.isVerbose()) buildInfo.println(Messages.console_delete(helper.hideUserInfo(file.getCanonicalPath())));
            helper.delete(file);
        }
    }

    public void beginTransfers(final CifsTransfer transfer) {
        if (!transfer.hasConfiguredSourceFiles())
            throw new BapPublisherException(Messages.exception_noSourceFiles());
    }

    public void transferFile(final CifsTransfer transfer, final FilePath filePath, final InputStream content) throws IOException {
        final String newFileUrl = context + filePath.getName();
        if (buildInfo.isVerbose()) buildInfo.println(Messages.console_copy(helper.hideUserInfo(newFileUrl)));
        final OutputStream out = createFile(newFileUrl).getOutputStream();
        try {
            IOUtils.copy(content, out, bufferSize);
        } finally {
            out.close();
        }
    }

    public void disconnect() {
    }

    public void disconnectQuietly() {
    }

    @SuppressWarnings("PMD.PreserveStackTrace") // security
    private SmbFile createFile(final String url) {
        try {
            return createSmbFile(url);
        } catch (MalformedURLException mue) {
            throw new BapPublisherException(Messages.exception_maformedUrlException(helper.hideUserInfo(url)));
        }
    }

    protected SmbFile createSmbFile(final String url) throws MalformedURLException {
	if(auth != null) {
            return new SmbFile(url, auth);
        } else {
            return new SmbFile(url);
        }
    }

}
