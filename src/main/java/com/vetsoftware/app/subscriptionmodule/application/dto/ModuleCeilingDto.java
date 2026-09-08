package com.vetsoftware.app.subscriptionmodule.application.dto;

public record ModuleCeilingDto(String dimensionCode, String measureKind, int used, int limit,
        int warnThreshold, String enforcement) {
}
