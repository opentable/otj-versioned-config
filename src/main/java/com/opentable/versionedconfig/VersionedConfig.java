package com.opentable.versionedconfig;

import java.io.File;
import java.net.URI;

import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VersionedConfig {
    @Bean
    public GitProperties defaultVersioningServiceProperties(@Value("${config.repo.remote}") URI remoteRepo,
                                                            @Value("${config.repo.username}") String username,
                                                            @Value("${config.repo.password:x-oauth-basic}") String auth,
                                                            @Value("${config.repo.local:#{null}}") File localPath,
                                                            @Value("${config.repo.branch:master}") String branch) {
        return new GitProperties(remoteRepo, username, auth, localPath, branch);
    }


    @Bean
    public VersioningService defaultVersioningService(@Named("defaultVersioningServiceProperties") GitProperties config) {
        return VersioningService.forGitRepository(config);
    }

    /**
     * @deprecated use {@link VersionedConfig#defaultVersioningServiceProperties} instead
     */
    @Bean
    @Deprecated
    public VersioningServiceProperties versioningServiceProperties(
            @Named("defaultVersioningServiceProperties") GitProperties config) {
        return VersioningServiceProperties.fromGitProperties(config);
    }
}
