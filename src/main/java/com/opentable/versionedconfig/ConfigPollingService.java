package com.opentable.versionedconfig;


import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

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
    private final ConfigUpdateAction onUpdate;

    @Inject
    public ConfigPollingService(VersioningService versioning,
            VersioningServiceProperties runtimeProperties,
            ConfigUpdateAction onUpdate) throws VersioningServiceException
    {
        LOG.info("ConfigUpdateService initializing");
        this.versioning = versioning;
        this.onUpdate = onUpdate;

        LOG.info("ConfigUpdateService seeding initial configuration for FrontDoorService");
        versioning.readConfig(onUpdate);

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
