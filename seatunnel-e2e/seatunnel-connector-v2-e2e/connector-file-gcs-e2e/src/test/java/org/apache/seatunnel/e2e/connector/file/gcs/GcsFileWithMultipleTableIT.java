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

import org.apache.seatunnel.e2e.common.TestSuiteBase;
import org.apache.seatunnel.e2e.common.container.ContainerExtendedFactory;
import org.apache.seatunnel.e2e.common.container.TestContainer;
import org.apache.seatunnel.e2e.common.container.TestHelper;
import org.apache.seatunnel.e2e.common.junit.TestContainerExtension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Disabled("Disabled because it needs user's personal GCS account to run this test")
public class GcsFileWithMultipleTableIT extends TestSuiteBase {

    private static final Logger logger = LoggerFactory.getLogger(GcsFileWithMultipleTableIT.class);

    public static final String GCS_CONNECTOR_DOWNLOAD =
            "https://repo1.maven.org/maven2/com/google/cloud/bigdataoss/gcs-connector/hadoop3-2.2.0/gcs-connector-hadoop3-2.2.0-shaded.jar";
    public static final String GOOGLE_CLOUD_STORAGE_DOWNLOAD =
            "https://repo1.maven.org/maven2/com/google/cloud/google-cloud-storage/2.5.0/google-cloud-storage-2.5.0.jar";

    // 从环境变量获取配置
    private final String projectId = System.getenv().getOrDefault("GCS_PROJECT_ID", "your-project-id");
    private final String bucket = System.getenv().getOrDefault("GCS_BUCKET", "your-test-bucket");
    private final String credentialsPath = System.getenv().getOrDefault("GOOGLE_APPLICATION_CREDENTIALS", "");

    private GcsUtils gcsUtils;
    private List<String> uploadedFiles = new ArrayList<>();
    private final String testPrefix = "test/seatunnel/multi-table/" + System.currentTimeMillis() + "/";

    @TestContainerExtension
    private final ContainerExtendedFactory extendedFactory =
            container -> {
                logger.info("Setting up GCS dependencies for multiple table tests...");

                Container.ExecResult extraCommands =
                        container.execInContainer(
                                "bash",
                                "-c",
                                "mkdir -p /tmp/seatunnel/plugins/gcs/lib && cd /tmp/seatunnel/plugins/gcs/lib && curl -L -O "
                                        + GCS_CONNECTOR_DOWNLOAD);
                Assertions.assertEquals(0, extraCommands.getExitCode(), "Failed to download GCS connector");

                extraCommands =
                        container.execInContainer(
                                "bash",
                                "-c",
                                "cd /tmp/seatunnel/plugins/gcs/lib && curl -L -O " + GOOGLE_CLOUD_STORAGE_DOWNLOAD);
                Assertions.assertEquals(0, extraCommands.getExitCode(), "Failed to download Google Cloud Storage library");

                extraCommands =
                        container.execInContainer(
                                "bash",
                                "-c",
                                "cd /tmp/seatunnel/lib && curl -L -O " + GCS_CONNECTOR_DOWNLOAD);
                Assertions.assertEquals(0, extraCommands.getExitCode(), "Failed to download GCS connector to lib");

                extraCommands =
                        container.execInContainer(
                                "bash", "-c", "cd /tmp/seatunnel/lib && curl -L -O " + GOOGLE_CLOUD_STORAGE_DOWNLOAD);
                Assertions.assertEquals(0, extraCommands.getExitCode(), "Failed to download Google Cloud Storage library to lib");

                logger.info("GCS dependencies setup completed for multiple table tests");
            };

    @BeforeEach
    void setUp() {
        logger.info("Initializing GCS utils for multiple table tests - project: {}, bucket: {}", projectId, bucket);
        gcsUtils = new GcsUtils(projectId, bucket, credentialsPath);
    }

    @AfterEach
    void tearDown() {
        logger.info("Cleaning up multiple table test files from GCS...");
        try {
            // 清理所有上传的测试文件
            for (String filePath : uploadedFiles) {
                try {
                    gcsUtils.deleteFile(filePath);
                } catch (Exception e) {
                    logger.warn("Failed to delete file during cleanup: {}", filePath, e);
                }
            }

            // 清理测试目录
            try {
                gcsUtils.deleteFile(testPrefix + ".keep");
            } catch (Exception e) {
                logger.warn("Failed to delete directory marker during cleanup", e);
            }
        } finally {
            if (gcsUtils != null) {
                gcsUtils.close();
            }
        }
        logger.info("Multiple table test cleanup completed");
    }

    /** Copy data files to GCS for multiple table tests */
    @TestTemplate
    public void addTestFiles(TestContainer container) throws IOException, InterruptedException {
        // 验证 GCS 客户端是否正常初始化
        Assertions.assertNotNull(gcsUtils, "GCS utils should be initialized");

        logger.info("Uploading test files for multiple table tests...");

        try {
            // 上传 JSON 文件
            uploadTestFile("/json/e2e.json", "read/json/name=tyrantlucifer/hobby=coding/e2e.json");

            // 上传文本文件
            uploadTestFile("/text/e2e.txt", "read/text/name=tyrantlucifer/hobby=coding/e2e.txt");

            // 上传 Excel 文件
            uploadTestFile("/excel/e2e.xlsx", "read/excel/name=tyrantlucifer/hobby=coding/e2e.xlsx");

            // 上传 ORC 文件
            uploadTestFile("/orc/e2e.orc", "read/orc/name=tyrantlucifer/hobby=coding/e2e.orc");

            // 上传 Parquet 文件
            uploadTestFile("/parquet/e2e.parquet", "read/parquet/name=tyrantlucifer/hobby=coding/e2e.parquet");

            // 创建测试目录
            gcsUtils.createDir("tmp/fake_empty");
            uploadedFiles.add("tmp/fake_empty/.keep");

            logger.info("All test files for multiple table tests uploaded successfully to GCS");

        } catch (Exception e) {
            logger.error("Failed to upload test files for multiple table tests", e);
            throw new RuntimeException("GCS file upload for multiple table tests failed", e);
        }
    }

    private void uploadTestFile(String sourcePath, String targetPath) throws IOException {
        String fullTargetPath = testPrefix + targetPath;
        gcsUtils.uploadTestFiles(sourcePath, fullTargetPath, true);
        uploadedFiles.add(fullTargetPath);

        // 验证文件是否成功上传
        boolean exists = gcsUtils.fileExists(fullTargetPath);
        Assertions.assertTrue(exists, "Uploaded file should exist: " + fullTargetPath);
    }

    @TestTemplate
    public void testFakeToGcsFileInMultipleTableMode_text(TestContainer testContainer)
            throws IOException, InterruptedException {
        TestHelper helper = new TestHelper(testContainer);
        logger.info("Running multiple table text write test...");
        helper.execute("/text/fake_to_gcs_file_with_multiple_table.conf");
        logger.info("Multiple table text write test completed");
    }

    @TestTemplate
    public void testGcsFileReadAndWriteInMultipleTableMode_excel(TestContainer container)
            throws IOException, InterruptedException {
        TestHelper helper = new TestHelper(container);
        logger.info("Running multiple table Excel read/write test...");
        helper.execute("/excel/gcs_excel_to_assert_with_multipletable.conf");
        logger.info("Multiple table Excel read/write test completed");
    }

    @TestTemplate
    public void testGcsFileReadAndWriteInMultipleTableMode_json(TestContainer container)
            throws IOException, InterruptedException {
        TestHelper helper = new TestHelper(container);
        logger.info("Running multiple table JSON read/write test...");
        helper.execute("/json/gcs_file_json_to_assert_with_multipletable.conf");
        logger.info("Multiple table JSON read/write test completed");
    }

    @TestTemplate
    public void testGcsFileReadAndWriteInMultipleTableMode_orc(TestContainer container)
            throws IOException, InterruptedException {
        TestHelper helper = new TestHelper(container);
        logger.info("Running multiple table ORC read/write test...");
        helper.execute("/orc/gcs_file_orc_to_assert_with_multipletable.conf");
        logger.info("Multiple table ORC read/write test completed");
    }

    @TestTemplate
    public void testGcsFileReadAndWriteInMultipleTableMode_parquet(TestContainer container)
            throws IOException, InterruptedException {
        TestHelper helper = new TestHelper(container);
        logger.info("Running multiple table Parquet read/write test...");
        helper.execute("/parquet/gcs_file_parquet_to_assert_with_multipletable.conf");
        logger.info("Multiple table Parquet read/write test completed");
    }

    @TestTemplate
    public void testGcsFileReadAndWriteInMultipleTableMode_text(TestContainer container)
            throws IOException, InterruptedException {
        TestHelper helper = new TestHelper(container);
        logger.info("Running multiple table text read/write test...");
        helper.execute("/text/gcs_file_text_to_assert_with_multipletable.conf");
        logger.info("Multiple table text read/write test completed");
    }
}