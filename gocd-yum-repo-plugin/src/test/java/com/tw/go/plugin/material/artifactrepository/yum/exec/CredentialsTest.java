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

package com.tw.go.plugin.material.artifactrepository.yum.exec;

import com.tw.go.plugin.material.artifactrepository.yum.exec.message.ValidationError;
import com.tw.go.plugin.material.artifactrepository.yum.exec.message.ValidationResultMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CredentialsTest {
    @Test
    public void shouldGetUserInfo() throws Exception {
        Credentials credentials = new Credentials("user", "password");
        assertEquals("user:password", credentials.getUserInfo());
    }

    @Test
    public void shouldGetUserInfoWithEscapedPassword() throws Exception {
        Credentials credentials = new Credentials("user", "!password@:");
        assertEquals("user:%21password%40%3A", credentials.getUserInfo());
    }

    @Test
    public void shouldEncodeURLCorrectlyWhenUsernameIsEmailAddress() throws Exception {
        Credentials credentials = new Credentials("user@example.com", "!password@:");
        assertEquals("user%40example.com:%21password%40%3A", credentials.getUserInfo());
    }

    @Test
    public void shouldFailValidationIfOnlyPasswordProvided() {
        ValidationResultMessage validationResult = new ValidationResultMessage();
        new Credentials(null, "password").validate(validationResult);
        assertTrue(validationResult.failure());
        assertTrue(validationResult.getValidationErrors().contains(new ValidationError(Constants.USERNAME, "Both Username and password are required.")));

        validationResult = new ValidationResultMessage();
        new Credentials("user", "").validate(validationResult);
        assertTrue(validationResult.failure());
        assertTrue(validationResult.getValidationErrors().contains(new ValidationError(Constants.PASSWORD, "Both Username and password are required.")));
    }
}
