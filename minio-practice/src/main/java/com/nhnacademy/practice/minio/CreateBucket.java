package com.nhnacademy.practice.minio;

/**
 * @since : 1.0
 */

// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import java.util.List;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

/**
 * Create an Amazon S3 bucket.
 *
 * This code expects that you have AWS credentials set up per:
 * http://docs.aws.amazon.com/java-sdk/latest/developer-guide/setup-credentials.html
 */
public class CreateBucket {

    public static Bucket getBucket(String bucket_name) {
        try (S3Client s3 = MinioClient.create()) {

            ListBucketsResponse response = s3.listBuckets();

            if (response.hasBuckets()) {
                List<Bucket> buckets = response.buckets();
                for (Bucket b : buckets) {
                    if (b.name().equals(bucket_name)) {
                        return b;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Bucket createBucket(String bucket_name) {
        final S3Client s3 = MinioClient.create();
        Bucket b = null;
        if (getBucket(bucket_name) != null) {
            System.out.format("Bucket %s already exists.\n", bucket_name);
            b = getBucket(bucket_name);
        } else {
            try {

                CreateBucketResponse createBucketResponse
                        = s3.createBucket(c -> c.bucket(bucket_name));

                if (createBucketResponse.sdkHttpResponse().isSuccessful()) {
                    b = getBucket(bucket_name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return b;
    }

    public static void main(String[] args) {
        final String USAGE = "\n" +
                "CreateBucket - create an S3 bucket\n\n" +
                "Usage: CreateBucket <bucketname>\n\n" +
                "Where:\n" +
                "  bucketname - the name of the bucket to create.\n\n" +
                "The bucket name must be unique, or an error will result.\n";


        String bucket_name = MinioClient.sample_bucket_name;

        System.out.format("\nCreating S3 bucket: %s\n", bucket_name);
        Bucket b = createBucket(bucket_name);
        if (b == null) {
            System.out.println("Error creating bucket!\n");
        } else {
            System.out.println("Done!\n");
        }
    }
}
