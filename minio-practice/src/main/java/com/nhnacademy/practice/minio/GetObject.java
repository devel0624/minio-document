package com.nhnacademy.practice.minio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * @author : 이성준
 * @since : 1.0
 */


public class GetObject {

    public static void main(String[] args) {
        S3Client s3 = MinioClient.create();

        String bucket_name = MinioClient.sample_bucket_name;
        String object_name = MinioClient.sample_object_name;


        try {
            ResponseInputStream<GetObjectResponse> stream =
                    s3.getObject(req -> req.bucket(bucket_name).key(object_name));

            String USER_HOME = System.getProperty("user.home");
            String DOWNLOAD_DIRECTORY = USER_HOME + "/Downloads";
            String DOWNLOAD_TEMP_FILE_PATH = DOWNLOAD_DIRECTORY + "/" + UUID.randomUUID() + ".tmp";

            File file = new File(DOWNLOAD_TEMP_FILE_PATH);

            byte[] bytes = stream.readAllBytes();

            boolean isNew = file.createNewFile();


            while (!isNew){
                DOWNLOAD_TEMP_FILE_PATH = DOWNLOAD_DIRECTORY + "/" + UUID.randomUUID() + ".tmp";
                file = new File(DOWNLOAD_TEMP_FILE_PATH);
                isNew = file.createNewFile();
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
                fos.flush();

                String DOWNLOAD_FILE_NAME = DOWNLOAD_DIRECTORY + "/" + object_name;
                Files.move(Paths.get(DOWNLOAD_TEMP_FILE_PATH), Paths.get(DOWNLOAD_FILE_NAME));
            } catch (IOException e) {
                e.printStackTrace();
                file.delete();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
