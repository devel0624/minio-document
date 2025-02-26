package com.nhnacademy.practice.minio;

import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author : 이성준
 * @since : 1.0
 */


public class MinioClient {

    public static S3Client create() {

        return S3Client.builder()
                .endpointOverride(URI.create("${host}"))
                .region(Region.US_EAST_1)
                .credentialsProvider(() -> AwsBasicCredentials.builder()
                        .accessKeyId("${accesskey}")
                        .secretAccessKey("${secretkey}")
                        .build())
                .forcePathStyle(true)
                .build();

    }
}
