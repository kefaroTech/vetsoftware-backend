package com.vetsoftware.app.clinicalhistory.infrastructure.pdf;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalHistoryReportModel;
import com.vetsoftware.app.clinicalhistory.application.dto.ReportClinicalEvent;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalHistoryPdfPort;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ClinicalHistoryPdfAdapter implements ClinicalHistoryPdfPort {

    private static final String[] SPANISH_MONTHS = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private static final Map<ClinicalEventType, String> TYPE_LABELS = Map.of(
            ClinicalEventType.CONSULTATION, "Consulta",
            ClinicalEventType.SURGERY, "Cirugía",
            ClinicalEventType.VACCINATION, "Vacunación",
            ClinicalEventType.DEWORMING, "Desparasitación",
            ClinicalEventType.HOSPITALIZATION, "Hospitalización",
            ClinicalEventType.LABORATORY_TEST, "Laboratorio",
            ClinicalEventType.DIAGNOSTIC_IMAGING, "Imagen diagnóstica",
            ClinicalEventType.PRESCRIPTION, "Prescripción",
            ClinicalEventType.SPA, "Estética"
    );

    private final HtmlPdfRenderer renderer;

    public ClinicalHistoryPdfAdapter(HtmlPdfRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public byte[] render(ClinicalHistoryReportModel model) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("animal", model.animal());
        ctx.put("from", model.from());
        ctx.put("to", model.to());
        ctx.put("typeFilters", model.typeFilters());
        ctx.put("alerts", model.alerts());
        ctx.put("hasAlerts", model.alerts() != null && !model.alerts().isEmpty());
        ctx.put("problems", model.problems());
        ctx.put("hasProblems", model.problems() != null && !model.problems().isEmpty());
        ctx.put("typeLabels", labelsByName());
        ctx.put("typeCounts", countByType(model.events()));
        ctx.put("monthGroups", groupByMonth(model.events()));
        ctx.put("hasEvents", !model.events().isEmpty());
        ctx.put("eventCount", model.events().size());
        ctx.put("generatedAt", model.generatedAt());
        return renderer.render("clinical-history", ctx);
    }

    private Map<String, String> labelsByName() {
        Map<String, String> byName = new HashMap<>();
        TYPE_LABELS.forEach((type, label) -> byName.put(type.name(), label));
        return byName;
    }

    /** Conteo de eventos por tipo, en el orden del enum, para el resumen del encabezado. */
    private List<TypeCount> countByType(List<ReportClinicalEvent> events) {
        Map<ClinicalEventType, Long> counts = new LinkedHashMap<>();
        for (ClinicalEventType type : ClinicalEventType.values()) {
            long n = events.stream().filter(e -> e.eventType() == type).count();
            if (n > 0) {
                counts.put(type, n);
            }
        }
        return counts.entrySet().stream()
                .map(e -> new TypeCount(e.getKey().name(), TYPE_LABELS.get(e.getKey()), e.getValue()))
                .toList();
    }

    private List<ClinicalHistoryMonthGroup> groupByMonth(List<ReportClinicalEvent> events) {
        LinkedHashMap<String, List<ReportClinicalEvent>> map = new LinkedHashMap<>();
        for (ReportClinicalEvent ev : events) {
            String key = ev.eventDate().getYear() + "-"
                    + String.format("%02d", ev.eventDate().getMonthValue());
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(ev);
        }
        return map.entrySet().stream()
                .map(e -> new ClinicalHistoryMonthGroup(
                        e.getKey(), monthLabel(e.getKey()), e.getValue()))
                .toList();
    }

    private String monthLabel(String yyyymm) {
        String[] parts = yyyymm.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        return SPANISH_MONTHS[month - 1] + " " + year;
    }

    /** View-model para el chip de resumen por tipo en el encabezado del reporte. */
    public record TypeCount(String type, String label, long count) {
    }
}
