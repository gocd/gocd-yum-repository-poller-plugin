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

import com.tw.go.plugin.material.artifactrepository.yum.exec.message.ValidationError;
import com.tw.go.plugin.material.artifactrepository.yum.exec.message.ValidationResultMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tw.go.plugin.material.artifactrepository.yum.exec.Constants.REPO_URL;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RepoUrlTest {
    @Test
    public void shouldCorrectlyCheckIfRepositoryConfigurationValid() {
        assertRepositoryUrlValidation("", null, null, singletonList(new ValidationError(REPO_URL, "Repository url is empty")), true);
        assertRepositoryUrlValidation(null, null, null, singletonList(new ValidationError(REPO_URL, "Repository url is empty")), true);
        assertRepositoryUrlValidation("  ", null, null, singletonList(new ValidationError(REPO_URL, "Repository url is empty")), true);
        assertRepositoryUrlValidation("h://localhost", null, null, singletonList(new ValidationError(REPO_URL, "Invalid URL : h://localhost")), true);
        assertRepositoryUrlValidation("ftp:///foo.bar", null, null, singletonList(new ValidationError(REPO_URL, "Invalid URL: Only 'file', 'http' and 'https' protocols are supported.")), true);
        assertRepositoryUrlValidation("incorrectUrl", null, null, singletonList(new ValidationError(REPO_URL, "Invalid URL : incorrectUrl")), true);
        assertRepositoryUrlValidation("http://user:password@localhost", null, null, singletonList(new ValidationError(REPO_URL, "User info should not be provided as part of the URL. Please provide credentials using USERNAME and PASSWORD configuration keys.")), true);
        assertRepositoryUrlValidation("http://correct.com/url", null, null, emptyList(), false);
        assertRepositoryUrlValidation("file:///foo.bar", null, null, emptyList(), false);
        assertRepositoryUrlValidation("file:///foo.bar", "user", "password", singletonList(new ValidationError(REPO_URL, "File protocol does not support username and/or password.")), true);
    }

    @Test
    public void shouldThrowUpWhenFileProtocolAndCredentialsAreUsed() {
        RepoUrl repoUrl = new RepoUrl("file://foo.bar", null, "password");
        ValidationResultMessage errors = new ValidationResultMessage();

        repoUrl.validate(errors);

        assertTrue(errors.failure());
        assertEquals(1, errors.getValidationErrors().size());
        assertEquals("File protocol does not support username and/or password.", errors.getValidationErrors().get(0).getMessage());
    }

    @Test
    public void shouldReturnURLWithBasicAuth() {
        RepoUrl repoUrl = new RepoUrl("http://localhost", "user", "password");
        assertEquals("http://user:password@localhost", repoUrl.getUrlWithBasicAuth());
    }

    @Test
    public void shouldReturnTheRightConnectionCheckerBasedOnUrlScheme() {
        ConnectionChecker checker = new RepoUrl("http://foobar.com", null, null).getChecker();
        assertTrue(checker instanceof HttpConnectionChecker);

        checker = new RepoUrl("https://foobar.com", null, null).getChecker();
        assertTrue(checker instanceof HttpConnectionChecker);

        checker = new RepoUrl("file://foo/bar", null, null).getChecker();
        assertTrue(checker instanceof FileBasedConnectionChecker);
    }

    @Test
    public void shouldThrowExceptionIfURIIsInvalid_checkConnection() {
        try {
            new RepoUrl("://foobar.com", null, null).checkConnection();
            fail("should have failed");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid URL: java.net.MalformedURLException: no protocol: ://foobar.com"));
        }
    }

    @Test
    public void shouldThrowExceptionIfSchemeIsInvalid_checkConnection() {
        try {
            new RepoUrl("httph://foobar.com", null, null).checkConnection();
            fail("should have failed");
        } catch (Exception e) {
            assertEquals("Invalid URL: java.net.MalformedURLException: unknown protocol: httph", e.getMessage());
        }
    }

    @Test
    public void shouldFailCheckConnectionToTheRepoWhenHttpUrlIsNotReachable() {
        try {
            RepoUrl repoUrl = spy(new RepoUrl("url", null, null));
            ConnectionChecker connectionChecker = mock(ConnectionChecker.class);
            doThrow(new RuntimeException("Unreachable url")).when(connectionChecker).checkConnection(any(), any());
            doReturn(connectionChecker).when(repoUrl).getChecker();
            repoUrl.checkConnection();
            fail("should fail");
        } catch (Exception e) {
            assertEquals("Unreachable url", e.getMessage());
        }
    }

    @Test
    public void shouldFailCheckConnectionToTheRepoWhenRepoFileSystemPathIsNotReachable() {
        try {
            new RepoUrl("file:///foo/bar", null, null).checkConnection();
            fail("should fail");
        } catch (Exception e) {
            assertEquals("Invalid file path.", e.getMessage());
        }
    }

    @Test
    public void shouldNotThrowExceptionIfCheckConnectionToTheRepoPasses() {
        RepoUrl repoUrl = spy(new RepoUrl("url", null, null));
        ConnectionChecker connectionChecker = mock(ConnectionChecker.class);
        doReturn(connectionChecker).when(repoUrl).getChecker();

        repoUrl.checkConnection();

        verify(connectionChecker).checkConnection(any(), any());
    }

    @Test
    public void shouldGetUrlForDisplay() {
        assertEquals("file:///foo/bar", new RepoUrl("file:///foo/bar", null, null).forDisplay());
    }

    @Test
    public void shouldGetRepoMetadataUrl() {
        assertEquals("file:///foo/bar/repodata/repomd.xml", new RepoUrl("file:///foo/bar", null, null).getRepoMetadataUrl());
        assertEquals("file:///foo/bar/repodata/repomd.xml", new RepoUrl("file:///foo/bar/", null, null).getRepoMetadataUrl());
        assertEquals("file:///foo/bar/repodata/repomd.xml", new RepoUrl("file:///foo/bar//", null, null).getRepoMetadataUrl());
    }

    private void assertRepositoryUrlValidation(String url, String username, String password, List<ValidationError> expectedErrors, boolean isFailure) {
        ValidationResultMessage errors = new ValidationResultMessage();
        new RepoUrl(url, username, password).validate(errors);
        assertEquals(isFailure, errors.failure());
        assertEquals(expectedErrors.size(), errors.getValidationErrors().size());
        assertTrue(errors.getValidationErrors().containsAll(expectedErrors));
    }
}
