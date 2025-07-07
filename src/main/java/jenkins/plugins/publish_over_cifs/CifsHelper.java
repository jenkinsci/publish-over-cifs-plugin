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

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jenkins.plugins.publish_over.BapPublisherException;

public class CifsHelper {

    public SmbFile[] listFiles(final SmbFile file, final String url) {
        try {
            return file.listFiles();
        } catch (final SmbException smbe) {
            throw new BapPublisherException(Messages.exception_jCifsException_listFiles(
                                              hideUserInfo(url), smbe.getLocalizedMessage()), smbe);
        }
    }

    public void delete(final SmbFile file) {
        try {
            file.delete();
        } catch (final SmbException smbe) {
            throw new BapPublisherException(Messages.exception_jCifsException_delete(
                    hideUserInfo(file.getCanonicalPath()), smbe.getLocalizedMessage()), smbe);
        }
    }

    public boolean exists(final SmbFile file, final String url) {
        try {
            return file.exists();
        } catch (final SmbException smbe) {
            throw new BapPublisherException(Messages.exception_jCifsException_exists(
                    hideUserInfo(url), smbe.getLocalizedMessage()), smbe);
        }
    }

    public boolean canRead(final SmbFile file, final String url) {
        try {
            return file.canRead();
        } catch (final SmbException smbe) {
            throw new BapPublisherException(Messages.exception_jCifsException_canRead(
                    hideUserInfo(url), smbe.getLocalizedMessage()), smbe);
        }
    }

    public void mkdirs(final SmbFile file, final String url) {
        try {
            file.mkdirs();
        } catch (final SmbException smbe) {
            throw new BapPublisherException(Messages.exception_jCifsException_mkdirs(
                    hideUserInfo(url), smbe.getLocalizedMessage()), smbe);
        }
    }

    public String hideUserInfo(final String url) {
        if (url.contains("@"))
            return CifsHostConfiguration.SMB_URL_PREFIX + "******" + url.substring(url.indexOf('@'));
        else
            return url;
    }

}
