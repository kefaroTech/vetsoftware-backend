package com.vetsoftware.app.cashregister.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.cashregister.application.dto.CashArqueoReport;
import com.vetsoftware.app.cashregister.testsupport.CashSessionMother;
import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
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
@DisplayName("CashReportPdfAdapter")
class CashReportPdfAdapterTest {

    private static final byte[] PDF_BYTES = {1, 2, 3};

    @Mock
    private HtmlPdfRenderer renderer;

    @InjectMocks
    private CashReportPdfAdapter adapter;

    @Captor
    private ArgumentCaptor<Map<String, Object>> ctxCaptor;

    @Nested
    @DisplayName("renderArqueo")
    class RenderArqueo {

        @Test
        @DisplayName("delega en el renderer con la plantilla 'cash-arqueo' y devuelve sus bytes")
        void delega_con_la_plantilla_cash_arqueo() {
            when(renderer.render(eq("cash-arqueo"), anyMap())).thenReturn(PDF_BYTES);
            CashArqueoReport report = CashArqueoReport.from(CashSessionMother.sesionCerrada());

            byte[] result = adapter.renderArqueo(report);

            assertThat(result).isEqualTo(PDF_BYTES);
        }

        @Test
        @DisplayName("el contexto lleva el reporte completo bajo la clave 'r'")
        void el_contexto_lleva_el_reporte_bajo_la_clave_r() {
            when(renderer.render(eq("cash-arqueo"), anyMap())).thenReturn(PDF_BYTES);
            CashArqueoReport report = CashArqueoReport.from(CashSessionMother.sesionCerrada());

            adapter.renderArqueo(report);

            verify(renderer).render(eq("cash-arqueo"), ctxCaptor.capture());
            assertThat(ctxCaptor.getValue()).containsEntry("r", report);
        }
    }
}
