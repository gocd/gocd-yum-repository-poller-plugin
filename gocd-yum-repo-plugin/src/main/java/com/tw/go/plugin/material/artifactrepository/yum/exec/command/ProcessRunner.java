/*
 * Copyright 2022 ThoughtWorks, Inc.
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

import org.apache.commons.io.IOUtils;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ProcessRunner {
    public ProcessOutput execute(String[] command, Map<String, String> envMap) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = null;
        ProcessOutput processOutput;
        try {
            processBuilder.environment().putAll(envMap);
            process = processBuilder.start();
            int returnCode = process.waitFor();
            List<String> outputStream = IOUtils.readLines(process.getInputStream(), Charset.defaultCharset());
            List<String> errorStream = IOUtils.readLines(process.getErrorStream(), Charset.defaultCharset());
            processOutput = new ProcessOutput(returnCode, outputStream, errorStream);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } finally {
            if (process != null) {
                closeQuietly(process::getInputStream);
                closeQuietly(process::getErrorStream);
                closeQuietly(process::getOutputStream);
                process.destroy();
            }
        }
        return processOutput;
    }

    private void closeQuietly(Supplier<AutoCloseable> fn) {
        //noinspection EmptyTryBlock
        try (final AutoCloseable ignored = fn.get()) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
