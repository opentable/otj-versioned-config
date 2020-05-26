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
package com.opentable.versionedconfig.credentials;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.credentials.client.api.CredentialsClient;
import com.opentable.versionedconfig.GitPropertiesFactoryBean;
import com.opentable.versionedconfig.MutableUri;
import com.opentable.versionedconfig.VersionedURICustomizer;

public class VersionedConfigCredentialsProvider implements VersionedURICustomizer {
    private static final Logger LOG = LoggerFactory.getLogger( VersionedConfigCredentialsProvider.class );

    private final CredentialsClient credentialsClient;
    private final String sharedManifestName;

    // Defined in an @Bean as needed.
    // eg
    // @Bean
    // public VersionedConfigCredentialsProvider(CredentialsClient client) {
    //      return new VersionedConfigCredentialsProvider(client, "mysecretnane");
    // }
    public VersionedConfigCredentialsProvider(CredentialsClient client, String sharedManifestName) {
        this.credentialsClient = client;
        this.sharedManifestName = sharedManifestName;
    }

    @Override
    public void accept(final Map<String, Object> properties, final MutableUri versionedUri) {
        final String secretPath = (String) properties.get(GitPropertiesFactoryBean.PROPERTY_SECRET_PATH);
        if (StringUtils.isNotBlank(secretPath) && !versionedUri.hasPassword()) {
            // get a secret
            final VersionedConfigSecret secret = credentialsClient.getSharedSecret(sharedManifestName, VersionedConfigSecret.class).get();
            final String username = secret.getUsername();
            final String password = secret.getPassword();
            versionedUri.setPassword(password);
            versionedUri.setUsername(username);
            LOG.debug("Successfully injected the VersionedConfigSecret");
        }
    }
}
