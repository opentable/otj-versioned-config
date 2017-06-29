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
| config.repo.oauth-token | github oauth token |
| config.repo.username | username | *your username* |
| config.repo.password | password | *your password* |
| config.repo.local | where to check out repo locally (URI) | frontdoor-config |
| config.repo.branch | The branch in the configuration repo to read | master |
| config.repo.file | The configuraation file to read (relative to repo) | /mappings.cfg.tsv |

**Note**: `config.repo.oauth-token` and `config.repo.username/password` are mutually exclusive.
You should use one or the other, but not both. (Setting an `oauth-token` automatically sets your
password to `"x-oauth-basic"`.)
