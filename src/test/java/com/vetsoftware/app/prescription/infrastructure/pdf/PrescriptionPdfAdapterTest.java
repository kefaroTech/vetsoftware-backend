package com.vetsoftware.app.prescription.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import com.vetsoftware.app.prescription.application.dto.PrescriptionReportModel;
import com.vetsoftware.app.prescription.application.dto.PrescriptionSignalment;
import com.vetsoftware.app.prescription.domain.MedicamentRef;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionPdfAdapter")
class PrescriptionPdfAdapterTest {

    private static final byte[] PDF_BYTES = {9, 9, 9};

    @Mock
    private HtmlPdfRenderer renderer;

    @InjectMocks
    private PrescriptionPdfAdapter adapter;

    @Captor
    private ArgumentCaptor<Map<String, Object>> ctxCaptor;

    private static PrescriptionSignalment signalment() {
        return new PrescriptionSignalment("Veterinaria Test", "900123456", "Calle 1", "3000000000",
                "Medellin", "Firulais", "A-001", "Perro", "Labrador", "Macho", "Negro", "3 años",
                "12 kg", "Juan Perez", "123456", "3111111111", "juan@test.local", "Calle 2");
    }

    @Nested
    @DisplayName("render")
    class Render {

        @Test
        @DisplayName("delega en el renderer con la plantilla 'prescription' y devuelve sus bytes")
        void delega_con_la_plantilla_prescription() {
            when(renderer.render(eq("prescription"), anyMap())).thenReturn(PDF_BYTES);
            PrescriptionReportModel model = new PrescriptionReportModel(signalment(), "Dra. Ana",
                    LocalDate.of(2026, 1, 10), "Otitis", "Control en 7 dias", List.of(),
                    LocalDateTime.of(2026, 1, 10, 9, 0));

            byte[] result = adapter.render(model);

            assertThat(result).isEqualTo(PDF_BYTES);
        }

        @Test
        @DisplayName("sin medicamentos hasMedicaments es false y la lista queda vacia")
        void sin_medicamentos_has_medicaments_false() {
            when(renderer.render(eq("prescription"), anyMap())).thenReturn(PDF_BYTES);
            PrescriptionReportModel model = new PrescriptionReportModel(signalment(), null,
                    LocalDate.of(2026, 1, 10), "Otitis", "Control en 7 dias", List.of(),
                    LocalDateTime.of(2026, 1, 10, 9, 0));

            adapter.render(model);

            verify(renderer).render(eq("prescription"), ctxCaptor.capture());
            Map<String, Object> ctx = ctxCaptor.getValue();
            assertThat(ctx.get("hasMedicaments")).isEqualTo(false);
            assertThat((List<?>) ctx.get("medicaments")).isEmpty();
        }

        @Test
        @DisplayName("cada medicamento se numera desde 1 y conserva sus datos")
        void cada_medicamento_se_numera_desde_uno() {
            when(renderer.render(eq("prescription"), anyMap())).thenReturn(PDF_BYTES);
            MedicamentRef primero = new MedicamentRef(1L, "Amoxicilina", "Tableta", 2.0,
                    "Cada 12 horas", "Con alimento");
            MedicamentRef segundo = new MedicamentRef(2L, "Ivermectina", "Solucion", 1.5,
                    "Cada 24 horas", null);
            PrescriptionReportModel model = new PrescriptionReportModel(signalment(), "Dra. Ana",
                    LocalDate.of(2026, 1, 10), "Otitis", "Control en 7 dias",
                    List.of(primero, segundo), LocalDateTime.of(2026, 1, 10, 9, 0));

            adapter.render(model);

            verify(renderer).render(eq("prescription"), ctxCaptor.capture());
            @SuppressWarnings("unchecked")
            List<PrescriptionPdfAdapter.MedicamentLine> lines = (List<PrescriptionPdfAdapter.MedicamentLine>) ctxCaptor
                    .getValue().get("medicaments");
            assertThat(lines).hasSize(2);
            assertThat(lines.get(0).index()).isEqualTo(1);
            assertThat(lines.get(0).name()).isEqualTo("Amoxicilina");
            assertThat(lines.get(0).quantity()).isEqualTo("2");
            assertThat(lines.get(1).index()).isEqualTo(2);
            assertThat(lines.get(1).observation()).isNull();
            assertThat(ctxCaptor.getValue().get("hasMedicaments")).isEqualTo(true);
        }

        @Test
        @DisplayName("una cantidad cero se formatea sin decimales de relleno")
        void cantidad_cero_se_formatea_sin_decimales() {
            when(renderer.render(eq("prescription"), anyMap())).thenReturn(PDF_BYTES);
            MedicamentRef sinCantidad = new MedicamentRef(1L, "Amoxicilina", "Tableta", 0.0,
                    "Cada 12 horas", null);
            PrescriptionReportModel model = new PrescriptionReportModel(signalment(), null,
                    LocalDate.of(2026, 1, 10), null, null, List.of(sinCantidad),
                    LocalDateTime.of(2026, 1, 10, 9, 0));

            adapter.render(model);

            verify(renderer).render(eq("prescription"), ctxCaptor.capture());
            @SuppressWarnings("unchecked")
            List<PrescriptionPdfAdapter.MedicamentLine> lines = (List<PrescriptionPdfAdapter.MedicamentLine>) ctxCaptor
                    .getValue().get("medicaments");
            assertThat(lines.get(0).quantity()).isEqualTo("0");
        }
    }
}
