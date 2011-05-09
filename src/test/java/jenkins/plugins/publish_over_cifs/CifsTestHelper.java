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
import jenkins.plugins.publish_over.BPBuildEnv;
import jenkins.plugins.publish_over.BPBuildInfo;

import java.io.File;
import java.util.Calendar;
import java.util.TreeMap;

public class CifsTestHelper {

    public static BPBuildEnv createEmptyBuildEnv() {
        return new BPBuildEnv(new TreeMap<String, String>(), new FilePath(new File("")), Calendar.getInstance());
    }

    public static BPBuildInfo createEmpty() {
        return createEmpty(true);
    }

    public static BPBuildInfo createEmpty(final boolean setEffectiveEnvironment) {
        final BPBuildInfo buildInfo = new FakeBuildInfo();
        if (setEffectiveEnvironment) {
            buildInfo.setBuildTime(buildInfo.getCurrentBuildEnv().getBuildTime());
            buildInfo.setBaseDirectory(buildInfo.getCurrentBuildEnv().getBaseDirectory());
            buildInfo.setEnvVars(buildInfo.getCurrentBuildEnv().getEnvVars());
        }
        return buildInfo;
    }

    public static void setOnMaster(final BPBuildInfo buildInfo) {
        ((FakeBuildInfo) buildInfo).onMaster = true;
    }

    public static class FakeBuildInfo extends BPBuildInfo {
        private boolean onMaster;
        public FakeBuildInfo() {
            super(TaskListener.NULL, "", new FilePath(new File("")), createEmptyBuildEnv(), null);
        }
        @Override
        public boolean onMaster() {
            return onMaster;
        }
    }   

}
