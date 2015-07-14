package com.opentable.versionedconfig;


import static java.util.stream.Collectors.toList;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import com.opentable.logging.Log;

/**
 * This service periodically checks for configuration updates and applies them.
 */
@Singleton
public class ConfigPollingService implements Closeable
{
    private static final Log LOG = Log.findLog();

    private final VersioningService versioning;
    private final Consumer<VersionedConfigUpdate> onUpdate;
    private final ScheduledExecutorService updateExecutor;
    private final List<Path> allPathsOfInterest;

    @Inject
    public ConfigPollingService(VersioningService versioning,
            VersioningServiceProperties runtimeProperties,
            Consumer<VersionedConfigUpdate> onUpdate) throws VersioningServiceException
    {
        this(versioning, runtimeProperties, onUpdate, Executors.newScheduledThreadPool(1,
                new ThreadFactoryBuilder().setNameFormat("config-update").build()
        ));
    }

    @VisibleForTesting
    ConfigPollingService(VersioningService versioning,
            VersioningServiceProperties runtimeProperties,
            Consumer<VersionedConfigUpdate> onUpdate,
            ScheduledExecutorService updateExecutor) {
        LOG.info("ConfigUpdateService initializing");
        this.updateExecutor = updateExecutor;
        this.versioning = versioning;
        this.onUpdate = onUpdate;

        LOG.info("ConfigUpdateService seeding initial configuration for FrontDoorService");
        this.allPathsOfInterest = runtimeProperties.configFiles().stream()
                .map(filename -> versioning.getCheckoutDirectory().resolve(filename))
                .collect(toList());
        final VersionedConfigUpdate initialUpdate = new VersionedConfigUpdate(new HashSet<>(allPathsOfInterest), allPathsOfInterest);
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
            final Optional<VersionedConfigUpdate> newStuff = versioning.checkForUpdate();
            newStuff.ifPresent(onUpdate::accept);
        }
        catch (VersioningServiceException error) {
            LOG.error(error, "Could not reconfigure service! Serious configuration error!");
            // TODO put some kind of notification in JMX here.
        }
    }
}
