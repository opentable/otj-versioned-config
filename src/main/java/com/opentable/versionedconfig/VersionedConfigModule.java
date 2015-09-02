package com.opentable.versionedconfig;

import com.google.inject.AbstractModule;

import com.opentable.config.ConfigProvider;

public class VersionedConfigModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(VersioningService.class).to(GitService.class).asEagerSingleton();
        bind(VersioningServiceProperties.class).toProvider(ConfigProvider.of(VersioningServiceProperties.class));
    }
}
