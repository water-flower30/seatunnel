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
import org.apache.seatunnel.e2e.common.util.ContainerUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

import io.airlift.compress.lzo.LzopCodec;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Disabled("Disabled because it needs user's personal GCS account to run this test")
public class GcsFileIT extends TestSuiteBase {

    private static final Logger logger = LoggerFactory.getLogger(GcsFileIT.class);

    public static final String GCS_CONNECTOR_DOWNLOAD =
            "https://repo1.maven.org/maven2/com/google/cloud/bigdataoss/gcs-connector/hadoop3-2.2.0/gcs-connector-hadoop3-2.2.0-shaded.jar";
    public static final String GOOGLE_CLOUD_STORAGE_DOWNLOAD =
            "https://repo1.maven.org/maven2/com/google/cloud/google-cloud-storage/2.5.0/google-cloud-storage-2.5.0.jar";

    // 从环境变量获取配置，提供默认值用于本地测试
    private final String projectId = System.getenv().getOrDefault("GCS_PROJECT_ID", "your-project-id");
    private final String bucket = System.getenv().getOrDefault("GCS_BUCKET", "your-test-bucket");
    private final String credentialsPath = System.getenv().getOrDefault("GOOGLE_APPLICATION_CREDENTIALS", "");

    private GcsUtils gcsUtils;
    private List<String> uploadedFiles = new ArrayList<>();
    private final String testPrefix = "test/seatunnel/e2e/" + System.currentTimeMillis() + "/";

    @TestContainerExtension
    private final ContainerExtendedFactory extendedFactory =
            container -> {
                logger.info("Setting up GCS dependencies in container...");

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

                logger.info("GCS dependencies setup completed successfully");
            };

    @BeforeEach
    void setUp() {
        logger.info("Initializing GCS utils for project: {}, bucket: {}", projectId, bucket);
        gcsUtils = new GcsUtils(projectId, bucket, credentialsPath);
    }

    @AfterEach
    void tearDown() {
        logger.info("Cleaning up test files from GCS...");
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
        logger.info("Cleanup completed");
    }

    /** Copy data files to GCS */
    @TestTemplate
    public void testGcsFileReadAndWrite(TestContainer container)
            throws IOException, InterruptedException {
        // 验证 GCS 客户端是否正常初始化
        Assertions.assertNotNull(gcsUtils, "GCS utils should be initialized");

        // Copy test files to GCS
        try {
            // 上传 JSON 文件
            uploadTestFile("/json/e2e.json", "read/json/name=tyrantlucifer/hobby=coding/e2e.json");

            // 转换并上传 LZO 格式文件
            Path jsonLzo = convertToLzoFile(ContainerUtil.getResourcesFile("/json/e2e.json"));
            uploadTestFile(jsonLzo.toString(), "read/lzo_json/e2e.json", false);

            // 上传文本文件
            uploadTestFile("/text/e2e.txt", "read/text/name=tyrantlucifer/hobby=coding/e2e.txt");
            uploadTestFile("/text/e2e_delimiter.txt", "read/text_delimiter/e2e.txt");
            uploadTestFile("/text/e2e_time_format.txt", "read/text_time_format/e2e.txt");
            uploadTestFile("text/e2e-text.zip", "read/zip/text/e2e-text.zip");

            // 转换并上传 LZO 文本文件
            Path txtLzo = convertToLzoFile(ContainerUtil.getResourcesFile("/text/e2e.txt"));
            uploadTestFile(txtLzo.toString(), "read/lzo_text/e2e.txt", false);

            // 上传 Excel 文件
            uploadTestFile("/excel/e2e.xlsx", "read/excel/name=tyrantlucifer/hobby=coding/e2e.xlsx");
            uploadTestFile("/excel/e2e.xlsx", "read/excel_filter/name=tyrantlucifer/hobby=coding/e2e_filter.xlsx");

            // 上传 ORC 文件
            uploadTestFile("/orc/e2e.orc", "read/orc/name=tyrantlucifer/hobby=coding/e2e.orc");

            // 上传 Parquet 文件
            uploadTestFile("/parquet/e2e.parquet", "read/parquet/name=tyrantlucifer/hobby=coding/e2e.parquet");

            // 创建测试目录
            gcsUtils.createDir("tmp/fake_empty");
            uploadedFiles.add("tmp/fake_empty/.keep");

            logger.info("All test files uploaded successfully to GCS");

        } catch (Exception e) {
            logger.error("Failed to upload test files to GCS", e);
            throw new RuntimeException("GCS file upload failed", e);
        }

        TestHelper helper = new TestHelper(container);

        // 执行测试配置
        executeTestConfigs(helper);
    }

    private void uploadTestFile(String sourcePath, String targetPath) throws IOException {
        uploadTestFile(sourcePath, targetPath, true);
    }

    private void uploadTestFile(String sourcePath, String targetPath, boolean isFindFromResource) throws IOException {
        String fullTargetPath = testPrefix + targetPath;
        gcsUtils.uploadTestFiles(sourcePath, fullTargetPath, isFindFromResource);
        uploadedFiles.add(fullTargetPath);

        // 验证文件是否成功上传
        boolean exists = gcsUtils.fileExists(fullTargetPath);
        Assertions.assertTrue(exists, "Uploaded file should exist: " + fullTargetPath);
    }

    private void executeTestConfigs(TestHelper helper) throws IOException, InterruptedException {
        // Excel 测试
        helper.execute("/excel/fake_to_gcs_excel.conf");
        helper.execute("/excel/gcs_excel_to_assert.conf");
        helper.execute("/excel/gcs_excel_projection_to_assert.conf");

        // 文本文件测试
        helper.execute("/text/fake_to_gcs_file_text.conf");
        helper.execute("/text/gcs_file_text_lzo_to_assert.conf");
        helper.execute("/text/gcs_file_delimiter_assert.conf");
        helper.execute("/text/gcs_file_time_format_assert.conf");

        // 跳过头部测试
        helper.execute("/text/gcs_file_text_skip_headers.conf");

        // 读取文本文件测试
        helper.execute("/text/gcs_file_text_to_assert.conf");
        helper.execute("/text/gcs_file_zip_text_to_assert.conf");
        helper.execute("/text/gcs_file_text_projection_to_assert.conf");

        // JSON 文件测试
        helper.execute("/json/fake_to_gcs_file_json.conf");
        helper.execute("/json/gcs_file_json_to_assert.conf");
        helper.execute("/json/gcs_file_json_lzo_to_console.conf");

        // ORC 文件测试
        helper.execute("/orc/fake_to_gcs_file_orc.conf");
        helper.execute("/orc/gcs_file_orc_to_assert.conf");
        helper.execute("/orc/gcs_file_orc_projection_to_assert.conf");

        // Parquet 文件测试
        helper.execute("/parquet/fake_to_gcs_file_parquet.conf");
        helper.execute("/parquet/gcs_file_parquet_to_assert.conf");
        helper.execute("/parquet/gcs_file_parquet_projection_to_assert.conf");

        // 过滤测试
        helper.execute("/excel/gcs_filter_excel_to_assert.conf");

        // 空目录测试
        helper.execute("/json/gcs_file_to_console.conf");
        helper.execute("/parquet/gcs_file_to_console.conf");
    }

    private Path convertToLzoFile(File file) throws IOException {
        logger.info("Converting file to LZO format: {}", file.getAbsolutePath());
        LzopCodec lzo = new LzopCodec();
        Path path = Paths.get(file.getAbsolutePath() + ".lzo");
        try (OutputStream outputStream = lzo.createOutputStream(Files.newOutputStream(path))) {
            outputStream.write(Files.readAllBytes(file.toPath()));
        }
        logger.info("LZO conversion completed: {}", path);
        return path;
    }
}