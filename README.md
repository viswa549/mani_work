# Override Load API (ECVS-1240)

Spring Boot microservice that loads the **AAI Exception** CSV from Azure Blob Storage into PostgreSQL `landing.cis_exception`.

**Hybrid persistence:** JPA is retained for other service requirements; the bulk LOAD path uses **JDBC batch inserts** (not `saveAll`) for ~50k+ rows.

## What it does

1. Accepts `batchId` from `/api/v1/override-extract`
2. Streams `{batchId}.csv` from Azure Blob
3. Truncates / clears `landing.cis_exception`
4. JDBC batch-inserts valid rows
5. Returns sync `processStats` reconciliation payload

## Endpoint

```http
POST /api/v1/override-load
Content-Type: application/json

{
  "batchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

### Success / partial response (HTTP 200)

```json
{
  "batchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "currentStatus": "SUCCESS",
  "processStats": [
    {
      "stage": "LOAD",
      "status": "SUCCESS",
      "startTime": "2026-06-30T12:00:00Z",
      "endTime": "2026-06-30T12:00:05Z",
      "statistics": {
        "totalEntitlementsProcessed": 30397,
        "totalEntitlementsSuccessful": 30397,
        "newEntitlements": 30397,
        "modifiedEntitlements": null,
        "deletedEntitlements": 100,
        "totalEntitlementsFailed": 0
      }
    }
  ]
}
```

`currentStatus`: `SUCCESS` | `PARTIAL` | `FAILED` | `IN_PROGRESS`  
`processStats[].status`: `SUCCESS` | `PARTIAL_SUCCESS` | `FAILED` | `IN_PROGRESS`  
`stage`: `LOAD` (this API)

### Error response (4xx/5xx)

```json
{
  "timestamp": "2026-06-30T12:00:01Z",
  "error": "Azure Blob connectivity failure",
  "status": 503,
  "path": "/api/v1/override-load",
  "additionalDetails": ["batchId=...", "errorCode=OL-BLOB-001"],
  "processStats": []
}
```

### CSV → table mapping

| CSV header | Column |
|---|---|
| (request) `batchId` | `batch_id` |
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
| `DB_USERNAME` / `DB_PASSWORD` | DB credentials |
| `AZURE_STORAGE_ACCOUNT` | Storage account name |
| `AZURE_STORAGE_CONTAINER` | Container (default `aai-exception`) |
| `AZURE_BLOB_NAME_PATTERN` | Blob pattern (default `%s.csv`, `%s` = batchId) |
| `AZURE_STORAGE_CONNECTION_STRING` | Optional local/dev override |
| `override-load.batch-size` | JDBC batch size (default `1000`) |

Reference DDL: `src/main/resources/db/schema-landing-cis-exception.sql`

## Architecture notes

- **Keep JPA** (`CisException` + `CisExceptionRepository`) for lookups / other features
- **Use JDBC** (`CisExceptionJdbcRepository`) for TRUNCATE + `batchUpdate` on LOAD
- Same Spring `@Transactional` / DataSource — no second database config needed

## Run

```bash
mvn spring-boot:run
# or
mvn test
```

Requires Java 21+.
