package edu.jhu.thrax.util;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.net.URI;

public class DefaultConfigFileLoader
{
    public static InputStream getConfigStream(URI configURI) throws IOException
    { 
        return new FileInputStream(new File(configURI.getPath()));
    }
}

		
