/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.e2e.connector.file.gcs;

import org.apache.seatunnel.e2e.common.util.ContainerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public class GcsUtils {
    private static final Logger logger = LoggerFactory.getLogger(GcsUtils.class);
    private final Storage storage;
    private final String projectId;
    private final String bucket;

    public GcsUtils(String projectId, String bucket, String credentialsPath) {
        this.projectId = Objects.requireNonNull(projectId, "projectId cannot be null");
        this.bucket = Objects.requireNonNull(bucket, "bucket cannot be null");

        try {
            StorageOptions.Builder builder = StorageOptions.newBuilder().setProjectId(projectId);

            if (credentialsPath != null && !credentialsPath.trim().isEmpty()) {
                File credentialsFile = new File(credentialsPath);
                if (!credentialsFile.exists()) {
                    throw new FileNotFoundException("Credentials file not found: " + credentialsPath);
                }

                try (FileInputStream credentialsStream = new FileInputStream(credentialsFile)) {
                    GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
                    builder.setCredentials(credentials);
                    logger.info("Using credentials from path: {}", credentialsPath);
                }
            }

            this.storage = builder.build().getService();
            logger.info("GCS client initialized for project: {}, bucket: {}", projectId, bucket);

        } catch (Exception e) {
            logger.error("Failed to initialize GCS client for project: {}, bucket: {}", projectId, bucket, e);
            throw new RuntimeException("GCS client initialization failed", e);
        }
    }

    public void uploadTestFiles(String filePath, String targetFilePath, boolean isFindFromResource) {
        try {
            File sourceFile = isFindFromResource ?
                    ContainerUtil.getResourcesFile(filePath) :
                    new File(filePath);

            if (!sourceFile.exists()) {
                throw new FileNotFoundException("Source file not found: " + sourceFile.getAbsolutePath());
            }

            byte[] fileContent = Files.readAllBytes(sourceFile.toPath());
            BlobId blobId = BlobId.of(bucket, targetFilePath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            storage.create(blobInfo, fileContent);
            logger.info("Successfully uploaded file to gs://{}/{}", bucket, targetFilePath);

        } catch (Exception e) {
            logger.error("Failed to upload file to GCS: {} -> gs://{}/{}",
                    filePath, bucket, targetFilePath, e);
            throw new RuntimeException("GCS upload failed", e);
        }
    }

    public void createDir(String dir) {
        try {
            String dirPath = dir.endsWith("/") ? dir : dir + "/";
            BlobId blobId = BlobId.of(bucket, dirPath + ".keep");
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            storage.create(blobInfo, new byte[0]);
            logger.info("Successfully created directory marker: gs://{}/{}", bucket, dirPath);

        } catch (Exception e) {
            logger.error("Failed to create directory marker in GCS: gs://{}/{}", bucket, dir, e);
            throw new RuntimeException("GCS directory creation failed", e);
        }
    }

    // 添加文件存在性检查方法，用于测试验证
    public boolean fileExists(String filePath) {
        try {
            BlobId blobId = BlobId.of(bucket, filePath);
            return storage.get(blobId) != null;
        } catch (Exception e) {
            logger.warn("Error checking file existence: gs://{}/{}", bucket, filePath, e);
            return false;
        }
    }

    // 添加清理方法，用于测试后清理
    public void deleteFile(String filePath) {
        try {
            BlobId blobId = BlobId.of(bucket, filePath);
            boolean deleted = storage.delete(blobId);
            if (deleted) {
                logger.info("Successfully deleted: gs://{}/{}", bucket, filePath);
            } else {
                logger.warn("File not found for deletion: gs://{}/{}", bucket, filePath);
            }
        } catch (Exception e) {
            logger.error("Failed to delete file: gs://{}/{}", bucket, filePath, e);
            throw new RuntimeException("GCS file deletion failed", e);
        }
    }

    public void close() {
        // Storage client 是线程安全的，通常不需要关闭
        // 但在测试环境中，如果需要完全清理，可以调用此方法
        logger.info("GCS utils closed");
    }
}

