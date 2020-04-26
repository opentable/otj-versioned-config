otj-versioned-config
====================

Component for reading and polling for configuration from git.

Usage
-----

Typical usage:

* Configure the properties (see below)

```
@Bean
public GitPropertiesFactoryBean myFactory(
    return new GitPropertiesFactoryBean("a unique name here");
)
@Bean
public VersioningService defaultVersioningService(GitProperties config) {
    return VersioningService.forGitRepository(config);
 }
```
  Now inject VersioningService!

The old, deprecated methodology of importing `VersionedConfig` is still
supported but strongly recommended against.

How it works
------------
The VersioningService maintains a local clone of the remote Git repository.
Any time you call `checkForUpdate()`, it will do a fetch of the remote repository,
and indicate whether any changes were found.

At any time you may invoke `getCurrentState()` or `getLatestRevision()` to get the
state of the *local* repository.

Remember to `close()` your versioning service when you are done with it to clean
up the local checkout.  (This is done for you if you use the Spring integration.)

Configuration Properties (using GitPropertiesFactoryBean)
---------
${config.repo.remote}") List<URI> remoteRepo,
                                                            @Value("${config.repo.local:#{null}}") Path localPath,
                                                            @Value("${config.repo.branch:master}"
------------------------
| Property name | Purpose | Example value |
| ------------- | ------- | ------------- |
| ot.versioned-config.${name}.local | where to check out repo locally (URI) | frontdoor-config |
| ot.versioned-config.${name}.branch | The branch in the configuration repo to read | master |
| ot.versioned-config.${name}.repo | comma delimited list of repo URIs. May include credentials if not using Credentials Managment | https://github.com/opentable/service-ot-frontdoor-config |
| ot.versioned-config.${name}.secrets.(index) | if using cm this is the named of the shared secret manifest. index corresponds to the entry of repo (0, 1..) | my-github-secret |

Configuration Properties (using `VersionedConfig` deprecated)
------------------------
| Property name | Purpose | Example value |
| ------------- | ------- | ------------- |
| config.repo.remote |comma delimited list of repo URIs. May include credentials if not using Credentials Managment | https://github.com/opentable/service-ot-frontdoor-config |
| config.repo.local | where to check out repo locally (URI) | frontdoor-config |
| config.repo.branch | The branch in the configuration repo to read | master |
