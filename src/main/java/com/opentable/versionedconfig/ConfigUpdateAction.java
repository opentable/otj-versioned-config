package com.opentable.versionedconfig;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * A consumer of a Config update stream. Client apps should bind
 * an implementation of this. It will receive update streams.
 */
public interface ConfigUpdateAction extends Consumer<InputStream>
{
}
