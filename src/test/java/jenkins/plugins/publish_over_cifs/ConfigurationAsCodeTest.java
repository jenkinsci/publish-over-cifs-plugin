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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.util.Secret;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import jenkins.plugins.publish_over_cifs.options.CifsOverrideDefaults;
import jenkins.plugins.publish_over_cifs.options.CifsPluginDefaults;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeTest {

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void shouldImportAllSystemConfiguration(JenkinsConfiguredWithCodeRule rule) {
        assertSystemConfiguration(rule);
    }

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void shouldExportAndReapplyAllSystemConfiguration(JenkinsConfiguredWithCodeRule rule) throws Exception {
        String exported = rule.exportToString(true);

        assertTrue(exported.contains("cifsPublisher:"));
        assertTrue(exported.contains("overrideDefaults:"));
        assertTrue(exported.contains("winsServer: \"192.0.2.10\""));
        assertFalse(exported.contains("cifs-secret"));

        CifsPublisherPlugin.Descriptor descriptor =
                rule.jenkins.getDescriptorByType(CifsPublisherPlugin.Descriptor.class);
        descriptor.setHostConfigurations(List.of());
        descriptor.setDefaults(new CifsPluginDefaults());
        rule.jenkins.getGlobalNodeProperties().clear();

        Path exportedFile = Files.createTempFile(rule.jenkins.getRootDir().toPath(), "cifs-casc-", ".yml");
        Files.writeString(exportedFile, exported);
        ConfigurationAsCode.get().configure(exportedFile.toString());

        assertSystemConfiguration(rule);
    }

    private static void assertSystemConfiguration(JenkinsConfiguredWithCodeRule rule) {
        CifsPublisherPlugin.Descriptor descriptor =
                rule.jenkins.getDescriptorByType(CifsPublisherPlugin.Descriptor.class);

        assertEquals(1, descriptor.getHostConfigurations().size());
        CifsHostConfiguration host = descriptor.getHostConfigurations().get(0);
        assertEquals("primary", host.getName());
        assertEquals("files.example.test", host.getHostname());
        assertEquals("EXAMPLE\\jenkins", host.getUsername());
        assertEquals("cifs-secret", Secret.toString(Secret.decrypt(host.getEncryptedPassword())));
        assertEquals("artifacts/releases", host.getRemoteRootDir());
        assertEquals(445, host.getPort());
        assertEquals(45000, host.getTimeout());
        assertEquals(8192, host.getBufferSize());
        assertEquals(CifsHostConfiguration.SmbVersions.SMB_V3, host.getSmbVersion());

        CifsOverrideDefaults defaults = assertInstanceOf(CifsOverrideDefaults.class, descriptor.getDefaults());
        assertTrue(defaults.getOverrideInstanceConfig().isAlwaysPublishFromMaster());
        assertTrue(defaults.getOverrideInstanceConfig().isContinueOnError());
        assertTrue(defaults.getOverrideInstanceConfig().isFailOnError());
        assertTrue(defaults.getOverrideInstanceConfig().isPublishWhenFailed());
        assertEquals("CIFS_PUBLISH", defaults.getOverrideParamPublish().getParameterName());
        assertEquals("primary", defaults.getOverridePublisher().getConfigName());
        assertTrue(defaults.getOverridePublisher().isUseWorkspaceInPromotion());
        assertTrue(defaults.getOverridePublisher().isUsePromotionTimestamp());
        assertTrue(defaults.getOverridePublisher().isVerbose());
        assertEquals("release", defaults.getOverridePublisherLabel().getLabel());
        assertEquals(4, defaults.getOverrideRetry().getRetries());
        assertEquals(15000, defaults.getOverrideRetry().getRetryDelay());
        assertEquals("target/*.zip", defaults.getOverrideTransfer().getSourceFiles());
        assertEquals("target/*-sources.zip", defaults.getOverrideTransfer().getExcludes());
        assertEquals("target", defaults.getOverrideTransfer().getRemovePrefix());
        assertEquals("builds", defaults.getOverrideTransfer().getRemoteDirectory());
        assertTrue(defaults.getOverrideTransfer().isFlatten());
        assertTrue(defaults.getOverrideTransfer().isRemoteDirectorySDF());
        assertTrue(defaults.getOverrideTransfer().isCleanRemote());
        assertTrue(defaults.getOverrideTransfer().isNoDefaultExcludes());
        assertTrue(defaults.getOverrideTransfer().isMakeEmptyDirs());
        assertEquals("[, ]+", defaults.getOverrideTransfer().getPatternSeparator());

        CifsNodeProperties nodeProperties = rule.jenkins.getGlobalNodeProperties().get(CifsNodeProperties.class);
        assertEquals("192.0.2.10", nodeProperties.getWinsServer());
    }
}