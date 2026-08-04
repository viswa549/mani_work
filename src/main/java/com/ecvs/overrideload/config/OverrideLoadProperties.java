package com.ecvs.overrideload.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "override-load")
public class OverrideLoadProperties {

    private int batchSize = 500;
    private String jobName = "OVERRIDE_LOAD";
}
