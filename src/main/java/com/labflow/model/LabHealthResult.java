package com.labflow.model;

import java.util.List;

public record LabHealthResult(
        int score,
        String level,
        List<String> reasons,
        List<String> warnings
) {
}
