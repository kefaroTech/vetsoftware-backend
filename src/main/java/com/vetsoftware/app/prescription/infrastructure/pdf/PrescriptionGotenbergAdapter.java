package com.vetsoftware.app.prescription.infrastructure.pdf;

import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import com.vetsoftware.app.infrastructure.pdf.PdfOptions;
import com.vetsoftware.app.prescription.application.dto.PrescriptionReportModel;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionPdfPort;
import com.vetsoftware.app.prescription.domain.MedicamentRef;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionGotenbergAdapter implements PrescriptionPdfPort {

    private final HtmlPdfRenderer renderer;

    public PrescriptionGotenbergAdapter(HtmlPdfRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public byte[] render(PrescriptionReportModel model) {
        List<MedicamentLine> lines = new ArrayList<>();
        int i = 1;
        for (MedicamentRef m : model.medicaments()) {
            lines.add(new MedicamentLine(
                    i++,
                    m.name(),
                    m.presentation(),
                    formatQuantity(m.quantity()),
                    m.posology(),
                    m.observation()));
        }

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("s", model.signalment());
        ctx.put("prescriberName", model.prescriberName());
        ctx.put("date", model.date());
        ctx.put("diagnosis", model.diagnosis());
        ctx.put("observations", model.observations());
        ctx.put("medicaments", lines);
        ctx.put("hasMedicaments", !lines.isEmpty());
        ctx.put("generatedAt", model.generatedAt());
        return renderer.render("prescription", ctx, PdfOptions.defaults());
    }

    private static String formatQuantity(Double quantity) {
        if (quantity == null) return "";
        return BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString();
    }

    /** Fila de medicamento ya formateada para la plantilla. */
    public record MedicamentLine(
            int index,
            String name,
            String presentation,
            String quantity,
            String posology,
            String observation
    ) {
    }
}
