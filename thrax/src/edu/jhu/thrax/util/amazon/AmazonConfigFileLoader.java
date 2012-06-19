package edu.jhu.thrax.util.amazon;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;

public class AmazonConfigFileLoader
{
    protected static final String CRED_PROPS = "AwsCredentials.properties"; 

    public static InputStream getConfigStream(URI configURI) throws IOException
    {
        InputStream resStream = AmazonConfigFileLoader.class.getResourceAsStream(CRED_PROPS);

        if (resStream == null) {
            resStream = AmazonConfigFileLoader.class.getResourceAsStream("/" + CRED_PROPS);
        }

        if (resStream == null) {
            throw new IllegalArgumentException("Could not locate " + CRED_PROPS);
        }

        AmazonS3 s3 = new AmazonS3Client(new PropertiesCredentials(resStream));
        return s3.getObject(new GetObjectRequest(configURI.getHost(), configURI.getPath().replaceFirst("/+", ""))).getObjectContent();  
    }
}
