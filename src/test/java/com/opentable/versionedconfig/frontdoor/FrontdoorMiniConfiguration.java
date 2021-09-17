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
