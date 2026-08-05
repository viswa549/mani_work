package com.ecvs.overrideload.service;

import com.ecvs.overrideload.entity.CisException;
import com.ecvs.overrideload.exception.ErrorCodes;
import lombok.Getter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Parses AAI Exception File CSV headers into CisException entities.
 */
@Component
public class AaiExceptionCsvParser {

    public static final String COL_COID = "AAI_COID";
    public static final String COL_CUST_NUM = "AAI_CUST_NUM";
    public static final String COL_LINKED_PROD = "AAI_LINKED_PROD";
    public static final String COL_LINKED_ACCOUNT = "AAI_LINKED_ACCOUNT";
    public static final String COL_LINKED_SUBPC = "AAI_LINKED_SUBPC";
    public static final String COL_LINKED_CUAC = "AAI_LINKED_CUAC";
    public static final String COL_EXC_ACAC = "AAI_EXC_ACAC";
    public static final String COL_STD_ACAC = "AAI_STD_ACAC";
    public static final String COL_CREATE_DATE = "AAI_CREATE_DATE";
    public static final String COL_CHANGE_DATE = "AAI_CHANGE_DATE";
    public static final String COL_DELETED_FLAG = "AAI_DELETED_FLAG";

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    public ParseResult parse(InputStream inputStream, UUID batchId) throws IOException {
        List<CisException> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int total = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .setIgnoreHeaderCase(true)
                .build();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {

            for (CSVRecord csvRecord : parser) {
                total++;
                long rowNumber = csvRecord.getRecordNumber() + 1; // header is row 1
                try {
                    records.add(mapRow(csvRecord, batchId));
                } catch (IllegalArgumentException ex) {
                    errors.add("Row %d [%s]: %s".formatted(
                            rowNumber, ErrorCodes.CSV_ROW_INVALID, ex.getMessage()));
                }
            }
        }

        return new ParseResult(total, records, errors);
    }

    private CisException mapRow(CSVRecord row, UUID batchId) {
        return CisException.builder()
                .batchId(batchId)
                .coid(parseShort(required(row, COL_COID), COL_COID))
                .customerNumber(parseLong(required(row, COL_CUST_NUM), COL_CUST_NUM))
                .linkedProductCode(trimToNull(optional(row, COL_LINKED_PROD)))
                .linkedAccountNumber(parseLong(optional(row, COL_LINKED_ACCOUNT), COL_LINKED_ACCOUNT))
                .linkedSubProductCode(trimToNull(firstPresent(row, COL_LINKED_SUBPC, "AAI_LINKED_SUB")))
                .linkedCuacCode(trimToNull(optional(row, COL_LINKED_CUAC)))
                .excAcac(trimToNull(optional(row, COL_EXC_ACAC)))
                .stdAcac(trimToNull(firstPresent(row, COL_STD_ACAC, "AAI_STD_A")))
                .createDate(parseDate(optional(row, COL_CREATE_DATE), COL_CREATE_DATE))
                .changeDate(parseDate(optional(row, COL_CHANGE_DATE), COL_CHANGE_DATE))
                .deletedFlag(normalizeFlag(optional(row, COL_DELETED_FLAG)))
                .build();
    }

    private String required(CSVRecord row, String column) {
        String value = optional(row, column);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Missing required column " + column);
        }
        return value.trim();
    }

    private String optional(CSVRecord row, String column) {
        if (!row.isMapped(column)) {
            return null;
        }
        String value = row.get(column);
        return value == null ? null : value.trim();
    }

    private String firstPresent(CSVRecord row, String primary, String alternate) {
        String value = optional(row, primary);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return optional(row, alternate);
    }

    private Short parseShort(String value, String column) {
        try {
            return Short.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid %s value '%s'".formatted(column, value));
        }
    }

    private Long parseLong(String value, String column) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid %s value '%s'".formatted(column, value));
        }
    }

    private LocalDate parseDate(String value, String column) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw new IllegalArgumentException("Invalid %s date '%s'".formatted(column, value));
    }

    private String normalizeFlag(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim().toUpperCase(Locale.ROOT);
        if (trimmed.length() > 1) {
            return trimmed.substring(0, 1);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    @Getter
    public static class ParseResult {
        private final int totalRecordCount;
        private final List<CisException> validRecords;
        private final List<String> errors;

        public ParseResult(int totalRecordCount, List<CisException> validRecords, List<String> errors) {
            this.totalRecordCount = totalRecordCount;
            this.validRecords = validRecords;
            this.errors = errors;
        }
    }
}
