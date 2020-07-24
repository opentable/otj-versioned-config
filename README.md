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
  Now `@Inject VersioningService`!

The old, deprecated methodology of importing `VersionedConfig` is still
supported for now but strongly recommended against. We will eventually remove this.

Credentials Management
-----
* make sure you configure  `ot.versioned-config.${name}.remote.${remote}.secret` (see table below)
* Add the following:
```
@Bean
public VersionedConfigCredentialsProvider versionedConfigCredentialsProvider(CredentialsClient credentialsClient) {
    return new VersionedConfigCredentialsProvider(credentialsClient);
}
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

Configuration Properties (using GitPropertiesFactoryBean)
---------

------------------------
| Property name | Purpose | Example value |
| ------------- | ------- | ------------- |
| ot.versioned-config.${name}.local | REQUIRED.where to check out repo locally (URI) | frontdoor-config |
| ot.versioned-config.${name}.branch | REQUIRED.The branch in the configuration repo to read | master |
| ot.versioned-config.${name}.remotes | REQUIRED. comma delimited list of remotes, each referenced by a unique name ${remote} | foo, bar
| ot.versioned-config.${name}.remote.${remote).uri | REQUIRED. URI for this specific remote listed in the remotes list . May include credentials if not using Credentials Managment | https://github.com/opentable/service-ot-frontdoor-config |
| ot.versioned-config.${name}.remote.${remote}.secret | if using Credentials Management, this is the named of the shared secret manifest. | my-github-secret |

_Example_

For a bean where I pass "myclient" to the `GitPropertiesFactoryBean`,
and have two remotes, named github and mirror.


```
ot.versioned-config.myclient.local=$MESOS_SANDBOX
ot.versioned-config.myclient.branch=master
ot-versioned-config.myclient.remotes=mirror,github
ot-versioned-config.myclient.mirror.uri=https://docker-mirror-ci-sf.otenv.com
ot-versioned-config.myclient.mirror.secret=shared-mirror-secret
ot-versioned-config.myclient.github.uri=https://github.com/opentable/service-ot-frontdoor-config.git
ot-versioned-config.myclient.github.secret=shared-frontdoor-github-secret
```

Configuration Properties (using `VersionedConfig` deprecated)
----

| Property name | Purpose | Example value |
| ------------- | ------- | ------------- |
| config.repo.remote |comma delimited list of repo URIs. May include credentials if not using Credentials Managment | https://github.com/opentable/service-ot-frontdoor-config |
| config.repo.local | where to check out repo locally (URI) | frontdoor-config |
| config.repo.branch | The branch in the configuration repo to read | master |
