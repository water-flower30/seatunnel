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

package org.apache.seatunnel.e2e.connector.file.abfs;

import org.apache.seatunnel.e2e.common.util.ContainerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

public class AbfsUtils {
    private static Logger logger = LoggerFactory.getLogger(AbfsUtils.class);
    private CloudBlobClient blobClient = null;
    private CloudBlobContainer container = null;
    private String accountName = "your-account-name";
    private String accountKey = "your-account-key";
    private String containerName = "your-container";

    public AbfsUtils() {
        try {
            String storageConnectionString =
                    String.format(
                            "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net",
                            accountName, accountKey);
            CloudStorageAccount storageAccount =
                    CloudStorageAccount.parse(storageConnectionString);
            blobClient = storageAccount.createCloudBlobClient();
            container = blobClient.getContainerReference(containerName);
            container.createIfNotExists();
        } catch (URISyntaxException | InvalidKeyException | StorageException e) {
            logger.error("Failed to initialize ABFS client", e);
            throw new RuntimeException(e);
        }
    }

    public void uploadTestFiles(
            String filePath, String targetFilePath, boolean isFindFromResource) {
        try {
            File resourcesFile = null;
            if (isFindFromResource) {
                resourcesFile = ContainerUtil.getResourcesFile(filePath);
            } else {
                resourcesFile = new File(filePath);
            }

            CloudBlockBlob blob = container.getBlockBlobReference(targetFilePath);
            FileInputStream fileInputStream = new FileInputStream(resourcesFile);
            blob.upload(fileInputStream, resourcesFile.length());
            fileInputStream.close();

            logger.info("Uploaded file to ABFS: {}", targetFilePath);
        } catch (URISyntaxException | StorageException | IOException e) {
            logger.error("Failed to upload file to ABFS: {}", targetFilePath, e);
            throw new RuntimeException(e);
        }
    }

    public void createDir(String dir) {
        try {
            // Azure Blob Storage doesn't have directories, but we can create a marker blob
            CloudBlockBlob blob = container.getBlockBlobReference(dir + "/.keep");
            blob.uploadText("");
            logger.info("Created directory marker in ABFS: {}", dir);
        } catch (URISyntaxException | StorageException | IOException e) {
            logger.error("Failed to create directory in ABFS: {}", dir, e);
            throw new RuntimeException(e);
        }
    }

    public void close() {
        // Azure Storage SDK doesn't require explicit closing
        logger.info("ABFS utils closed");
    }
}
