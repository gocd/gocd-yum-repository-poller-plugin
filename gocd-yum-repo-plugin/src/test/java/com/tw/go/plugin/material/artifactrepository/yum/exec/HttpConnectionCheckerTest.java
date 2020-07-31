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

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HttpConnectionCheckerTest {

    private HttpConnectionChecker checker;
    private MockWebServer webServer;

    @BeforeEach
    public void setUp() {
        webServer = new MockWebServer();
        checker = new HttpConnectionChecker();
    }

    @AfterEach
    public void tearDown() throws Exception {
        webServer.shutdown();
    }

    @Test
    public void shouldNotThrowExceptionIfCheckConnectionToTheRepoPasses() throws Exception {
        webServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));
        HttpUrl url = webServer.url("/repodata/repomd.xml");

        checker.checkConnection(url.toString(), new Credentials(null, null));

        RecordedRequest recordedRequest = webServer.takeRequest();
        assertEquals("/repodata/repomd.xml", recordedRequest.getPath());
        assertEquals(1, webServer.getRequestCount());
    }

    @Test
    public void shouldPerformBasicAuthUsingChallengeResponseAuth() throws Exception {
        webServer.enqueue(new MockResponse().setResponseCode(401).setHeader("WWW-Authenticate", "Basic realm=\"YumRepo\""));
        webServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        HttpUrl url = webServer.url("/repodata/repomd.xml");

        checker.checkConnection(url.toString(), new Credentials("foo", "bar"));
        assertEquals(2, webServer.getRequestCount());

        RecordedRequest recordedRequest = webServer.takeRequest();
        assertEquals("/repodata/repomd.xml", recordedRequest.getPath());
        assertNull(recordedRequest.getHeaders().get("Authorization"));

        recordedRequest = webServer.takeRequest();
        assertEquals("/repodata/repomd.xml", recordedRequest.getPath());
        assertEquals("Basic Zm9vOmJhcg==", recordedRequest.getHeaders().get("Authorization"));
    }


    @Test
    public void shouldFailCheckConnectionToTheRepoWhenHttpClientReturnsAUnSuccessfulReturnCode() {
        webServer.enqueue(new MockResponse().setResponseCode(500));
        HttpUrl url = webServer.url("/repodata/repomd.xml");

        try {
            checker.checkConnection(url.toString(), new Credentials(null, null));
            fail("should fail");
        } catch (Exception e) {
            assertEquals("HTTP/1.1 500 Server Error", e.getMessage());
        }
        assertEquals(1, webServer.getRequestCount());
    }

    @Test
    public void shouldFailCheckConnectionToTheRepoWhenHttpClientThrowsIOException() {
        try {
            checker.checkConnection("https://localhost:11111", new Credentials(null, null));
            fail("should fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Connection refused"));
        }
    }
}
