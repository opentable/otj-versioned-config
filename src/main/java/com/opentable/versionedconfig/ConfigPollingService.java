package com.opentable.versionedconfig;


import static java.util.stream.Collectors.toSet;

import java.io.Closeable;
import java.io.File;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import com.opentable.logging.Log;

/**
 * This service periodically checks for configuration updates and applies them.
 */
public class ConfigPollingService implements Closeable
{
    private static final Log LOG = Log.findLog();

    private final ScheduledExecutorService updateExecutor = Executors.newScheduledThreadPool(1,
            new ThreadFactoryBuilder().setNameFormat("config-update").build()
    );
    private final VersioningService versioning;
    private final Consumer<VersionedConfigUpdate> onUpdate;

    @Inject
    public ConfigPollingService(VersioningService versioning,
            VersioningServiceProperties runtimeProperties,
            Consumer<VersionedConfigUpdate> onUpdate) throws VersioningServiceException
    {
        LOG.info("ConfigUpdateService initializing");
        this.versioning = versioning;
        this.onUpdate = onUpdate;

        LOG.info("ConfigUpdateService seeding initial configuration for FrontDoorService");
        final Set<File> pathsOfInterest = runtimeProperties.configFiles().stream()
                .map(filename -> new File(runtimeProperties.localConfigRepository(), filename))
                .collect(toSet());
        final VersionedConfigUpdate initialUpdate = new VersionedConfigUpdate(ImmutableSet.copyOf(pathsOfInterest));
        onUpdate.accept(initialUpdate);
        if (runtimeProperties.configPollingIntervalSeconds() > 0) {
            updateExecutor.scheduleAtFixedRate(this::update,
                    runtimeProperties.configPollingIntervalSeconds(),
                    runtimeProperties.configPollingIntervalSeconds(),
                    TimeUnit.SECONDS);
        } else {
            LOG.warn("Config update polling is disabled!");
        }
    }

    @Override
    public void close()
    {
        updateExecutor.shutdown();
    }

    public void update()
    {
        try {
            versioning.checkForUpdate(onUpdate);
        } catch (VersioningServiceException error) {
            LOG.error(error, "Could not reconfigure service! Serious configuration error!");
            // TODO put some kind of notification in JMX here.
        }
    }
}
