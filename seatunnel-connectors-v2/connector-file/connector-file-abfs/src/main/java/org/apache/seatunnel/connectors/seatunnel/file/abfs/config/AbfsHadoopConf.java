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

package org.apache.seatunnel.connectors.seatunnel.file.abfs.config;

import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.connectors.seatunnel.file.config.HadoopConf;

import java.util.HashMap;

public class AbfsHadoopConf extends HadoopConf {
    private static final String HDFS_IMPL = "org.apache.hadoop.fs.azurebfs.AzureBlobFileSystem";
    private static final String SCHEMA = "abfs";

    @Override
    public String getFsHdfsImpl() {
        return HDFS_IMPL;
    }

    @Override
    public String getSchema() {
        return SCHEMA;
    }

    public AbfsHadoopConf(String hdfsNameKey) {
        super(hdfsNameKey);
    }

    public static HadoopConf buildWithConfig(ReadonlyConfig config) {
        String accountName = config.get(AbfsFileBaseOptions.ACCOUNT_NAME);
        String container = config.get(AbfsFileBaseOptions.CONTAINER);
        String endpoint = config.get(AbfsFileBaseOptions.ENDPOINT);

        // Build the ABFS path: container@account.endpoint
        String hdfsNameKey = container + "@" + accountName + "." + endpoint;

        HadoopConf hadoopConf = new AbfsHadoopConf(hdfsNameKey);
        HashMap<String, String> abfsOptions = new HashMap<>();

        // Azure storage account key configuration
        String accountKeyProperty = "fs.azure.account.key." + accountName + "." + endpoint;
        abfsOptions.put(accountKeyProperty, config.get(AbfsFileBaseOptions.ACCOUNT_KEY));

        // Enable secure transfer
        abfsOptions.put("fs.azure.secure.mode", "true");

        hadoopConf.setExtraOptions(abfsOptions);
        return hadoopConf;
    }
}
