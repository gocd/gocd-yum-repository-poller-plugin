/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tw.go.plugin.material.artifactrepository.yum.exec;

import com.tw.go.plugin.material.artifactrepository.yum.exec.message.CheckConnectionResultMessage;
import com.tw.go.plugin.material.artifactrepository.yum.exec.message.PackageMaterialProperties;
import com.tw.go.plugin.material.artifactrepository.yum.exec.message.PackageMaterialProperty;
import com.tw.go.plugin.material.artifactrepository.yum.exec.message.PackageRevisionMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class PackageRepositoryPollerTest {
    private PackageMaterialProperties repositoryConfiguration;
    private PackageMaterialProperties packageConfiguration;
    private File sampleRepoDirectory;
    private String repoUrl;
    private PackageRepositoryPoller poller;

    @BeforeEach
    public void setup() throws IOException {
        RepoqueryCacheCleaner.performCleanup();
        sampleRepoDirectory = new File("src/test/repos/samplerepo");
        repoUrl = "file://" + sampleRepoDirectory.getAbsolutePath();

        repositoryConfiguration = new PackageMaterialProperties();
        repositoryConfiguration.addPackageMaterialProperty(Constants.REPO_URL, new PackageMaterialProperty().withValue(repoUrl));

        packageConfiguration = new PackageMaterialProperties();
        packageConfiguration.addPackageMaterialProperty(Constants.PACKAGE_SPEC, new PackageMaterialProperty().withValue("go-agent"));

        PackageRepositoryConfigurationProvider configurationProvider = new PackageRepositoryConfigurationProvider();
        poller = new PackageRepositoryPoller(configurationProvider);
    }

    @Test
    public void shouldGetLatestModificationGivenPackageAndRepoConfigurations_getLatestRevision() {
        final PackageRevisionMessage latestRevision = poller.getLatestRevision(packageConfiguration, repositoryConfiguration);
        final PackageRevisionMessage expected = new PackageRevisionMessage("go-agent-13.1.1-16714.noarch", new Date(fromEpochTime(1365054258L)), null, null, null);
        assertPackageRevisionMessageEquivalent(expected, latestRevision);
        assertPackageLocationData("/go-agent-13.1.1-16714.noarch.rpm", latestRevision);
    }

    @Test
    public void shouldThrowExceptionWhileGettingLatestRevisionIfCheckConnectionFails_getLatestRevision() {
        repositoryConfiguration = new PackageMaterialProperties();
        repositoryConfiguration.addPackageMaterialProperty(Constants.REPO_URL, new PackageMaterialProperty().withValue("file://foo/bar"));
        try {
            poller.getLatestRevision(packageConfiguration, repositoryConfiguration);
        } catch (RuntimeException e) {
            assertEquals("Invalid file path.", e.getMessage());
        }
    }

    @Test
    public void shouldGetTheRightLocationForAnyPackage_getLatestRevision() {
        PackageMaterialProperties ppc = new PackageMaterialProperties();
        ppc.addPackageMaterialProperty(Constants.PACKAGE_SPEC, new PackageMaterialProperty().withValue("php"));
        final PackageRevisionMessage latestRevision = poller.getLatestRevision(ppc, repositoryConfiguration);
        final PackageRevisionMessage expected = new PackageRevisionMessage("php-0-0.noarch", new Date(fromEpochTime(1365053593)), null, null, null);
        assertPackageRevisionMessageEquivalent(expected, latestRevision);
        assertPackageLocationData("/innerFolder/php-0-0.noarch.rpm", latestRevision);
    }

    @Test
    public void shouldThrowExceptionGivenNonExistingRepo_getLatestRevision() {
        repositoryConfiguration = new PackageMaterialProperties();
        repositoryConfiguration.addPackageMaterialProperty(Constants.REPO_URL, new PackageMaterialProperty().withValue("file://junk-repo"));
        packageConfiguration = new PackageMaterialProperties();
        packageConfiguration.addPackageMaterialProperty(Constants.PACKAGE_SPEC, new PackageMaterialProperty().withValue("junk-artifact"));
        try {
            poller.getLatestRevision(packageConfiguration, repositoryConfiguration);
            fail("should have thrown exception");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().startsWith("Invalid file path."));
        }
    }

    @Test
    public void shouldThrowExceptionGivenNonExistingPackageInExistingRepo_getLatestRevision() {
        packageConfiguration = new PackageMaterialProperties();
        packageConfiguration.addPackageMaterialProperty(Constants.PACKAGE_SPEC, new PackageMaterialProperty().withValue("junk-artifact"));

        try {
            poller.getLatestRevision(packageConfiguration, repositoryConfiguration);
            fail("");
        } catch (RuntimeException e) {
            String expectedMessage = String.format("Error while querying repository with path '%s' and package spec '%s'.", repositoryConfiguration.getProperty(Constants.REPO_URL).value(), "junk-artifact");
            assertTrue(e.getMessage().startsWith(expectedMessage));
        }
    }

    @Test
    public void shouldThrowExceptionGivenEmptyRepo_getLatestRevision() {
        repositoryConfiguration = new PackageMaterialProperties();
        File emptyRepo = new File("src/test/repos/emptyrepo");
        repositoryConfiguration.addPackageMaterialProperty(Constants.REPO_URL, new PackageMaterialProperty().withValue("file://" + emptyRepo.getAbsolutePath()));
        packageConfiguration = new PackageMaterialProperties();
        packageConfiguration.addPackageMaterialProperty(Constants.PACKAGE_SPEC, new PackageMaterialProperty().withValue("junk-artifact"));
        try {
            poller.getLatestRevision(packageConfiguration, repositoryConfiguration);
            fail("");
        } catch (RuntimeException e) {
            String expectedMessage = String.format("Error while querying repository with path '%s' and package spec '%s'.", repositoryConfiguration.getProperty(Constants.REPO_URL).value(), "junk-artifact");
            assertTrue(e.getMessage().startsWith(expectedMessage));
        }
    }

    @Test
    public void shouldPerformRepositoryConfigurationBeforeModificationCheck_getLatestRevision() {
        packageConfiguration = new PackageMaterialProperties();
        packageConfiguration.addPackageMaterialProperty(Constants.PACKAGE_SPEC, new PackageMaterialProperty().withValue("junk-artifact"));
        try {
            poller.getLatestRevision(packageConfiguration, new PackageMaterialProperties());
            fail("should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals(("Repository url not specified"), e.getMessage());
        }
    }

    @Test
    public void shouldPerformPackageConfigurationBeforeModificationCheck() {
        repositoryConfiguration = new PackageMaterialProperties();
        File emptyRepo = new File("src/test/reposemptyrepo");
        repositoryConfiguration.addPackageMaterialProperty(Constants.REPO_URL, new PackageMaterialProperty().withValue("file://" + emptyRepo.getAbsolutePath()));
        try {
            poller.getLatestRevision(new PackageMaterialProperties(), repositoryConfiguration);
            fail("should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals(("Package spec not specified"), e.getMessage());
        }
    }

    @Test
    public void testShouldConcatenateErrorsWhenModificationCheckFails() {
        try {
            poller.getLatestRevision(new PackageMaterialProperties(), new PackageMaterialProperties());
            fail("should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals(("Repository url not specified; Package spec not specified"), e.getMessage());
        }
    }

    @Test
    public void shouldGetLatestModificationSinceGivenPackageAndRepoConfigurationsAndPreviouslyKnownRevision() {
        PackageRevisionMessage previousPackageRevision = new PackageRevisionMessage("symlinks-1.2-24.2.2.i386", new Date(fromEpochTime(1263710418L)), null, null, null);
        final PackageRevisionMessage latestRevision = poller.getLatestRevisionSince(packageConfiguration, repositoryConfiguration, previousPackageRevision);
        final PackageRevisionMessage expected = new PackageRevisionMessage("go-agent-13.1.1-16714.noarch", new Date(fromEpochTime(1365054258L)), null, null, null);
        assertPackageRevisionMessageEquivalent(expected, latestRevision);
    }

    @Test
    public void shouldReturnNullGivenPackageAndRepoConfigurationsAndPreviouslyKnownRevision() {
        PackageRevisionMessage packageRevisionMessage = new PackageRevisionMessage("go-agent-13.1.1-16714-noarch", new Date(fromEpochTime(1365054258L)), null, null, null);
        PackageRevisionMessage latestRevision = poller.getLatestRevisionSince(packageConfiguration, repositoryConfiguration, packageRevisionMessage);
        assertNull(latestRevision);
    }

    @Test
    public void shouldReturnNullWhenPreviouslyKnownPackageRevisionIsSameAsCurrent() {
        PackageRepositoryPoller spy = spy(poller);
        PackageRevisionMessage packageRevision = new PackageRevisionMessage("go-agent-13.1.1-16714-noarch", new Date(fromEpochTime(1365054258L)), null, null, null);
        when(spy.getLatestRevision(packageConfiguration, repositoryConfiguration)).thenReturn(packageRevision);
        PackageRevisionMessage latestRevision = poller.getLatestRevisionSince(packageConfiguration, repositoryConfiguration, new PackageRevisionMessage("go-agent-13.1.1-16714-noarch", new Date(fromEpochTime(1365054258L)), null, null, null));
        assertNull(latestRevision);
    }

    @Test
    public void shouldThrowExceptionIfCredentialsHaveBeenProvidedAlongWithFileProtocol() {
        repositoryConfiguration.addPackageMaterialProperty(Constants.USERNAME, new PackageMaterialProperty().withValue("loser"));
        repositoryConfiguration.addPackageMaterialProperty(Constants.PASSWORD, new PackageMaterialProperty().withValue("pwd"));
        try {
            poller.getLatestRevision(packageConfiguration, repositoryConfiguration);
            fail("Should have failed");
        } catch (Exception e) {
            assertEquals("File protocol does not support username and/or password.", e.getMessage());
        }
    }

    @Test
    public void shouldCheckRepoConnection() {
        CheckConnectionResultMessage checkConnectionResultMessage = poller.checkConnectionToRepository(repositoryConfiguration);
        assertTrue(checkConnectionResultMessage.success());
        assertEquals(1, checkConnectionResultMessage.getMessages().size());
        assertEquals(String.format("Successfully accessed repository metadata at %s", repoUrl + "/repodata/repomd.xml"), checkConnectionResultMessage.getMessages().get(0));
    }

    @Test
    public void shouldReturnErrorsWhenConnectionToRepoFails() {
        repositoryConfiguration = new PackageMaterialProperties();
        repositoryConfiguration.addPackageMaterialProperty(Constants.REPO_URL, new PackageMaterialProperty().withValue("file://invalid_path"));

        CheckConnectionResultMessage result = poller.checkConnectionToRepository(repositoryConfiguration);
        assertFalse(result.success());
        assertEquals("Could not access file - file://invalid_path/repodata/repomd.xml. Invalid file path.", result.getMessages().get(0));
    }

    @Test
    public void shouldPerformRepoValidationsBeforeCheckConnection() {
        repositoryConfiguration = new PackageMaterialProperties();
        repositoryConfiguration.addPackageMaterialProperty(Constants.REPO_URL, new PackageMaterialProperty().withValue("ftp://username:password@invalid_path"));

        CheckConnectionResultMessage result = poller.checkConnectionToRepository(repositoryConfiguration);
        assertFalse(result.success());
        assertEquals(2, result.getMessages().size());
        assertEquals("Invalid URL: Only 'file', 'http' and 'https' protocols are supported.", result.getMessages().get(0));
        assertEquals("User info should not be provided as part of the URL. Please provide credentials using USERNAME and PASSWORD configuration keys.", result.getMessages().get(1));
    }

    @Test
    public void shouldCheckConnectionToPackageAndRespondWithLatestPackageFound() {
        CheckConnectionResultMessage result = poller.checkConnectionToPackage(packageConfiguration, repositoryConfiguration);
        assertTrue(result.success());
        assertEquals("Found package 'go-agent-13.1.1-16714.noarch'.", result.getMessages().get(0));
    }

    @Test
    public void shouldFailConnectionToPackageRepositoryIfPackageIsNotFound() {
        packageConfiguration = new PackageMaterialProperties();
        packageConfiguration.addPackageMaterialProperty(Constants.PACKAGE_SPEC, new PackageMaterialProperty().withValue("go-a"));
        CheckConnectionResultMessage result = poller.checkConnectionToPackage(packageConfiguration, repositoryConfiguration);
        assertFalse(result.success());
        assertEquals("Could not find any package that matched 'go-a'.", result.getMessages().get(0));
    }

    @Test
    public void shouldFailConnectionToPackageRepositoryIfMultiplePackageIsFound() {
        packageConfiguration = new PackageMaterialProperties();
        packageConfiguration.addPackageMaterialProperty(Constants.PACKAGE_SPEC, new PackageMaterialProperty().withValue("go*"));
        CheckConnectionResultMessage result = poller.checkConnectionToPackage(packageConfiguration, repositoryConfiguration);
        assertFalse(result.success());
        assertTrue(result.getMessages().get(0).startsWith("Given Package Spec (go*) resolves to more than one file on the repository: "));
        assertTrue(result.getMessages().get(0).contains("go-agent-13.1.1-16714.noarch.rpm"));
        assertTrue(result.getMessages().get(0).contains("go-server-13.1.1-16714.noarch.rpm"));
    }

    @Test
    public void shouldFailConnectionToPackageRepositoryIfRepositoryIsNotReachable() {
        repositoryConfiguration = new PackageMaterialProperties();
        repositoryConfiguration.addPackageMaterialProperty(Constants.REPO_URL, new PackageMaterialProperty().withValue("file://invalid_random_2q342340"));
        CheckConnectionResultMessage result = poller.checkConnectionToPackage(packageConfiguration, repositoryConfiguration);
        assertFalse(result.success());
        assertEquals("Could not access file - file://invalid_random_2q342340/repodata/repomd.xml. Invalid file path.", result.getMessages().get(0));
    }

    @Test
    public void shouldValidatePackageDataWhileTestingConnection() {
        CheckConnectionResultMessage result = poller.checkConnectionToPackage(new PackageMaterialProperties(), repositoryConfiguration);
        assertFalse(result.success());
        assertEquals("Package spec not specified", result.getMessages().get(0));
    }

    @AfterEach
    public void tearDown() throws Exception {
        RepoqueryCacheCleaner.performCleanup();
    }

    private void assertPackageLocationData(String expectedLocation, PackageRevisionMessage pkg) {
        // RHEL 7 gives the full path while RHEL 8 gives the relative path. This assertion
        // should cover both cases.
        assertTrue(("file://" + sampleRepoDirectory.getAbsolutePath() + expectedLocation).endsWith(pkg.getDataFor("LOCATION")));
    }

    private void assertPackageRevisionMessageEquivalent(PackageRevisionMessage expected, PackageRevisionMessage actual) {
        if (!"(none)".equals(actual.getUser())) {
            assertEquals(expected.getUser(), actual.getUser());
        }

        assertEquals(expected.getRevision(), actual.getRevision());
        assertTrue(isWithinAMinute(expected.getTimestamp(), actual.getTimestamp()));
    }

    /**
     * Unlike RHEL 7, RHEL 8 (i.e., dnf repoquery) BUILDTIME field outputs the time to only minute precision as opposed
     * to second precision. Thus, in order to support both yum repoquery and dnf repoquery, ignore the seconds when
     * comparing timestamps.
     *
     * @param left  a {@link Date}
     * @param right a {@link Date}
     * @return true if the difference is a minute or less, false otherwise
     */
    private boolean isWithinAMinute(Date left, Date right) {
        return Math.abs(left.getTime() - right.getTime()) <= 60000L;
    }

    private long fromEpochTime(long timeInSeconds) {
        return timeInSeconds * 1000;
    }

}
