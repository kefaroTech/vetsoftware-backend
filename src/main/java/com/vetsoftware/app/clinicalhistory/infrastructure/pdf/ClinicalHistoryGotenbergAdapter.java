package com.vetsoftware.app.clinicalhistory.infrastructure.pdf;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalHistoryReportModel;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalHistoryPdfPort;
import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import com.vetsoftware.app.infrastructure.pdf.PdfOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ClinicalHistoryGotenbergAdapter implements ClinicalHistoryPdfPort {

    private static final String[] SPANISH_MONTHS = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private final HtmlPdfRenderer renderer;

    public ClinicalHistoryGotenbergAdapter(HtmlPdfRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public byte[] render(ClinicalHistoryReportModel model) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("animal", model.animal());
        ctx.put("from", model.from());
        ctx.put("to", model.to());
        ctx.put("typeFilters", model.typeFilters());
        ctx.put("monthGroups", groupByMonth(model.events()));
        ctx.put("hasEvents", !model.events().isEmpty());
        ctx.put("eventCount", model.events().size());
        ctx.put("generatedAt", model.generatedAt());
        return renderer.render("clinical-history", ctx, PdfOptions.defaults());
    }

    private List<ClinicalHistoryMonthGroup> groupByMonth(List<ClinicalEventDto> events) {
        LinkedHashMap<String, List<ClinicalEventDto>> map = new LinkedHashMap<>();
        for (ClinicalEventDto ev : events) {
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
}
