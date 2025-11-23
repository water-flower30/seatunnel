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

package org.apache.seatunnel.connectors.seatunnel.file.gcs.config;

import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.connectors.seatunnel.file.config.HadoopConf;

import java.util.HashMap;

public class GoogleCloudStorageHadoopConf extends HadoopConf {
    private static final String HDFS_IMPL = "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem";
    private static final String SCHEMA = "gs";

    @Override
    public String getFsHdfsImpl() {
        return HDFS_IMPL;
    }

    @Override
    public String getSchema() {
        return SCHEMA;
    }

    public GoogleCloudStorageHadoopConf(String hdfsNameKey) {
        super(hdfsNameKey);
    }

    public static HadoopConf buildWithConfig(ReadonlyConfig config) {
        HadoopConf hadoopConf =
                new GoogleCloudStorageHadoopConf(config.get(GoogleCloudStorageBaseOptions.BUCKET));
        HashMap<String, String> gcsOptions = new HashMap<>();

        if (config.getOptional(GoogleCloudStorageBaseOptions.PROJECT_ID).isPresent()) {
            gcsOptions.put(
                    "fs.gs.project.id", config.get(GoogleCloudStorageBaseOptions.PROJECT_ID));
        }

        if (config.getOptional(GoogleCloudStorageBaseOptions.CREDENTIALS_PATH).isPresent()) {
            gcsOptions.put(
                    "fs.gs.auth.service.account.json.keyfile",
                    config.get(GoogleCloudStorageBaseOptions.CREDENTIALS_PATH));
            gcsOptions.put("fs.gs.auth.service.account.enable", "true");
        }

        hadoopConf.setExtraOptions(gcsOptions);
        return hadoopConf;
    }
}
