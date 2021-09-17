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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import com.opentable.service.AppInfo;
import com.opentable.versionedconfig.VersionedConfigUpdate;
import com.opentable.versionedconfig.VersioningService;

public class ConfigUpdateService
{
    private static final Logger LOG = LoggerFactory.getLogger(ConfigUpdateService.class);

    private final VersioningService versioningService;

    private final AppInfo app;
    private final StandardEnvironment baseEnv;
    private Optional<String> latestValidRevision;

    public Optional<String> getLatestValidRevision() {
        return latestValidRevision;
    }

    public ConfigUpdateService(VersioningService versioningService,
                               AppInfo app,
                               StandardEnvironment baseEnv) {
        this.versioningService = versioningService;
        this.app = app;
        this.baseEnv = baseEnv;
        final Map<String, Object> map = new HashMap<>();
        map.put("OT_ENV_WHOLE", app.getEnvInfo().getWhole());
        final MapPropertySource m = new MapPropertySource("ot-env-whole", map);
        this.baseEnv.getPropertySources().addFirst(m);
    }

    public void tryUpdate() {
        versioningService.checkForUpdate().ifPresent(this::processUpdate);
    }

    private void processUpdate(VersionedConfigUpdate configUpdate) {
        LOG.info("Begin updating to {}", configUpdate.getNewRevision());
        try {
            loadRuleFiles(configUpdate);
            latestValidRevision = Optional.of(configUpdate.getNewRevision());
            LOG.info("New configuration {} armed and ready", configUpdate.getNewRevision());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The first VersionedConfigUpdate (initializingConfigUpdate) we get from the Versioning will just have
     * the root files of the config trees, as specified in the app source /config directories. We use those
     * to seed the initial configuration.
     * <p>
     * Subsequent updates look at all the files loaded (i.e. !!included files as well) in allKnownFiles
     */
    final void loadRuleFiles(VersionedConfigUpdate configUpdate) throws IOException {
        final List<Path> roots;
        try (Stream<String> configPaths = Files.lines(findConfig())) {
            roots = configPaths
                .map(Paths::get)
                .collect(Collectors.toList());
        }

        LOG.info("loaded new configuration with {}", roots);
        latestValidRevision = Optional.of(configUpdate.getNewRevision());
    }

    private Path findConfig() {
        final List<String> envs = new ArrayList<>();
        final List<String> fails = new ArrayList<>();

        if (app != null) {
            envs.add(StringUtils.trimToNull(app.getEnvInfo().getWhole()));
            envs.add(StringUtils.trimToNull(app.getEnvInfo().getEnvironment()));
            while (envs.remove(null)) {} // NOPMD - remove nulls repeatedly
        }
        if (envs.isEmpty()) {
            LOG.warn("Loading rules for development environment");
            envs.add("dev");
        }

        for (String env : envs) {
            if (StringUtils.isBlank(env)) {
                continue;
            }

            final Path rootsFile = versioningService.getCheckoutDirectory()
                    .resolve(env)
                    .resolve("config-roots.txt");

            if (Files.exists(rootsFile)) {
                LOG.info("Found configuration root at {}", rootsFile);
                return rootsFile;
            }

            LOG.debug("Didn't find {}", rootsFile);
            fails.add(rootsFile.toString());
        }
        throw new IllegalStateException("No configuration found at any of: " + fails);
    }
}
