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

import com.tw.go.plugin.common.util.StringUtil;
import com.tw.go.plugin.material.artifactrepository.yum.exec.Constants;
import com.tw.go.plugin.material.artifactrepository.yum.exec.RepoUrl;
import com.tw.go.plugin.material.artifactrepository.yum.exec.RepoqueryCacheCleaner;
import com.tw.go.plugin.material.artifactrepository.yum.exec.message.PackageRevisionMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static com.tw.go.plugin.material.artifactrepository.yum.exec.command.RepoQueryCommand.DELIMITER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RepoQueryCommandTest {
    @BeforeEach
    public void setUp() throws Exception {
        RepoqueryCacheCleaner.performCleanup();
    }

    @Test
    public void shouldRunRepoqueryCommand() {
        ProcessRunner processRunner = mock(ProcessRunner.class);
        String repoid = "repoid";
        String repourl = "http://repourl";
        String spec = "pkg-spec";
        String repoFromPath = repoid + "," + repourl;
        String[] expectedCommand = repoQueryCommand(repoid, spec, repoFromPath);

        ArrayList<String> stdOut = new ArrayList<>();
        long time = 5;
        stdOut.add(repoQueryOutput(time, "packager", "http://location", "http://jenkins.job"));
        when(processRunner.execute(expectedCommand, envMapWithDefaultValues(repoid))).thenReturn(new ProcessOutput(0, stdOut, new ArrayList<>()));
        PackageRevisionMessage packageRevision = new RepoQueryCommand(processRunner, new RepoQueryParams(repoid, new RepoUrl(repourl, null, null), spec)).execute();

        assertEquals("name-version-release.arch", packageRevision.getRevision());
        assertEquals(new Date(5000), packageRevision.getTimestamp());
        assertEquals("packager", packageRevision.getUser());
        assertEquals("http://location", packageRevision.getData().get(Constants.PACKAGE_LOCATION));
        assertEquals("http://jenkins.job", packageRevision.getTrackbackUrl());
        verify(processRunner).execute(expectedCommand, envMapWithDefaultValues(repoid));
    }

    @Test
    public void shouldCreatePackageRevisionWithNullParametersWhenRepoQueryReturnsNone() {
        ProcessRunner processRunner = mock(ProcessRunner.class);
        String repoid = "repoid";
        String repourl = "http://repourl";
        String spec = "pkg-spec";
        String repoFromPath = repoid + "," + repourl;
        String[] expectedCommand = repoQueryCommand(repoid, spec, repoFromPath);

        ArrayList<String> stdOut = new ArrayList<>();
        long time = 5;
        stdOut.add(repoQueryOutput(time, "None", "NONE", "NOne"));

        when(processRunner.execute(expectedCommand, envMapWithDefaultValues(repoid))).thenReturn(new ProcessOutput(0, stdOut, new ArrayList<>()));
        PackageRevisionMessage packageRevision = new RepoQueryCommand(processRunner, new RepoQueryParams(repoid, new RepoUrl(repourl, null, null), spec)).execute();

        assertEquals("name-version-release.arch", packageRevision.getRevision());
        assertEquals(new Date(5000), packageRevision.getTimestamp());
        assertNull(packageRevision.getUser());
        assertNull(packageRevision.getData().get(Constants.PACKAGE_LOCATION));
        assertNull(packageRevision.getTrackbackUrl());
        assertNull(packageRevision.getRevisionComment());
        verify(processRunner).execute(expectedCommand, envMapWithDefaultValues(repoid));
    }

    @Test
    public void shouldThrowExceptionIfCommandFails() {
        ProcessRunner processRunner = mock(ProcessRunner.class);
        ArrayList<String> stdErr = new ArrayList<>();
        stdErr.add("err msg");
        when(processRunner.execute(any(), any())).thenReturn(new ProcessOutput(1, null, stdErr));
        try {
            new RepoQueryCommand(processRunner, new RepoQueryParams("repoid", new RepoUrl("http://url", null, null), "spec")).execute();
            fail("expected exception");
        } catch (Exception e) {
            assertEquals("Error while querying repository with path 'http://url' and package spec 'spec'. Error Message: err msg", e.getMessage());
        }
    }

    @Test
    public void shouldRunRepoQueryWithUserCredentialsIfProvided() {
        ProcessRunner processRunner = mock(ProcessRunner.class);
        String repoid = "repoid";
        String repourl = "http://repohost:1111/some/path";
        String spec = "pkg-spec";
        String repoFromPath = "repoid,http://username:%214321abcd@repohost:1111/some/path";
        String[] expectedCommand = repoQueryCommand(repoid, spec, repoFromPath);

        ArrayList<String> stdOut = new ArrayList<>();
        long time = 5;
        stdOut.add(repoQueryOutput(time, "packager", "http://location", "http://jenkins.job"));
        when(processRunner.execute(expectedCommand, envMapWithDefaultValues(repoid))).thenReturn(new ProcessOutput(0, stdOut, new ArrayList<>()));
        RepoQueryParams params = new RepoQueryParams(repoid, new RepoUrl(repourl, "username", "!4321abcd"), spec);
        PackageRevisionMessage packageRevision = new RepoQueryCommand(processRunner, params).execute();
        assertNotNull(packageRevision);
        verify(processRunner).execute(expectedCommand, envMapWithDefaultValues(repoid));
    }

    @Test
    public void shouldIncludeCredentialsWhenProvidedInTheDownloadLocation() {
        ProcessRunner processRunner = mock(ProcessRunner.class);
        String repoid = "repoid";
        String repourl = "http://repohost:1111/some/path";
        String spec = "pkg-spec";
        String repoFromPath = "repoid,http://username:%214321abcd@repohost:1111/some/path";
        String[] expectedCommand = repoQueryCommand(repoid, spec, repoFromPath);

        ArrayList<String> stdOut = new ArrayList<>();
        long time = 5;
        stdOut.add(repoQueryOutput(time, "packager", "http://foo.com/bar", "http://jenkins.job"));

        when(processRunner.execute(expectedCommand, envMapWithDefaultValues(repoid))).thenReturn(new ProcessOutput(0, stdOut, new ArrayList<>()));
        RepoQueryParams params = new RepoQueryParams(repoid, new RepoUrl(repourl, "username", "!4321abcd"), spec);
        PackageRevisionMessage packageRevision = new RepoQueryCommand(processRunner, params).execute();
        assertEquals("http://foo.com/bar", packageRevision.getData().get(Constants.PACKAGE_LOCATION));
        verify(processRunner).execute(expectedCommand, envMapWithDefaultValues(repoid));
    }

    @Test
    public void shouldFailCommandExecutionIfMoreThanOneResultIsReturned() {
        ProcessRunner processRunner = mock(ProcessRunner.class);
        String repoid = "repoid";
        String repourl = "http://repohost:1111/some/path";
        String spec = "go-agent";
        String repoFromPath = "repoid,http://username:%214321abcd@repohost:1111/some/path";
        String[] expectedCommand = repoQueryCommand(repoid, spec, repoFromPath);

        ArrayList<String> stdOut = new ArrayList<>();
        long time = 5;

        stdOut.add("getPackage/go-agent-13.1.0-13422.noarch.rpm" + DELIMITER + "go-agent" + DELIMITER + "13.1.0" + DELIMITER + "13422" + DELIMITER + "noarch" + DELIMITER + time + DELIMITER + "packager" + DELIMITER + "http://foo.com/bar"
                + DELIMITER + "trackback" + DELIMITER + "revision Comment");
        stdOut.add("getPackage/go-agent-13.1.0-13422.x86_64.rpm" + DELIMITER + "go-agent" + DELIMITER + "13.1.0" + DELIMITER + "13422" + DELIMITER + "x86_64" + DELIMITER + time + DELIMITER + "packager" +
                DELIMITER + "http://foo.com/bar" + DELIMITER + "trackback" + DELIMITER + "revision Comment");
        when(processRunner.execute(expectedCommand, envMapWithDefaultValues(repoid))).thenReturn(new ProcessOutput(0, stdOut, new ArrayList<>()));
        RepoQueryParams params = new RepoQueryParams(repoid, new RepoUrl(repourl, "username", "!4321abcd"), spec);
        try {
            new RepoQueryCommand(processRunner, params).execute();
            fail("expected failure");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Given Package Spec (go-agent) resolves to more than one file on the repository: go-agent-13.1.0-13422.noarch.rpm, go-agent-13.1.0-13422.x86_64.rpm"));
            verify(processRunner).execute(expectedCommand, envMapWithDefaultValues(repoid));
        }
    }

    @Test
    public void shouldHandleMultipleThreads() throws InterruptedException {
        final StringBuilder errors = new StringBuilder();
        Thread.UncaughtExceptionHandler handler = (t, e) -> errors.append(t.getName()).append(" : ").append(e.getMessage());
        String repoId = UUID.randomUUID().toString();
        ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Thread thread = new Thread(new CommandThread(repoId));
            thread.setUncaughtExceptionHandler(handler);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        if (!StringUtil.isBlank(errors.toString())) {
            fail(errors.toString());
        }
    }

    private String repoQueryOutput(long time, String packager, String location, String trackbackUrl) {
        return "relativepath" + DELIMITER + "name" + DELIMITER + "version" + DELIMITER + "release" + DELIMITER + "arch" + DELIMITER + time + DELIMITER + packager + DELIMITER + location + DELIMITER + trackbackUrl;
    }

    private String[] repoQueryCommand(String repoid, String spec, String repoFromPath) {
        return new String[]{"repoquery",
                "--repofrompath=" + repoFromPath,
                "--repoid=" + repoid,
                "-q",
                spec,
                "--qf",
                "%{RELATIVEPATH}" + DELIMITER + "%{NAME}" + DELIMITER + "%{VERSION}" + DELIMITER + "%{RELEASE}" + DELIMITER + "%{ARCH}" + DELIMITER + "%{BUILDTIME}" + DELIMITER + "%{PACKAGER}" + DELIMITER + "%{LOCATION}" + DELIMITER + "%{URL}"};
    }

    private Map<String, String> envMapWithDefaultValues(String repoid) {
        Map<String, String> expectedEnvMap = new HashMap<>();
        expectedEnvMap.put("HOME", System.getenv("HOME"));
        expectedEnvMap.put("TMPDIR", String.format("/var/tmp/go-yum-plugin-%s", repoid));
        return expectedEnvMap;
    }

    static class CommandThread implements Runnable {
        private final String repoId;
        private final String repoUrl;

        CommandThread(String repoId) {
            this.repoId = repoId;
            repoUrl = "file://" + new File("src/test/repos/samplerepo").getAbsolutePath();
        }

        public void run() {
            new RepoQueryCommand(new RepoQueryParams(repoId, new RepoUrl(repoUrl, null, null), "go-agent")).execute();
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        RepoqueryCacheCleaner.performCleanup();
    }
}
