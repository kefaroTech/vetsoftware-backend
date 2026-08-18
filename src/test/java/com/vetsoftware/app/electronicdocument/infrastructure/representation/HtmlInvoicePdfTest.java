package com.vetsoftware.app.electronicdocument.infrastructure.representation;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("HtmlInvoicePdf — arma el modelo plano para Thymeleaf y delega el render")
class HtmlInvoicePdfTest {

    @Mock
    private HtmlPdfRenderer renderer;

    private HtmlInvoicePdf invoicePdf;

    @BeforeEach
    void montar() {
        invoicePdf = new HtmlInvoicePdf(renderer);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> renderizarYCapturarModelo(ElectronicDocument documento) {
        byte[] pdfEsperado = "pdf-bytes".getBytes();
        when(renderer.render(eq("electronic-invoice"), any())).thenReturn(pdfEsperado);

        byte[] resultado = invoicePdf.render(documento, "QR_BASE64");

        assertThat(resultado).isEqualTo(pdfEsperado);
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(renderer).render(eq("electronic-invoice"), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("usa siempre la plantilla electronic-invoice y devuelve los bytes del renderer")
    void usa_la_plantilla_electronic_invoice() {
        renderizarYCapturarModelo(facturaValidada(1L));
    }

    @Test
    @DisplayName("el numero es prefijo + consecutivo concatenados")
    void el_numero_es_prefijo_mas_consecutivo() {
        Map<String, Object> model = renderizarYCapturarModelo(facturaValidada(1L));

        assertThat(model.get("number")).isEqualTo("SETP990");
    }

    @Test
    @DisplayName("el QR se embebe como data URI base64 de PNG")
    void el_qr_se_embebe_como_data_uri() {
        Map<String, Object> model = renderizarYCapturarModelo(facturaValidada(1L));

        assertThat(model.get("qr")).isEqualTo("data:image/png;base64,QR_BASE64");
    }

    @Test
    @DisplayName("traslada emisor, adquiriente y totales del documento")
    void traslada_emisor_adquiriente_y_totales() {
        ElectronicDocument documento = facturaValidada(1L);

        Map<String, Object> model = renderizarYCapturarModelo(documento);

        assertThat(model.get("issuerLegalName")).isEqualTo(documento.getIssuer().legalName());
        assertThat(model.get("customerName")).isEqualTo(documento.getCustomer().name());
        assertThat(model.get("payable")).isEqualTo(documento.getPayableAmount());
        assertThat(model.get("cufe")).isEqualTo(documento.getCufe());
    }

    @Nested
    @DisplayName("lineas y desglose de impuesto por tarifa")
    class LineasYDesglose {

        @Test
        @DisplayName("cada linea del documento se traduce a una fila del modelo")
        void cada_linea_se_traduce_a_una_fila() {
            Map<String, Object> model = renderizarYCapturarModelo(facturaValidada(1L));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lines = (List<Map<String, Object>>) model.get("lines");
            assertThat(lines).hasSize(1);
            assertThat(lines.get(0).get("taxCategory")).isEqualTo("GRAVADO");
            assertThat(lines.get(0).get("total")).isEqualTo(new BigDecimal("1190.00"));
        }

        @Test
        @DisplayName("agrupa el desglose de impuesto por esquema+tarifa")
        void agrupa_el_desglose_de_impuesto_por_tarifa() {
            Map<String, Object> model = renderizarYCapturarModelo(facturaValidada(1L));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> taxTotals = (List<Map<String, Object>>) model
                    .get("taxTotals");
            assertThat(taxTotals).hasSize(1);
            assertThat(taxTotals.get(0).get("label")).isEqualTo("IVA 19%");
        }
    }
}
