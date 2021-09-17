package com.opentable.versionedconfig.frontdoor;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.service.AppInfo;
import com.opentable.service.EnvInfo;
import com.opentable.versionedconfig.GitProperties;
import com.opentable.versionedconfig.GitPropertiesFactoryBean;
import com.opentable.versionedconfig.VersioningService;

@Configuration
@Import({
        ConfigUpdateService.class,
        AppInfo.class, EnvInfo.class
})
public class FrontdoorMiniConfiguration {

    @Bean
    public GitPropertiesFactoryBean myFactory() {
        return new GitPropertiesFactoryBean("mike");
    }

    @Bean
    public VersioningService defaultVersioningService(GitProperties config) {
        return VersioningService.forGitRepository(config);
    }

}
