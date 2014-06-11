package com.opentable.versionedconfig;

import java.io.InputStream;
import java.util.function.Consumer;

public interface ConfigUpdateAction extends Consumer<InputStream>
{
}
