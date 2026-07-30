# Override Load API (ECVS-1240)

Spring Boot microservice endpoint that loads the **AAI Exception** CSV from Azure Blob Storage into PostgreSQL table `landing.cis_exception`.

## What it does

1. Deletes existing rows from `landing.cis_exception`
2. Downloads the AAI Exception CSV from Blob storage (AKS workload identity / `DefaultAzureCredential`, or connection string for local)
3. Parses and inserts valid rows
4. Returns Automic-friendly reconciliation + error payload

## Endpoint

```http
POST /api/v1/override-load
Content-Type: application/json

{
  "blobName": "AAI Exception File 6-26-26.csv",
  "containerName": "aai-exception"
}
```

Body is optional; defaults come from configuration.

### Success / partial response

```json
{
  "job_name": "OVERRIDE_LOAD",
  "job_status": "SUCCESS",
  "http_status": 200,
  "message": "Override load completed",
  "blob_name": "AAI Exception File 6-26-26.csv",
  "started_at": "2026-06-26T12:00:00Z",
  "completed_at": "2026-06-26T12:00:05Z",
  "total_record_count": 30397,
  "success_count": 30397,
  "exception_count": 0,
  "deleted_count": 100,
  "error": []
}
```

`job_status` values: `SUCCESS`, `PARTIAL_SUCCESS`, `FAILED`.

### CSV → table mapping

| CSV header | Column |
|---|---|
| `AAI_COID` | `coid` |
| `AAI_CUST_NUM` | `customer_number` |
| `AAI_LINKED_PROD` | `linked_product_code` |
| `AAI_LINKED_ACCOUNT` | `linked_account_number` |
| `AAI_LINKED_SUBPC` | `linked_sub_product_code` |
| `AAI_LINKED_CUAC` | `linked_cuac_code` |
| `AAI_EXC_ACAC` | `exc_acac` |
| `AAI_STD_ACAC` | `std_acac` |
| `AAI_CREATE_DATE` | `create_date` |
| `AAI_CHANGE_DATE` | `change_date` |
| `AAI_DELETED_FLAG` | `deleted_flag` |

## Configuration

| Property / env | Purpose |
|---|---|
| `DB_URL` | JDBC URL for AAI Postgres |
| `DB_USERNAME` / `DB_PASSWORD` | DB credentials (or rely on workload identity proxy) |
| `AZURE_STORAGE_ACCOUNT` | Storage account name |
| `AZURE_STORAGE_CONTAINER` | Container (default `aai-exception`) |
| `AZURE_BLOB_NAME` | Default blob file name |
| `AZURE_STORAGE_CONNECTION_STRING` | Optional local/dev override |

Reference DDL: `src/main/resources/db/schema-landing-cis-exception.sql`

## Run

```bash
mvn spring-boot:run
# or
mvn test
```

Requires Java 21+.
