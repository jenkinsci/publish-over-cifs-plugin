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

import jenkins.plugins.publish_over.BPBuildInfo;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static jenkins.plugins.publish_over_cifs.CifsPublisher.*;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CifsPublisherTest {

    private final BPBuildInfo buildInfo = CifsTestHelper.createEmpty();
    private final IMocksControl mockControl = EasyMock.createNiceControl();
    private final CifsClient mockClient = mockControl.createMock(CifsClient.class);
    private final CifsHostConfiguration mockConfig = mockControl.createMock(CifsHostConfiguration.class);

    @Before public void setUp() {
        expect(mockConfig.createClient(buildInfo)).andReturn(mockClient);
    }

    @Test public void noWinsServerIfNoDefaultsAndNoneForNode() throws Exception {
        CifsTestHelper.setOnMaster(buildInfo);
        mockControl.replay();
        createPublisher().perform(mockConfig, buildInfo);
        assertNull(buildInfo.get(CifsPublisher.CTX_KEY_WINS_SERVER));
    }

    @Test public void noWinsServerIfNoDefaultsAndOnMaster() throws Exception {
        mockControl.replay();
        createPublisher().perform(mockConfig, buildInfo);
        assertNull(buildInfo.get(CifsPublisher.CTX_KEY_WINS_SERVER));
    }

    @Test public void winsServerFromCurrent() throws Exception {
        final String winsServer = "1.2.3.4";
        final CifsCleanNodeProperties current = new CifsCleanNodeProperties(winsServer);
        buildInfo.put(CTX_KEY_NODE_PROPERTIES_CURRENT, current);
        mockControl.replay();
        createPublisher().perform(mockConfig, buildInfo);
        assertEquals(winsServer, buildInfo.get(CTX_KEY_WINS_SERVER));
    }

    @Test public void winsServerDefaultIfOnMaster() throws Exception {
        final String defaultServer = "1.2.3.4";
        final String currentServer = "5.6.7.8";
        final CifsCleanNodeProperties current = new CifsCleanNodeProperties(currentServer);
        final CifsCleanNodeProperties defautls = new CifsCleanNodeProperties(defaultServer);
        buildInfo.put(CTX_KEY_NODE_PROPERTIES_CURRENT, current);
        buildInfo.put(CTX_KEY_NODE_PROPERTIES_DEFAULT, defautls);
        CifsTestHelper.setOnMaster(buildInfo);
        mockControl.replay();
        createPublisher().perform(mockConfig, buildInfo);
        assertEquals(defaultServer, buildInfo.get(CTX_KEY_WINS_SERVER));
    }

    @Test public void noWinsServerIfWinsServerEmptyInCurrent() throws Exception {
        final String defaultServer = "1.2.3.4";
        final CifsCleanNodeProperties current = new CifsCleanNodeProperties(null);
        final CifsCleanNodeProperties defaults = new CifsCleanNodeProperties(defaultServer);
        buildInfo.put(CTX_KEY_NODE_PROPERTIES_CURRENT, current);
        buildInfo.put(CTX_KEY_NODE_PROPERTIES_DEFAULT, defaults);
        mockControl.replay();
        createPublisher().perform(mockConfig, buildInfo);
        assertNull(buildInfo.get(CTX_KEY_WINS_SERVER));
    }

    private CifsPublisher createPublisher() {
        return new CifsPublisher("abc", false, new ArrayList<CifsTransfer>(0), false, false);
    }
}
