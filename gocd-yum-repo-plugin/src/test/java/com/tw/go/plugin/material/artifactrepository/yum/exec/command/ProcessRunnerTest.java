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

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessRunnerTest {
    @Test
    public void shouldRunACommand() {
        ProcessOutput output = new ProcessRunner().execute(new String[]{"echo", "foo"}, Collections.emptyMap());
        assertEquals("foo", output.getStdOut().get(0));
        assertEquals(0, output.getReturnCode());
    }

    @Test
    public void shouldThrowExceptionIfCommandThrowsAnException() {
        try {
            new ProcessRunner().execute(new String[]{"doesNotExist"}, Collections.emptyMap());
            fail("Should have thrown exception");
        } catch (Exception e) {
            assertTrue(e instanceof RuntimeException);
            if (isWindows()) {
                assertTrue(e.getMessage().contains("'doesNotExist' is not recognized as an internal or external command"));
            } else {
                assertTrue(e.getMessage().contains("Cannot run program \"doesNotExist\""));
            }
        }
    }

    @Test
    public void shouldReturnErrorOutputIfCommandFails() {
        ProcessOutput output;
        if (isWindows()) {
            output = new ProcessRunner().execute(new String[]{"dir", "foo:"}, null);
            assertTrue(output.getStdErrorAsString().contains("File Not Found"));
        } else {
            output = new ProcessRunner().execute(new String[]{"ls", "/foo"}, Collections.emptyMap());
            assertTrue(output.getStdErrorAsString().matches("Error Message: ls: cannot access [']?/foo[']?: No such file or directory"));
        }
        assertNotEquals(0, output.getReturnCode());
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.contains("Windows");
    }
}
