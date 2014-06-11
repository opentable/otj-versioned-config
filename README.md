otj-versioned-config
====================

Component for reading and polling for configuration from git.

Usage
-----

* In your project's Guice configure method, install a new VersionedConfigModule
* Bind an implementation of ConfigUpdateAction. This will handle update events whenever configuration updates are detected. The VersionedConfigModule will require a ConfigUpdateAction implementation to exist when Guice initializes it.
* Also make sure you bind a ConfigProvider that will provide an instance of VersioningServiceProperties (or a subclass thereof), e.g.:
``` 
bind(ConfigUpdateAction.class)
    .to(MyConfigUpdateHandler.class);
bind(VersioningServiceProperties.class)
    .toProvider(ConfigProvider.of(VersioningServiceProperties.class));
```
  This will read from the properties specified in the config file referenced by the system property ot.config.location

How it works
------------
The ConfigPollingService will spin up a single thread executor that pulls on the configured git repo periodically. By default this happens every 30 seconds but you can configure this to whatever you want.

After pulling we probe the configuration path to see if its HEAD SHA has changed. If so, we open the file to read and pass the input stream back to the configured ConfigUpdateAction implementation for processing, so that your application can do whatever it needs to do with the new configuration data.

Configuration Properties
------------------------
| Property name | Purpose | Example value |
| ------------- | ------- | ------------- |
| config.repo.remote | github repo URI | https://API_KEY_REPLACE_ME:x-oauth-basic@github.com/opentable/service-ot-frontdoor-config |
| config.repo.local | where to check out repo locally (URI) | file:frontdoor-config |
| config.repo.branch | The branch in the configuration repo to read | master |
| config.repo.file | The configuraation file to read (relative to repo) | /mappings.cfg.tsv |
| config.repo.polling.path.probe | Path in working copy to which we restrict update detection | . |
| config.repo.polling.interval.seconds | How often to poll for updates | 30 |

