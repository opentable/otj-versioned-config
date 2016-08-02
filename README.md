otj-versioned-config
====================

Component for reading and polling for configuration from git.

Usage
-----

For Spring projects, in your configuration classes just import VersionedConfig.  This will automatically determine the
Git repository to watch via application properties, and provide a `VersioningService` that tracks changes.

You can also configure it manually if you prefer:
```java
VersioningService config = VersioningService.forGitRepo(
    new VersioningServiceProperties()
        .setRemoteConfigRepository("http://git.somewhere.com/..."));
```

How it works
------------
The VersioningService maintains a local clone of the remote Git repository.
Any time you call `checkForUpdate()`, it will do a fetch of the remote repository,
and indicate whether any changes were found.

At any time you may invoke `getCurrentState()` or `getLatestRevision()` to get the
state of the *local* repository.

Remember to `close()` your versioning service when you are done with it to clean
up the local checkout.  (This is done for you if you use the Spring integration.)

Configuration Properties
------------------------
| Property name | Purpose | Example value |
| ------------- | ------- | ------------- |
| config.repo.remote | github repo URI | https://github.com/opentable/service-ot-frontdoor-config |
| config.repo.username | username or API key | *your github API key* |
| config.repo.password | password or authentication method | x-oauth-basic |
| config.repo.local | where to check out repo locally (URI) | frontdoor-config |
| config.repo.branch | The branch in the configuration repo to read | master |
| config.repo.file | The configuraation file to read (relative to repo) | /mappings.cfg.tsv |
