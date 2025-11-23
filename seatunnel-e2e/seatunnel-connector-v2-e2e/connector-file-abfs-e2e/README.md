# ABFS File Connector E2E Tests

This module contains end-to-end tests for the ABFS (Azure Blob File System) file connector.

## Configuration

Before running tests, update the following configuration in test files:
- `account_name`: Your Azure storage account name
- `account_key`: Your Azure storage account key
- `container`: Your Azure storage container name

Also update these values in `AbfsUtils.java`.

## Test Structure

### Created Test Configuration Files

#### Text Format
- `/text/fake_to_abfs_file_text.conf` - Write text files to ABFS
- `/text/abfs_file_text_to_assert.conf` - Read text files from ABFS

#### JSON Format
- `/json/fake_to_abfs_file_json.conf` - Write JSON files to ABFS
- `/json/abfs_file_json_to_assert.conf` - Read JSON files from ABFS

#### ORC Format
- `/orc/fake_to_abfs_file_orc.conf` - Write ORC files to ABFS
- `/orc/abfs_file_orc_to_assert.conf` - Read ORC files from ABFS

#### Parquet Format
- `/parquet/fake_to_abfs_file_parquet.conf` - Write Parquet files to ABFS
- `/parquet/abfs_file_parquet_to_assert.conf` - Read Parquet files from ABFS

### Additional Test Files Needed

The following test configuration files are referenced in `AbfsFileIT.java` but need to be created:

#### Text Format
- `/text/abfs_file_text_lzo_to_assert.conf` - LZO compressed text
- `/text/abfs_file_delimiter_assert.conf` - Custom delimiter
- `/text/abfs_file_time_format_assert.conf` - Time format test
- `/text/abfs_file_text_skip_headers.conf` - Skip header rows
- `/text/abfs_file_zip_text_to_assert.conf` - ZIP compressed text
- `/text/abfs_file_text_projection_to_assert.conf` - Column projection

#### JSON Format
- `/json/abfs_file_json_lzo_to_console.conf` - LZO compressed JSON
- `/json/abfs_file_to_console.conf` - Empty directory test

#### ORC Format
- `/orc/abfs_file_orc_projection_to_assert.conf` - Column projection

#### Parquet Format
- `/parquet/abfs_file_parquet_projection_to_assert.conf` - Column projection
- `/parquet/abfs_file_to_console.conf` - Empty directory test

#### Excel Format
- `/excel/fake_to_abfs_excel.conf` - Write Excel files
- `/excel/abfs_excel_to_assert.conf` - Read Excel files
- `/excel/abfs_excel_projection_to_assert.conf` - Column projection
- `/excel/abfs_filter_excel_to_assert.conf` - File filtering

## Running Tests

Tests are disabled by default and require a personal Azure storage account. To run:

1. Update configuration in test files with your Azure credentials
2. Remove the `@Disabled` annotation from `AbfsFileIT.java`
3. Run: `mvn test -pl seatunnel-e2e/seatunnel-connector-v2-e2e/connector-file-abfs-e2e`

## Test Data Files

The tests use the following data files from the connector-file-base test resources:
- `/json/e2e.json`
- `/text/e2e.txt`
- `/text/e2e_delimiter.txt`
- `/text/e2e_time_format.txt`
- `/text/e2e-text.zip`
- `/excel/e2e.xlsx`
- `/orc/e2e.orc`
- `/parquet/e2e.parquet`

These files should be available in the parent connector-file-base-e2e module or need to be created.
