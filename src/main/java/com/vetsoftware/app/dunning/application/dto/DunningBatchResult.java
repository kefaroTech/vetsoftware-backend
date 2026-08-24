package com.vetsoftware.app.dunning.application.dto;

/**
 * Cursor estable del barrido de mora, sin tabla de leases ni estado adicional.
 */
public record DunningBatchResult(int processed, long lastId) {
}
