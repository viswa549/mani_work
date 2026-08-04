package com.ecvs.overrideload.exception;

/**
 * Stable error codes for Automic / ops mapping (ECVS-1240).
 */
public final class ErrorCodes {

    public static final String BLOB_CONNECTIVITY = "OL-BLOB-001";
    public static final String BLOB_NOT_FOUND = "OL-BLOB-002";
    public static final String BLOB_READ_FAILURE = "OL-BLOB-003";
    public static final String CSV_PARSE_FAILURE = "OL-CSV-001";
    public static final String CSV_ROW_INVALID = "OL-CSV-002";
    public static final String DB_CONNECTIVITY = "OL-DB-001";
    public static final String DB_DELETE_FAILURE = "OL-DB-002";
    public static final String DB_INSERT_FAILURE = "OL-DB-003";
    public static final String UNEXPECTED = "OL-SYS-001";

    private ErrorCodes() {
    }
}
