package com.opentable.versionedconfig;

import com.google.inject.AbstractModule;

public class VersionedConfigModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(VersioningService.class).to(GitService.class);
        bind(ConfigPollingService.class).asEagerSingleton();
        // you're going to need to bind something which implements ConfigUpdateAction in your app
    }
}
