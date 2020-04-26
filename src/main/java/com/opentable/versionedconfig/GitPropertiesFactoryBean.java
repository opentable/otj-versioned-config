/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentable.versionedconfig;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.ConfigurableEnvironment;

import com.opentable.spring.PropertySourceUtil;
import com.opentable.spring.SpecializedConfigFactory;

/**
  Core factory bean
 */
public class GitPropertiesFactoryBean implements FactoryBean<GitProperties> {

    @Inject
    private Optional<List<CredentialVersionedURICustomizer>> credentialVersionedURICustomizers = Optional.empty();
    private final List<VersionedURICustomizer> versionedURICustomizers = new ArrayList<>();

    @Inject
    private ConfigurableEnvironment environment;

    private final String name;


    public GitPropertiesFactoryBean(String name) {
        Objects.requireNonNull(name);
        if (name.contains(".")) {
            throw new IllegalArgumentException("name cannot contain a period");
        }
        this.name = name.trim();
    }

    public GitPropertiesFactoryBean withCustomizer(VersionedURICustomizer versionedURICustomizer) {
        this.versionedURICustomizers.add(versionedURICustomizer);
        return this;
    }

    @Override
    public GitProperties getObject() throws Exception {

        String prefix = "ot.versioned-config." + name;
        final SpecializedConfigFactory<VersionedSpringProperties> specializedConfigFactory = SpecializedConfigFactory.create(environment,
                VersionedSpringProperties.class, "ot.versioned-config.${name}");
        final VersionedSpringProperties versionedSpringProperties = specializedConfigFactory.getConfig(name);
        if (versionedSpringProperties.getRemote().isEmpty()) {
            throw new IllegalArgumentException("Must specify at least one uri");
        }
        if (StringUtils.isEmpty(versionedSpringProperties.getBranch())) {
            throw new IllegalArgumentException("Must specify branch");
        }
        if (StringUtils.isEmpty(versionedSpringProperties.getLocal())) {
            throw new IllegalArgumentException("Must specify local");
        }
        final Path localPath = Paths.get(versionedSpringProperties.getLocal());

        // Convert to mutable uris
        final List<MutableUri> mutableUriList = versionedSpringProperties.getRemote().stream().map(MutableUri::new).collect(Collectors.toList());
        // Standard customizers, none supplied by default, but it might be nice to support
        mutableUriList.forEach(uri -> this.versionedURICustomizers.forEach(t -> t.accept(uri)));

        // Credential Customizers
        credentialVersionedURICustomizers.ifPresent(credentialCustomizers -> {
            for (int i = 0; i < mutableUriList.size(); i++) {
                MutableUri mutableUri = mutableUriList.get(i);
                final Properties properties = PropertySourceUtil.getProperties(environment, prefix + ".secrets");
                final String secretPath = properties.getProperty(String.valueOf(i));
                credentialCustomizers.forEach(t -> t.accept(secretPath, mutableUri));
            }
        });

        List<URI> remoteRepos = mutableUriList.stream().map(MutableUri::toUri).collect(Collectors.toList());
        return new GitProperties(remoteRepos, localPath, versionedSpringProperties.getBranch());
    }

    @Override
    public Class<?> getObjectType() {
        return GitProperties.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public ConfigurableEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(final ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    public void setCredentialVersionedURICustomizers(final Optional<List<CredentialVersionedURICustomizer>> credentialVersionedURICustomizers) {
        this.credentialVersionedURICustomizers = credentialVersionedURICustomizers;
    }

    public Optional<List<CredentialVersionedURICustomizer>> getCredentialVersionedURICustomizers() {
        return credentialVersionedURICustomizers;
    }
}
