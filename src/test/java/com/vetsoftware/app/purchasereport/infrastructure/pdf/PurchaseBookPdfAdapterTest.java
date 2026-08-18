package com.vetsoftware.app.purchasereport.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;
import com.vetsoftware.app.purchasereport.testsupport.PurchaseReportMother;
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
@DisplayName("PurchaseBookPdfAdapter — renderiza el libro de compras con la infraestructura PDF embebida")
class PurchaseBookPdfAdapterTest {

    private static final byte[] PDF_BYTES = {0x25, 0x50, 0x44, 0x46};

    @Mock
    private HtmlPdfRenderer renderer;

    @InjectMocks
    private PurchaseBookPdfAdapter adapter;

    @Captor
    private ArgumentCaptor<Map<String, Object>> ctxCaptor;

    @Nested
    @DisplayName("renderPurchaseBook")
    class RenderPurchaseBook {

        @Test
        @DisplayName("delega en el renderer con la plantilla 'purchase-book' y devuelve sus bytes")
        void delega_con_la_plantilla_purchase_book_y_devuelve_sus_bytes() {
            when(renderer.render(eq("purchase-book"), anyMap())).thenReturn(PDF_BYTES);

            byte[] result = adapter.renderPurchaseBook(PurchaseReportMother.libro());

            assertThat(result).isEqualTo(PDF_BYTES);
            verify(renderer).render(eq("purchase-book"), anyMap());
        }

        @Test
        @DisplayName("pasa el libro completo al contexto de la plantilla bajo la clave 'b'")
        void pasa_el_libro_al_contexto_bajo_la_clave_b() {
            when(renderer.render(eq("purchase-book"), anyMap())).thenReturn(PDF_BYTES);
            PurchaseBookDto libro = PurchaseReportMother.libro();

            adapter.renderPurchaseBook(libro);

            verify(renderer).render(eq("purchase-book"), ctxCaptor.capture());
            assertThat(ctxCaptor.getValue()).containsOnly(Map.entry("b", libro));
        }

        @Test
        @DisplayName("un libro sin compras tambien se delega tal cual, sin filtrar nada en el adapter")
        void un_libro_sin_compras_tambien_se_delega_tal_cual() {
            when(renderer.render(eq("purchase-book"), anyMap())).thenReturn(PDF_BYTES);
            PurchaseBookDto libroVacio = PurchaseReportMother.libroSinCompras();

            adapter.renderPurchaseBook(libroVacio);

            verify(renderer).render(eq("purchase-book"), ctxCaptor.capture());
            assertThat(ctxCaptor.getValue().get("b")).isEqualTo(libroVacio);
        }
    }
}
