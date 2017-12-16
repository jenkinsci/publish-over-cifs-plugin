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
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import hudson.FilePath;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPDefaultClient;
import jenkins.plugins.publish_over.BapPublisherException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CifsClient extends BPDefaultClient<CifsTransfer> {

    private final BPBuildInfo buildInfo;
    private final String server;
    private final String shareName;
    private final String initialContext;
    private final AuthenticationContext auth;
    private String context;

    public CifsClient(final BPBuildInfo buildInfo, final String server, final String remoteRootDir, AuthenticationContext auth) {
        this.buildInfo = buildInfo;
        this.server = server;
        this.shareName = getShare(remoteRootDir);
        this.auth = auth;
        String subfolder = fix(getSubfolder(remoteRootDir));
        this.initialContext = context = subfolder;
    }

    protected String getContext() { return context; }

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

    @Override
    public boolean changeToInitialDirectory() {
        context = initialContext;
        return true;
    }

    public boolean changeDirectory(final String directory) {
        final String newLocation = createUrlForSubDir(directory);
        try {
            return execute(share -> {
                if (share.folderExists(fix(newLocation)) && share.list(fix(newLocation)).size() >= 0) {
                    context = newLocation;
                    return true;
                }

                return false;
            });
        } catch (Exception e) {
            return false;
        }
    }

    private String createUrlForSubDir(String directory) {
        String dir = fix(context + (context.endsWith("\\") ? "" : "\\") + directory);
        while (dir.endsWith("\\")) {
            dir = dir.substring(0, dir.length() - 1);
        }

        return dir;
    }

    public boolean makeDirectory(final String directory) {
        // if the directory to create is nested, create all intermediate directories
        String[] dirs = fix(directory).split("\\\\");
        String intermediatePath = dirs[0];
        for (int i = 1; i < dirs.length; i++) {
            final String fullIntermediatePath = createUrlForSubDir(intermediatePath);
            try {
                executeVoid(share -> {
                    if (!share.folderExists(fullIntermediatePath)) {
                        if (buildInfo.isVerbose()) {
                            buildInfo.println(Messages.console_mkdir(fullIntermediatePath));
                        }

                        share.mkdir(fullIntermediatePath);
                    }
                });
            }
            catch (Exception e) {
                throw new BapPublisherException(e);
            }

            intermediatePath += "\\" + dirs[i];
        }

        final String newDirectoryUrl = createUrlForSubDir(directory);
        try {
            executeVoid(share -> {
                if (share.folderExists(newDirectoryUrl)) {
                    throw new BapPublisherException(Messages.exception_mkdir_directoryExists(newDirectoryUrl));
                }

                if (buildInfo.isVerbose()) {
                    buildInfo.println(Messages.console_mkdir(newDirectoryUrl));
                }

                share.mkdir(newDirectoryUrl);
            });

            return true;
        } catch (Exception e) {
            throw new BapPublisherException(e);
        }
    }

    public void deleteTree() throws IOException {
        if (buildInfo.isVerbose()){
            buildInfo.println(Messages.console_clean(context));
        }

        try {
            executeVoid(share -> share.rmdir(fix(context), true));
        } catch (Exception smbe) {
            throw new BapPublisherException(
                    Messages.exception_jCifsException_delete(context, smbe.getLocalizedMessage()));
        }
    }

    public void beginTransfers(final CifsTransfer transfer) {
        if (!transfer.hasConfiguredSourceFiles())
            throw new BapPublisherException(Messages.exception_noSourceFiles());
    }

    public void transferFile(final CifsTransfer transfer, final FilePath filePath, final InputStream content) throws IOException {
        final String newFileUrl = context + (context.endsWith("\\") ? "" : "\\") + filePath.getName();
        if (buildInfo.isVerbose()) {
            buildInfo.println(Messages.console_copy(newFileUrl));
        }

        try {
            executeVoid(share -> {
                File f = share.openFile(fix(newFileUrl),
                        EnumSet.of(AccessMask.GENERIC_WRITE),
                        null,
                        SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OVERWRITE_IF,
                        null);
                try (OutputStream out = f.getOutputStream()) {
                    IOUtils.copy(content, out);
                }
                finally {
                    f.closeSilently();
                }
            });
        } catch (Exception e) {
            throw new BapPublisherException("Cannot transfer file " + newFileUrl, e);
        }
    }

    public void disconnect() {
    }

    public void disconnectQuietly() {
    }

    private String fix(String dir) {
        dir = dir.replace('/', '\\');
        while (dir.startsWith("\\")) {
            dir = dir.substring(1);
        }

        return dir;
    }

    @FunctionalInterface
    interface Function<T> {
        T apply(DiskShare share) throws Exception;
    }

    @FunctionalInterface
    interface Consumer {
        void apply(DiskShare share) throws Exception;
    }

    private void executeVoid(Consumer task) throws Exception {
        execute(share -> {
            task.apply(share);
            return null;
        });
    }

    protected <T> T execute(Function<T> task) throws Exception {
        SMBClient client = new SMBClient();
        try (Connection connection = client.connect(server)) {
            Session session = connection.authenticate(auth);

            // Connect to Share
            try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                return task.apply(share);
            }
        }
    }
}
