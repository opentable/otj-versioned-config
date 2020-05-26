package com.opentable.versionedconfig.credentials;

public class VersionedConfigSecret {
    private String username;
    private String password;

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(255).append("VersionedConfigSecret{");
        sb.append("username='").append(username).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
