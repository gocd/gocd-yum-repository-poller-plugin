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

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.tw.go.plugin.material.artifactrepository.yum.exec.message.CheckConnectionResultMessage;
import com.tw.go.plugin.material.artifactrepository.yum.exec.message.PackageRevisionMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import static com.tw.go.plugin.common.util.JsonUtil.fromJsonString;
import static com.tw.go.plugin.material.artifactrepository.yum.exec.YumArtifactRepositoryMaterial.*;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YumArtifactRepositoryMaterialTest {
    private YumArtifactRepositoryMaterial material;
    private String repoUrl;

    @BeforeEach
    public void setUp() throws IOException {
        material = new YumArtifactRepositoryMaterial();
        RepoqueryCacheCleaner.performCleanup();
        final File sampleRepoDirectory = new File("src/test/repos/samplerepo");
        repoUrl = "file://" + sampleRepoDirectory.getAbsolutePath();
    }

    @Test
    public void shouldReturnResponseForRepositoryConfiguration() {
        GoPluginApiResponse response = material.handle(new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_REPOSITORY_CONFIGURATION));
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());
        assertEquals("{\"REPO_URL\":{\"display-name\":\"Repository URL\",\"display-order\":\"0\"}," +
                "\"USERNAME\":{\"part-of-identity\":false,\"required\":false,\"display-name\":\"User\",\"display-order\":\"1\"}," +
                "\"PASSWORD\":{\"secure\":true,\"part-of-identity\":false,\"required\":false,\"display-name\":\"Password\",\"display-order\":\"2\"}" +
                "}", response.responseBody());
    }

    @Test
    public void shouldReturnResponseForPackageConfiguration() {
        GoPluginApiResponse response = material.handle(new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_PACKAGE_CONFIGURATION));
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());
        assertEquals("{\"PACKAGE_SPEC\":{\"display-name\":\"Package Spec\",\"display-order\":\"0\"}}", response.responseBody());
    }

    @Test
    public void shouldReturnSuccessForValidateRepositoryConfiguration() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_VALIDATE_REPOSITORY_CONFIGURATION);
        request.setRequestBody("{\"repository-configuration\":{\"REPO_URL\":{\"value\":\"http://localhost.com\"}}}");
        GoPluginApiResponse response = material.handle(request);
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());
        assertEquals("", response.responseBody());
    }

    @Test
    public void shouldReturnFailureForValidateRepositoryConfiguration() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_VALIDATE_REPOSITORY_CONFIGURATION);
        request.setRequestBody("{\"repository-configuration\":{\"RANDOM\":{\"value\":\"value\"}}}");
        GoPluginApiResponse response = material.handle(request);
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());
        assertEquals("[{\"key\":\"\",\"message\":\"Unsupported key(s) found : RANDOM. Allowed key(s) are : REPO_URL, USERNAME, PASSWORD\"},{\"key\":\"REPO_URL\",\"message\":\"Repository url not specified\"}]", response.responseBody());
    }

    @Test
    public void shouldReturnSuccessForValidatePackageConfiguration() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_VALIDATE_PACKAGE_CONFIGURATION);
        request.setRequestBody("{\"repository-configuration\":{\"REPO_URL\":{\"value\":\"http://localhost.com\"}},\"package-configuration\":{\"PACKAGE_SPEC\":{\"value\":\"go-agent\"}}}");
        GoPluginApiResponse response = material.handle(request);
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());
        assertEquals("", response.responseBody());
    }

    @Test
    public void shouldReturnFailureForValidatePackageConfiguration() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_VALIDATE_PACKAGE_CONFIGURATION);
        request.setRequestBody("{\"repository-configuration\":{\"REPO_URL\":{\"value\":\"http://localhost.com\"}},\"package-configuration\":{\"RANDOM\":{\"value\":\"go-agent\"}}}");
        GoPluginApiResponse response = material.handle(request);
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());
        assertEquals("[{\"key\":\"\",\"message\":\"Unsupported key(s) found : RANDOM. Allowed key(s) are : PACKAGE_SPEC\"},{\"key\":\"PACKAGE_SPEC\",\"message\":\"Package spec not specified\"}]", response.responseBody());
    }


    @Test
    public void shouldReturnSuccessForCheckRepositoryConnection() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_CHECK_REPOSITORY_CONNECTION);
        request.setRequestBody(String.format("{\"repository-configuration\":{\"REPO_URL\":{\"value\":\"%s\"}}}", repoUrl));
        GoPluginApiResponse response = material.handle(request);
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());
        CheckConnectionResultMessage result = fromJsonString(response.responseBody(), CheckConnectionResultMessage.class);
        assertTrue(result.success());
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    public void shouldReturnFailureForCheckRepositoryConnection() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_CHECK_REPOSITORY_CONNECTION);
        request.setRequestBody(String.format("{\"repository-configuration\":{\"REPO_URL\":{\"value\":\"%s\"}}}", repoUrl + "/random"));
        GoPluginApiResponse response = material.handle(request);
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());
        CheckConnectionResultMessage result = fromJsonString(response.responseBody(), CheckConnectionResultMessage.class);
        assertFalse(result.success());
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    public void shouldReturnSuccessForCheckPackageConnection() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_CHECK_PACKAGE_CONNECTION);
        request.setRequestBody(String.format("{\"repository-configuration\":{\"REPO_URL\":{\"value\":\"%s\"}},\"package-configuration\":{\"PACKAGE_SPEC\":{\"value\":\"go-agent\"}}}", repoUrl));
        GoPluginApiResponse response = material.handle(request);
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());
        CheckConnectionResultMessage result = fromJsonString(response.responseBody(), CheckConnectionResultMessage.class);
        assertTrue(result.success());
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    public void shouldReturnFailureForCheckPackageConnection() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_CHECK_PACKAGE_CONNECTION);
        request.setRequestBody(String.format("{\"repository-configuration\":{\"REPO_URL\":{\"value\":\"%s\"}},\"package-configuration\":{\"PACKAGE_SPEC\":{\"value\":\"incorrect\"}}}", repoUrl));
        GoPluginApiResponse response = material.handle(request);
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());
        CheckConnectionResultMessage result = fromJsonString(response.responseBody(), CheckConnectionResultMessage.class);
        assertFalse(result.success());
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    public void shouldReturnLatestPackageRevision() {
        TimeZone.setDefault(TimeZone.getTimeZone("IST"));
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_LATEST_PACKAGE_REVISION);
        request.setRequestBody(String.format("{\"repository-configuration\":{\"REPO_URL\":{\"value\":\"%s\"}},\"package-configuration\":{\"PACKAGE_SPEC\":{\"value\":\"go-agent\"}}}", repoUrl));
        GoPluginApiResponse response = material.handle(request);
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());

        final PackageRevisionMessage packageRevision = fromJsonString(response.responseBody(), PackageRevisionMessage.class);

        assertFalse(packageRevision.getData().isEmpty());
        assertRevisionAndTime("go-agent-13.1.1-16714.noarch", "2013-04-04T11:14:18.000Z", packageRevision);
    }

    @Test
    public void shouldReturnLatestPackageRevisionSince() {
        TimeZone.setDefault(TimeZone.getTimeZone("IST"));
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_LATEST_PACKAGE_REVISION_SINCE);
        request.setRequestBody(String.format("{\"repository-configuration\":{\"REPO_URL\":{\"value\":\"%s\"}}," +
                "\"package-configuration\":{\"PACKAGE_SPEC\":{\"value\":\"go-agent\"}}," +
                "\"previous-revision\":{\"revision\":\"go-agent-13.1.0-16714.noarch\",\"timestamp\":\"2013-04-03T11:14:18.000Z\",\"data\":{\"data-key-one\":\"data-value-one\",\"data-key-two\":\"data-value-two\"}}}", repoUrl));
        GoPluginApiResponse response = material.handle(request);
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());

        final PackageRevisionMessage packageRevision = fromJsonString(response.responseBody(), PackageRevisionMessage.class);

        assertFalse(packageRevision.getData().isEmpty());
        assertRevisionAndTime("go-agent-13.1.1-16714.noarch", "2013-04-04T11:14:18.000Z", packageRevision);
    }

    @Test
    public void shouldReturnNullLatestPackageRevisionSince() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(EXTENSION, "1.0", REQUEST_LATEST_PACKAGE_REVISION_SINCE);
        request.setRequestBody(String.format("{\"repository-configuration\":{\"REPO_URL\":{\"value\":\"%s\"}}," +
                "\"package-configuration\":{\"PACKAGE_SPEC\":{\"value\":\"go-agent\"}}," +
                "\"previous-revision\":{\"revision\":\"go-agent-13.1.1-16714.noarch\",\"timestamp\":\"2013-04-04T11:14:18.000Z\",\"data\":{\"data-key-one\":\"data-value-one\",\"data-key-two\":\"data-value-two\"}}}", repoUrl));
        GoPluginApiResponse response = material.handle(request);
        assertNull(response.responseBody());
        assertEquals(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response.responseCode());
    }

    @Test
    public void shouldReturnUnSuccessfulResponseWhenHandlerNotFondForRequest() {
        assertEquals(400, material.handle(new DefaultGoPluginApiRequest(EXTENSION, "1.0", "invalid")).responseCode());
        assertEquals(400, material.handle(new DefaultGoPluginApiRequest(EXTENSION, "1.0", null)).responseCode());
        assertEquals(400, material.handle(new DefaultGoPluginApiRequest(EXTENSION, "1.0", "")).responseCode());
    }

    @Test
    public void shouldReturnUnSuccessfulResponseOnException() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("package-repository", "1.0", "repository-configuration");
        final MessageHandler messageHandler = mock(MessageHandler.class);
        material = new YumArtifactRepositoryMaterial() {
            @Override
            MessageHandler repositoryConfigurationsMessageHandler() {
                return messageHandler;
            }
        };
        when(messageHandler.handle(request)).thenThrow(new RuntimeException("failed-for-some-reason"));
        GoPluginApiResponse response = material.handle(request);
        assertEquals(500, response.responseCode());
        assertEquals("failed-for-some-reason", response.responseBody());
    }

    @Test
    public void shouldReturnPluginIdentifierForPackageRepository() {
        GoPluginIdentifier pluginIdentifier = material.pluginIdentifier();
        assertEquals(EXTENSION, pluginIdentifier.getExtension());
        assertEquals(Collections.singletonList("1.0"), pluginIdentifier.getSupportedExtensionVersions());
    }

    @AfterEach
    public void tearDown() throws IOException {
        RepoqueryCacheCleaner.performCleanup();
    }

    @SuppressWarnings("SameParameterValue")
    private void assertRevisionAndTime(String revision, String timestamp, PackageRevisionMessage packageRevision) {
        assertEquals(revision, packageRevision.getRevision());
        assertTrue(isWithinAMinute(parseTimestamp(timestamp), packageRevision.getTimestamp()));
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

    @NotNull
    private Date parseTimestamp(String timestamp) {
        return Date.from(LocalDateTime.from(ISO_LOCAL_DATE_TIME.parse(timestamp.replaceAll("Z$", ""))).atZone(ZoneOffset.systemDefault()).toInstant());
    }
}
