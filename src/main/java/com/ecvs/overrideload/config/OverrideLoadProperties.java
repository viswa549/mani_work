package com.ecvs.overrideload.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "override-load")
public class OverrideLoadProperties {

    /** JDBC batch chunk size (500–2000 recommended for ~50k rows). */
    private int batchSize = 1000;

    /** Cap row-level errors returned in additionalDetails. */
    private int maxErrorDetails = 50;

    private String jobName = "OVERRIDE_LOAD";
}
