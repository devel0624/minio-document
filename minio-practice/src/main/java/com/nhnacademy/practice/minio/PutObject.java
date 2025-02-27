package com.nhnacademy.practice.minio;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author : 이성준
 * @since : 1.0
 */


public class PutObject {

    public static void main(String[] args) {
        S3Client s3 = MinioClient.create();

        String bucket_name = MinioClient.sample_bucket_name;
        String object_name = MinioClient.sample_object_name;

        URL sampleUrl = ClassLoader.getSystemResource(object_name);

        try {
            File file = new File(sampleUrl.toURI());
            s3.putObject(req -> req.bucket(bucket_name).key(object_name) , RequestBody.fromFile(file));

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }
}
