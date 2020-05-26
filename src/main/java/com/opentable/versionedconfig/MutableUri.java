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
import java.util.Objects;

import org.springframework.web.util.UriComponentsBuilder;

public class MutableUri implements Comparable<MutableUri> {
    private final String host;
    private final int port;
    private final String path;
    private String username;
    private String password;
    private String scheme;
    public MutableUri(URI uri) {
        this.host = uri.getHost();
        this.port = uri.getPort();
        this.path = uri.getPath();
        this.scheme = uri.getScheme();
        if (uri.getUserInfo() != null) {
            String[] userInfo = uri.getUserInfo().split(":");
            username = userInfo.length > 0 ? userInfo[0].trim() : null;
            password = userInfo.length > 1 ? userInfo[1].trim() : null;
        }
    }

    public String getHost() {
        return host;
    }

    public boolean hasPassword() {
        return password != null;
    }

    public MutableUri setPassword(final String password) {
        this.password = password;
        return this;
    }

    public MutableUri setUsername(final String username) {
        this.username = username;
        return this;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(final String scheme) {
        this.scheme = scheme;
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
        if (username != null) {
            return password == null ? username : username + ": " + password;
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(255).append("VersionedUri{");
        sb.append("host='").append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append(", path='").append(path).append('\'');
        sb.append(", username='").append(username).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MutableUri that = (MutableUri) o;
        return port == that.port &&
                Objects.equals(host, that.host) &&
                Objects.equals(path, that.path) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(scheme, that.scheme);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, path, username, password, scheme);
    }

    @Override
    public int compareTo(final MutableUri o) {
        return toUri().compareTo(o.toUri());
    }
}
