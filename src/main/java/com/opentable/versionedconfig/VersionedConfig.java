package com.opentable.versionedconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        VersioningServiceProperties.class
})
public class VersionedConfig
{
    @Bean
    public VersioningService versioningService(VersioningServiceProperties config) {
        return VersioningService.forGitRepository(config);
    }
}
