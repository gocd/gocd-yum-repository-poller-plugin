/*
 * Copyright 2022 Thoughtworks, Inc.
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

package com.tw.go.plugin.material.artifactrepository.yum.exec.command;

import com.tw.go.plugin.material.artifactrepository.yum.exec.RepoUrl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RepoQueryParamsTest {
    @Test
    public void shouldReturnUrlWithEscapedPassword() {
        String repoid = "repoid";
        String repourl = "http://repohost:1111/some/path#fragment?q=foo";
        String spec = "pkg-spec";
        String username = "username";
        String password = "!4321abcd";
        RepoQueryParams params = new RepoQueryParams(repoid, new RepoUrl(repourl, username, password), spec);

        assertEquals("repoid,http://username:%214321abcd@repohost:1111/some/path#fragment?q=foo", params.getRepoFromId());
    }

    @Test
    public void shouldThrowExceptionIfRepoUrlIsInvalid() {
        try {
            new RepoQueryParams("repoid", new RepoUrl("://some/path", "username", "!4321abcd"), "pkg-spec").getRepoFromId();
            fail("should throw exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("java.net.MalformedURLException"));
        }
    }

    @Test
    public void shouldReturnUrlAsIsIfNoCredentialsProvided() {
        String repoid = "repoid";
        String repourl = "http://repohost:1111/some/path#fragment?q=foo";
        String spec = "pkg-spec";
        RepoQueryParams params = new RepoQueryParams(repoid, new RepoUrl(repourl, null, null), spec);
        assertEquals("repoid,http://repohost:1111/some/path#fragment?q=foo", params.getRepoFromId());
    }

    @Test
    public void shouldThrowExceptionIfUrlDoesNotContainTwoForwardSlash() {
        RepoQueryParams params = new RepoQueryParams("id", new RepoUrl("file:/path", "user", "pwd"), "spec");
        try {
            params.getRepoFromId();
            fail("expected invalid uri exception");
        } catch (Exception e) {
            assertEquals("Invalid uri format file:/path", e.getMessage());
        }
    }

}
