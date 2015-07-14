package com.opentable.versionedconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ConfigPollingServiceTest {

    @Test
    public void constructorProvidesInitialUpdate() {
        final VersioningService versioning = mock(VersioningService.class);
        when(versioning.getCheckoutDirectory()).thenReturn(Paths.get("/tmp/stuff"));

        final Consumer<VersionedConfigUpdate> receiver = mock(Consumer.class);
        final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

        final long delaySec = 10;
        final VersioningServiceProperties properties = createVersioningServiceProperties(delaySec);

        final ConfigPollingService pollingService = new ConfigPollingService(versioning, properties, receiver, executor);

        final ArgumentCaptor<VersionedConfigUpdate> updateCatcher = ArgumentCaptor.forClass(VersionedConfigUpdate.class);
        verify(receiver, times(1)).accept(updateCatcher.capture());
        final Set<Path> result = updateCatcher.getValue().getAlteredPaths();
        assertEquals(2, result.size());
        assertTrue(result.contains(Paths.get("/tmp/stuff/items.txt")));
        assertTrue(result.contains(Paths.get("/tmp/stuff/things.txt")));
    }

    @Test
    public void constructorSchedulesUpdates() {
        final VersioningService versioning = mock(VersioningService.class);
        when(versioning.getCheckoutDirectory()).thenReturn(Paths.get("/tmp/stuff"));

        final Consumer<VersionedConfigUpdate> receiver = (x) -> {};
        final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

        when(versioning.getCheckoutDirectory()).thenReturn(Paths.get("/tmp/stuff"));
        final long delaySec = 10;
        final VersioningServiceProperties properties = createVersioningServiceProperties(delaySec);
        final ConfigPollingService pollingService = new ConfigPollingService(versioning, properties, receiver, executor);

        verify(executor).scheduleAtFixedRate(any(), eq(delaySec), eq(delaySec), eq(TimeUnit.SECONDS));
    }

    @Test
    public void scheduledUpdatesFeedConsumer() {
        final VersioningService versioning = mock(VersioningService.class);
        when(versioning.getCheckoutDirectory()).thenReturn(Paths.get("/tmp/stuff"));
        when(versioning.checkForUpdate()).thenReturn(Optional.of(new VersionedConfigUpdate(ImmutableSet.of(), ImmutableSet.of())));

        final Consumer<VersionedConfigUpdate> receiver = mock(Consumer.class);
        final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

        final long delaySec = 10;
        final VersioningServiceProperties properties = createVersioningServiceProperties(delaySec);
        final ConfigPollingService pollingService = new ConfigPollingService(versioning, properties, receiver, executor);

        // do what executor does
        pollingService.update();

        verify(receiver, times(2)).accept(any());
    }

    private VersioningServiceProperties createVersioningServiceProperties(long delaySec) {
        final VersioningServiceProperties properties = mock(VersioningServiceProperties.class);
        when(properties.configPollingIntervalSeconds()).thenReturn(delaySec);
        when(properties.localConfigRepository()).thenReturn(new File("/tmp/stuff"));
        when(properties.configFiles()).thenReturn(ImmutableList.of("items.txt", "things.txt"));
        when(properties.configPollingIntervalSeconds()).thenReturn(delaySec);
        return properties;
    }

}
