package com.vetsoftware.app.inventory.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import com.vetsoftware.app.inventory.application.dto.KardexReport;
import com.vetsoftware.app.inventory.application.dto.PurchasesReport;
import java.math.BigDecimal;
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

/**
 * El adaptador solo traduce el reporte de aplicación a la plantilla Thymeleaf
 * correcta; el motor de render (HtmlPdfRenderer) es infraestructura compartida
 * y se mockea aquí.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryReportPdfAdapter")
class InventoryReportPdfAdapterTest {

    private static final byte[] PDF_BYTES = {1, 2, 3};
    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 1, 31);

    @Mock
    private HtmlPdfRenderer renderer;

    @InjectMocks
    private InventoryReportPdfAdapter adapter;

    @Captor
    private ArgumentCaptor<Map<String, Object>> ctxCaptor;

    private static KardexReport kardex() {
        return new KardexReport("Amoxicilina 500mg", "SKU-100", "Sede Centro", DESDE, HASTA,
                LocalDateTime.of(2026, 2, 1, 8, 0), 20, 26, List.of());
    }

    private static PurchasesReport compras() {
        return new PurchasesReport("Sede Centro", DESDE, HASTA, LocalDateTime.of(2026, 2, 1, 8, 0),
                List.of(), 0, BigDecimal.ZERO);
    }

    @Nested
    @DisplayName("renderKardex")
    class RenderKardex {

        @Test
        @DisplayName("delega en el renderer con la plantilla 'inventory-kardex' y devuelve sus bytes")
        void delega_con_la_plantilla_inventory_kardex() {
            when(renderer.render(eq("inventory-kardex"), anyMap())).thenReturn(PDF_BYTES);

            byte[] result = adapter.renderKardex(kardex());

            assertThat(result).isEqualTo(PDF_BYTES);
        }

        @Test
        @DisplayName("pasa el reporte completo al contexto bajo la clave 'r'")
        void pasa_el_reporte_al_contexto() {
            when(renderer.render(eq("inventory-kardex"), anyMap())).thenReturn(PDF_BYTES);
            KardexReport report = kardex();

            adapter.renderKardex(report);

            verify(renderer).render(eq("inventory-kardex"), ctxCaptor.capture());
            assertThat(ctxCaptor.getValue()).containsExactly(Map.entry("r", report));
        }
    }

    @Nested
    @DisplayName("renderPurchases")
    class RenderPurchases {

        @Test
        @DisplayName("delega en el renderer con la plantilla 'inventory-purchases' y devuelve sus bytes")
        void delega_con_la_plantilla_inventory_purchases() {
            when(renderer.render(eq("inventory-purchases"), anyMap())).thenReturn(PDF_BYTES);

            byte[] result = adapter.renderPurchases(compras());

            assertThat(result).isEqualTo(PDF_BYTES);
        }

        @Test
        @DisplayName("pasa el reporte completo al contexto bajo la clave 'r'")
        void pasa_el_reporte_al_contexto() {
            when(renderer.render(eq("inventory-purchases"), anyMap())).thenReturn(PDF_BYTES);
            PurchasesReport report = compras();

            adapter.renderPurchases(report);

            verify(renderer).render(eq("inventory-purchases"), ctxCaptor.capture());
            assertThat(ctxCaptor.getValue()).containsExactly(Map.entry("r", report));
        }
    }
}
