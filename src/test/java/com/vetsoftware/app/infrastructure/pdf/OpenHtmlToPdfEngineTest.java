package com.vetsoftware.app.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class OpenHtmlToPdfEngineTest {

    @Test
    void rendersValidPdfWithAccentedTextAndPagedCss() throws Exception {
        OpenHtmlToPdfEngine engine = engine(DataSize.ofMegabytes(1), DataSize.ofMegabytes(2));
        String html = """
                <!DOCTYPE html>
                <html lang="es">
                  <head>
                    <meta charset="UTF-8"/>
                    <style>
                      @page { size: A4; margin: 15mm; }
                      body { font-family: sans-serif; }
                      table { width: 100%; border-collapse: collapse; -fs-table-paginate: paginate; }
                      th, td { border: 1px solid #444; padding: 4px; }
                    </style>
                  </head>
                  <body>
                    <h1>Fórmula médica veterinaria</h1>
                    <table><thead><tr><th>Descripción</th></tr></thead>
                    <tbody><tr><td>Vacunación y diagnóstico</td></tr></tbody></table>
                    <img alt="QR" width="1" height="1"
                         src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="/>
                  </body>
                </html>
                """;

        byte[] pdf = engine.render(html);

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        try (var document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isOne();
        }
    }

    @Test
    void rejectsBlankAndOversizedInput() {
        OpenHtmlToPdfEngine engine = engine(DataSize.ofBytes(16), DataSize.ofMegabytes(1));

        assertThatThrownBy(() -> engine.render(" ")).isInstanceOf(PdfRenderException.class)
                .hasMessageContaining("vacío");
        assertThatThrownBy(() -> engine.render("<html>documento demasiado grande</html>"))
                .isInstanceOf(PdfRenderException.class).hasMessageContaining("excede el límite");
    }

    @Test
    void stopsRenderingWhenOutputExceedsConfiguredLimit() {
        OpenHtmlToPdfEngine engine = engine(DataSize.ofKilobytes(4), DataSize.ofBytes(128));

        assertThatThrownBy(() -> engine.render("<html><body>PDF</body></html>"))
                .isInstanceOf(PdfRenderException.class);
    }

    private static OpenHtmlToPdfEngine engine(DataSize maxHtmlSize, DataSize maxPdfSize) {
        return new OpenHtmlToPdfEngine(
                new PdfProperties(1, Duration.ofSeconds(1), maxHtmlSize, maxPdfSize));
    }
}
