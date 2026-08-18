package com.vetsoftware.app.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class HtmlPdfRendererTest {

    @Mock
    private TemplateEngine templateEngine;
    @Mock
    private HtmlToPdfEngine pdfEngine;

    private HtmlPdfRenderer renderer;

    @Test
    @DisplayName("procesa la plantilla bajo pdf/ con el modelo y delega el HTML resultante al motor")
    void procesa_la_plantilla_bajo_pdf_y_delega_el_html_al_motor() {
        renderer = new HtmlPdfRenderer(templateEngine, pdfEngine);
        Map<String, Object> model = Map.of("r", "reporte");
        when(templateEngine.process(eq("pdf/inventory-kardex"), any(Context.class)))
                .thenReturn("<html>kardex</html>");
        byte[] pdfBytes = {'%', 'P', 'D', 'F'};
        when(pdfEngine.render("<html>kardex</html>")).thenReturn(pdfBytes);

        byte[] result = renderer.render("inventory-kardex", model);

        assertThat(result).isSameAs(pdfBytes);
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("pdf/inventory-kardex"), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getVariable("r")).isEqualTo("reporte");
        assertThat(contextCaptor.getValue().getLocale()).isEqualTo(Locale.forLanguageTag("es"));
    }
}
