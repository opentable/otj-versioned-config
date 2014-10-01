package com.opentable.versionedconfig;

import java.io.IOException;

public class VersioningServiceException extends Exception
{
    public VersioningServiceException(String message, Throwable cause)
    {
        super(message, cause);
    }
    public VersioningServiceException(String message)
    {
        super(message);
    }

    public VersioningServiceException(IOException e)
    {
        super(e);
    }
}
