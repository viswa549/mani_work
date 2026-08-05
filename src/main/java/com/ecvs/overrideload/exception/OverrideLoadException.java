package com.ecvs.overrideload.exception;

import com.ecvs.overrideload.dto.OverrideLoadResponse.ProcessStat;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class OverrideLoadException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;
    private final List<String> additionalDetails;
    private final List<ProcessStat> processStats;

    public OverrideLoadException(String errorCode, String message, int httpStatus) {
        this(errorCode, message, httpStatus, List.of(), List.of(), null);
    }

    public OverrideLoadException(String errorCode, String message, int httpStatus, Throwable cause) {
        this(errorCode, message, httpStatus, List.of(), List.of(), cause);
    }

    public OverrideLoadException(
            String errorCode,
            String message,
            int httpStatus,
            List<String> additionalDetails,
            List<ProcessStat> processStats,
            Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.additionalDetails = additionalDetails != null ? additionalDetails : new ArrayList<>();
        this.processStats = processStats != null ? processStats : new ArrayList<>();
    }
}
