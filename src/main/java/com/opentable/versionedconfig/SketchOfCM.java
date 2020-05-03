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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class SketchOfCM implements VersionedURICustomizer {
    @Override
    public void accept(final Map<String, Object> properties, final MutableUri versionedUri) {
        final String secretPath = (String) properties.get(GitPropertiesFactoryBean.PROPERTY_SECRET_PATH);
        if (StringUtils.isNotBlank(secretPath) && !versionedUri.hasPassword()) {
            // get a secret
            String username = null;
            String password = null;
            versionedUri.setPassword(password);
            versionedUri.setUsername(username);
        }
    }
}
