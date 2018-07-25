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
import java.nio.file.Path;
import java.util.List;

import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VersionedConfig {
    static final String X_OAUTH_BASIC = "x-oauth-basic";

    @Bean
    public GitProperties defaultVersioningServiceProperties(@Value("${config.repo.remote}") List<URI> remoteRepo,
                                                            @Value("${config.repo.local:#{null}}") Path localPath,
                                                            @Value("${config.repo.branch:master}") String branch) {
        return new GitProperties(remoteRepo, localPath, branch);
    }

    @Bean
    public VersioningService defaultVersioningService(@Named("defaultVersioningServiceProperties") GitProperties config) {
        return VersioningService.forGitRepository(config);
    }
}
