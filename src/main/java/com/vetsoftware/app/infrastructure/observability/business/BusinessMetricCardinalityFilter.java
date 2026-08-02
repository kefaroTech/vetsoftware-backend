package com.vetsoftware.app.infrastructure.observability.business;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import java.util.Map;
import java.util.Set;

/**
 * Lista blanca para las dimensiones de negocio. Cualquier clave o valor nuevo debe revisarse
 * deliberadamente antes de aumentar la cantidad de series en Prometheus.
 */
public final class BusinessMetricCardinalityFilter implements MeterFilter {

  private static final Set<String> COMMON_TAGS =
      Set.of("application", "environment", "instance", "region", "service");

  private static final Map<String, Set<String>> ALLOWED_VALUES =
      Map.ofEntries(
          Map.entry(
              "result",
              Set.of(
                  "completed",
                  "rejected",
                  "cancelled",
                  "error",
                  "validated",
                  "contingency",
                  "pending",
                  "success",
                  "insufficient_stock",
                  "duplicate_ignored",
                  "validation_error",
                  "difference")),
          Map.entry("channel", Set.of("pos", "open_account", "staff", "public")),
          Map.entry(
              "document.type",
              Set.of("fe_venta", "doc_equiv_pos", "nota_credito", "nota_debito", "unknown")),
          Map.entry("origin", Set.of("initial", "retry", "webhook", "reconciliation")),
          Map.entry(
              "status",
              Set.of(
                  "pending",
                  "contingency",
                  "requested",
                  "confirmed",
                  "arrived",
                  "in_progress",
                  "completed",
                  "no_show",
                  "cancelled")),
          Map.entry(
              "age",
              Set.of(
                  "lt_15m", "from_15m_to_1h", "gt_1h", "expired", "from_0_to_7d", "from_8_to_30d")),
          Map.entry(
              "movement.type",
              Set.of(
                  "purchase",
                  "sale",
                  "clinical_use",
                  "adjustment_in",
                  "adjustment_out",
                  "transfer_in",
                  "transfer_out",
                  "void_in",
                  "void_out")),
          Map.entry("event", Set.of("opened", "closed")),
          Map.entry("direction", Set.of("shortage", "surplus", "balanced")));

  @Override
  public MeterFilterReply accept(Meter.Id id) {
    if (!id.getName().startsWith(BusinessMetricNames.PREFIX)) {
      return MeterFilterReply.NEUTRAL;
    }
    for (io.micrometer.core.instrument.Tag tag : id.getTags()) {
      if (COMMON_TAGS.contains(tag.getKey())) {
        continue;
      }
      Set<String> values = ALLOWED_VALUES.get(tag.getKey());
      if (values == null || !values.contains(tag.getValue())) {
        return MeterFilterReply.DENY;
      }
    }
    return MeterFilterReply.NEUTRAL;
  }
}
