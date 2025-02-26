package com.nhnacademy.practice.minio;
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import java.util.List;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

/**
 * List your Amazon S3 buckets.
 * <p>
 * This code expects that you have AWS credentials set up per:
 * http://docs.aws.amazon.com/java-sdk/latest/developer-guide/setup-credentials.html
 */
public class ListBuckets {
    public static void main(String[] args) {
        final S3Client s3 = MinioClient.create();

        ListBucketsResponse response = s3.listBuckets();

        if (response.hasBuckets()) {
            System.out.println("Your Amazon S3 buckets are:");

            List<Bucket> buckets = response.buckets();
            for (Bucket b : buckets) {
                System.out.println("* " + b.name());
            }
        } else {
            System.out.println("No buckets found");
        }
    }
}