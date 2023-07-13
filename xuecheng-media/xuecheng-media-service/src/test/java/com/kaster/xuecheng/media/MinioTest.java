package com.kaster.xuecheng.media;


import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MinioTest {

    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://127.0.0.1:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();


    @Test
    public void testUpload() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket("testbucket")
                .filename("D:\\BaiduNetdiskDownload\\学成\\day05 媒资管理 Nacos Gateway MinIO\\资料\\第3章媒资管理模块v3.1.docx")
                .object("test.docx")
                .build();

        minioClient.uploadObject(uploadObjectArgs);
    }
}
