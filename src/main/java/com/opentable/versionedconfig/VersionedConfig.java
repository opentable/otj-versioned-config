package com.opentable.versionedconfig;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.File;
import java.net.URI;

import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VersionedConfig {
    static final String X_OAUTH_BASIC = "x-oauth-basic";

    @Bean
    public GitProperties defaultVersioningServiceProperties(@Value("${config.repo.remote}") URI remoteRepo,
                                                            @Value("${config.repo.oauth-token:#{null}}") String token,
                                                            @Value("${config.repo.username:#{null}}") String username,
                                                            @Value("${config.repo.password:#{null}}") String password,
                                                            @Value("${config.repo.local:#{null}}") File localPath,
                                                            @Value("${config.repo.branch:master}") String branch) {
        if (!isNullOrEmpty(token)) {
            if (!(isNullOrEmpty(username) && isNullOrEmpty(password))) {
                throw new IllegalArgumentException("oauth-token and username/password are mutually exclusive");
            }
            username = token;
            password = X_OAUTH_BASIC;
        }

        return new GitProperties(remoteRepo, username, password, localPath, branch);
    }

    @Bean
    public VersioningService defaultVersioningService(@Named("defaultVersioningServiceProperties") GitProperties config) {
        return VersioningService.forGitRepository(config);
    }
}
