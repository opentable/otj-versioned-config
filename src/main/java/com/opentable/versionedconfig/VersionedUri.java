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

import org.springframework.web.util.UriComponentsBuilder;

public class VersionedUri {
    private final String host;
    private final int port;
    private final String path;
    private String username;
    private String password;
    private String scheme;
    public VersionedUri(URI uri) {
        this.host = uri.getHost();
        this.port = uri.getPort();
        this.path = uri.getPath();
        this.scheme = uri.getScheme();
        if (uri.getUserInfo() != null) {
            String[] userInfo = uri.getUserInfo().split(":");
            username = userInfo[0].trim();
            password = userInfo[1].trim();
        }
    }

    public String getHost() {
        return host;
    }

    boolean hasPassword() {
        return password != null;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public URI toUri() {
        return UriComponentsBuilder
                .newInstance()
                .host(host)
                .scheme(scheme)
                .path(path)
                .port(port)
                .userInfo(toUserInfo())
                .build().toUri();
    }

    private String toUserInfo() {
        return username + ":" + password;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("VersionedUri{");
        sb.append("host='").append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append(", path='").append(path).append('\'');
        sb.append(", username='").append(username).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
