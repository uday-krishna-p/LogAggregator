package com.projects.logaggregator.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntryDTO {
    @NotBlank
    private String timestamp;
    @NotBlank
    private String message;
    @NotBlank
    private String level;

    private String source = "unknown";

    private String traceId = "";
}
