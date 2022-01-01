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

import com.tw.go.plugin.material.artifactrepository.yum.exec.message.PackageMaterialProperties;
import com.tw.go.plugin.material.artifactrepository.yum.exec.message.PackageMaterialProperty;
import com.tw.go.plugin.material.artifactrepository.yum.exec.message.ValidationError;
import com.tw.go.plugin.material.artifactrepository.yum.exec.message.ValidationResultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class PackageRepositoryConfigurationProviderTest {

    private PackageRepositoryConfigurationProvider configurationProvider;

    @BeforeEach
    public void setUp() {
        configurationProvider = new PackageRepositoryConfigurationProvider();
    }

    @Test
    public void shouldGetRepositoryConfiguration() {

        PackageMaterialProperties configuration = configurationProvider.repositoryConfiguration();

        assertNotNull(configuration.getProperty(Constants.REPO_URL));
        assertNull(configuration.getProperty(Constants.REPO_URL).partOfIdentity());
        assertNull(configuration.getProperty(Constants.REPO_URL).required());
        assertNull(configuration.getProperty(Constants.REPO_URL).secure());
        assertEquals("Repository URL", configuration.getProperty(Constants.REPO_URL).displayName());
        assertEquals("0", configuration.getProperty(Constants.REPO_URL).displayOrder());

        assertNotNull(configuration.getProperty(Constants.USERNAME));
        assertFalse(configuration.getProperty(Constants.USERNAME).partOfIdentity());
        assertFalse(configuration.getProperty(Constants.USERNAME).required());
        assertNull(configuration.getProperty(Constants.USERNAME).secure());
        assertEquals("User", configuration.getProperty(Constants.USERNAME).displayName());
        assertEquals("1", configuration.getProperty(Constants.USERNAME).displayOrder());

        assertNotNull(configuration.getProperty(Constants.PASSWORD));
        assertFalse(configuration.getProperty(Constants.PASSWORD).partOfIdentity());
        assertFalse(configuration.getProperty(Constants.PASSWORD).required());
        assertTrue(configuration.getProperty(Constants.PASSWORD).secure());
        assertEquals("Password", configuration.getProperty(Constants.PASSWORD).displayName());
        assertEquals("2", configuration.getProperty(Constants.PASSWORD).displayOrder());
    }

    @Test
    public void shouldGetPackageConfiguration() {
        PackageMaterialProperties configuration = configurationProvider.packageConfiguration();
        assertNotNull(configuration.getProperty(Constants.PACKAGE_SPEC));
        assertEquals("Package Spec", configuration.getProperty(Constants.PACKAGE_SPEC).displayName());
        assertEquals("0", configuration.getProperty(Constants.PACKAGE_SPEC).displayOrder());
    }

    @Test
    public void shouldCheckIfRepositoryConfigurationValid() {
        assertConfigurationErrors(configurationProvider.validateRepositoryConfiguration(new PackageMaterialProperties()), singletonList(new ValidationError(Constants.REPO_URL, "Repository url not specified")), false);
        assertConfigurationErrors(configurationProvider.validateRepositoryConfiguration(configurations(Constants.REPO_URL, null)), singletonList(new ValidationError(Constants.REPO_URL, "Repository url is empty")), false);
        assertConfigurationErrors(configurationProvider.validateRepositoryConfiguration(configurations(Constants.REPO_URL, "")), singletonList(new ValidationError(Constants.REPO_URL, "Repository url is empty")), false);
        assertConfigurationErrors(configurationProvider.validateRepositoryConfiguration(configurations(Constants.REPO_URL, "incorrectUrl")), singletonList(new ValidationError(Constants.REPO_URL, "Invalid URL : incorrectUrl")), false);
        assertConfigurationErrors(configurationProvider.validateRepositoryConfiguration(configurations(Constants.REPO_URL, "http://correct.com/url")), emptyList(), true);
        assertConfigurationErrors(configurationProvider.validateRepositoryConfiguration(configurations(Constants.REPO_URL, "http://correct.com/url")), emptyList(), true);
    }

    @Test
    public void shouldCheckForInvalidKeyInRepositoryConfiguration() {
        PackageMaterialProperties configurationProvidedByUser = new PackageMaterialProperties();
        configurationProvidedByUser.addPackageMaterialProperty(Constants.REPO_URL, new PackageMaterialProperty().withValue("http://correct.com/url"));
        configurationProvidedByUser.addPackageMaterialProperty("invalid-keys", new PackageMaterialProperty().withValue("some value"));
        ValidationResultMessage validationResultMessage = configurationProvider.validateRepositoryConfiguration(configurationProvidedByUser);
        assertConfigurationErrors(validationResultMessage, singletonList(new ValidationError("", "Unsupported key(s) found : invalid-keys. Allowed key(s) are : REPO_URL, USERNAME, PASSWORD")), false);
    }


    @Test
    public void shouldCheckIfPackageConfigurationValid() {
        assertConfigurationErrors(configurationProvider.validatePackageConfiguration(new PackageMaterialProperties()), singletonList(new ValidationError(Constants.PACKAGE_SPEC, "Package spec not specified")), false);
        assertConfigurationErrors(configurationProvider.validatePackageConfiguration(configurations(Constants.PACKAGE_SPEC, null)), singletonList(new ValidationError(Constants.PACKAGE_SPEC, "Package spec is null")), false);
        assertConfigurationErrors(configurationProvider.validatePackageConfiguration(configurations(Constants.PACKAGE_SPEC, "")), singletonList(new ValidationError(Constants.PACKAGE_SPEC, "Package spec is empty")), false);
        assertConfigurationErrors(configurationProvider.validatePackageConfiguration(configurations(Constants.PACKAGE_SPEC, "go-age?nt-*")), emptyList(), true);
        assertConfigurationErrors(configurationProvider.validatePackageConfiguration(configurations(Constants.PACKAGE_SPEC, "go-agent")), emptyList(), true);
    }

    @Test
    public void shouldCheckForInvalidKeyInPackageConfiguration() {
        PackageMaterialProperties configurationProvidedByUser = new PackageMaterialProperties();
        configurationProvidedByUser.addPackageMaterialProperty(Constants.PACKAGE_SPEC, new PackageMaterialProperty().withValue("go-agent"));
        configurationProvidedByUser.addPackageMaterialProperty("invalid-keys", new PackageMaterialProperty().withValue("some value"));
        ValidationResultMessage validationResultMessage = configurationProvider.validatePackageConfiguration(configurationProvidedByUser);
        assertConfigurationErrors(validationResultMessage, singletonList(new ValidationError("", "Unsupported key(s) found : invalid-keys. Allowed key(s) are : PACKAGE_SPEC")), false);
    }

    private void assertConfigurationErrors(ValidationResultMessage validationResult, List<ValidationError> expectedErrors, boolean expectedValidationResult) {
        assertEquals(expectedValidationResult, validationResult.success());
        assertEquals(expectedErrors.size(), validationResult.getValidationErrors().size());
        assertTrue(validationResult.getValidationErrors().containsAll(expectedErrors));
    }

    private PackageMaterialProperties configurations(String key, String value) {
        PackageMaterialProperties configurations = new PackageMaterialProperties();
        configurations.addPackageMaterialProperty(key, new PackageMaterialProperty().withValue(value));
        return configurations;
    }
}
