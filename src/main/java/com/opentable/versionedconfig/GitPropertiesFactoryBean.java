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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.ConfigurableEnvironment;

import com.opentable.spring.PropertySourceUtil;
import com.opentable.spring.SpecializedConfigFactory;

/**
 * Core factory bean
 */
public class GitPropertiesFactoryBean implements FactoryBean<GitProperties> {
    public static final String PROPERTY_SECRET_PATH = "secret";

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    private Optional<List<VersionedURICustomizer>> credentialVersionedURICustomizers = Optional.empty();
    // Similar but optional to add withCustomizer()
    private final List<VersionedURICustomizer> versionedURICustomizers = new ArrayList<>();

    // Inject the environment. For testing, MockEnvironment may be used
    @Inject
    private ConfigurableEnvironment environment;

    // Name of this configuration
    private final String name;

    public GitPropertiesFactoryBean(final String name) {
        Objects.requireNonNull(name);
        if (name.contains(".")) {
            throw new IllegalArgumentException("name cannot contain a period");
        }
        this.name = name.trim();
    }

    public GitPropertiesFactoryBean withCustomizer(final VersionedURICustomizer versionedURICustomizer) {
        this.versionedURICustomizers.add(versionedURICustomizer);
        return this;
    }

    @Override
    public GitProperties getObject() {

        String prefix = "ot.versioned-config." + name;
        final SpecializedConfigFactory<VersionedSpringProperties> specializedConfigFactory = SpecializedConfigFactory.create(environment,
                VersionedSpringProperties.class, "ot.versioned-config.${name}");
        final VersionedSpringProperties versionedSpringProperties = specializedConfigFactory.getConfig(name);
        if (versionedSpringProperties.getRemotes().isEmpty()) {
            throw new IllegalArgumentException("Must specify at least one uri in remotes list");
        }
        if (StringUtils.isEmpty(versionedSpringProperties.getBranch())) {
            throw new IllegalArgumentException("Must specify branch");
        }
        if (StringUtils.isEmpty(versionedSpringProperties.getLocal())) {
            throw new IllegalArgumentException("Must specify local attribute");
        }
        final Path localPath = Paths.get(versionedSpringProperties.getLocal());

        final List<String> allRemoteConnections = versionedSpringProperties.getRemotes().stream()
                .map(String::trim).collect(Collectors.toList());

        /*
         * ot.versioned-config.$(name).remotes=(comma separated list of remotes, each named by a unique string
         * example = foo, bar
         * Then
         * ot.versioned.config.$(name).remote.foo.uri=the uri, which may contain auth info, but probably shouldn't
         * ot.versioned.config.$(name).remote.foo.secret= name of CM shared secret
         */
        // Everything under (name)
        final Properties properties = PropertySourceUtil.getProperties(environment, prefix);

        // Take the ${name}.remote.${remoteName}.uri attribute, convert to uris
        // Key = original name attribute, value = uri
        final Map<String, URI> remoteUris = allRemoteConnections.stream()
                .filter(Objects::nonNull).collect(Collectors.toMap(Function.identity(), v -> {
                    // Check for URI property and throw exception if not there or invalid uri format.
                    String uri = properties.getProperty("remote." + v + ".uri");
                    if (uri == null) {
                        throw new IllegalArgumentException("The remote connector " + v + " doesn't have an uri attribute");
                    }
                    return URI.create(uri);
                }));

        final List<MutableUri> mutableUriList = new ArrayList<>();
        // Scan through the Map
        remoteUris.forEach((connectorName, uri) -> {
            // Create a mutable version of the uri
            final MutableUri mutableUri = new MutableUri(uri);
            // Check if there's a secret path, and add to the map instance if there.
            final String secretPath = properties.getProperty("remote." + connectorName + ".secret");
            final Map<String, Object> map = new HashMap<>();
            if (StringUtils.isNotBlank(secretPath)) {
                map.put(PROPERTY_SECRET_PATH, secretPath);
            }
            customizeUri(mutableUri, map);
            // Added mutated uris to the list
            mutableUriList.add(mutableUri);
        });

        // Now convert the MutatableURI to final URI form.
        List<URI> remoteRepos = mutableUriList.stream().map(MutableUri::toUri).collect(Collectors.toList());

        // ... and instantiate GitProperties!
        return new GitProperties(remoteRepos, localPath, versionedSpringProperties.getBranch());
    }

    private void customizeUri(final MutableUri mutableUri, final Map<String, Object> map) {
        // Non credentials Management
        this.versionedURICustomizers.forEach(
                t -> t.accept(map, mutableUri));
        // CM
        credentialVersionedURICustomizers.ifPresent(
                credentialCustomizers -> credentialCustomizers.forEach(
                        t -> t.accept(map, mutableUri)));
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

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public void setCredentialVersionedURICustomizers(final Optional<List<VersionedURICustomizer>> credentialVersionedURICustomizers) {
        this.credentialVersionedURICustomizers = credentialVersionedURICustomizers;
    }

    public Optional<List<VersionedURICustomizer>> getCredentialVersionedURICustomizers() {
        return credentialVersionedURICustomizers;
    }
}
