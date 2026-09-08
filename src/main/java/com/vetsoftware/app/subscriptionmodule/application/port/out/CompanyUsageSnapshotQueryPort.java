package com.vetsoftware.app.subscriptionmodule.application.port.out;

/**
 * Cuánto lleva usado la empresa de un eje, leído de la tabla que de verdad lo
 * cuenta: {@code CUMULATIVE} y {@code FLOW} en {@code company_usage_events},
 * {@code SERVICE_ITEM} en {@code services}, {@code USER}/{@code BRANCH} en
 * {@code company_capacities}.
 */
public interface CompanyUsageSnapshotQueryPort {

    int countCumulative(Long companyId, String dimensionCode);

    int countCurrentMonth(Long companyId, String dimensionCode);

    int countActiveServices(Long companyId);

    java.util.Optional<CapacitySnapshot> findCapacity(Long companyId, String dimensionCode);

    record CapacitySnapshot(int limitQuantity, int usedQuantity) {
    }
}
