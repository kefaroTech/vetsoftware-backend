package com.vetsoftware.app.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.DisplayName;
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

    @Test
    @DisplayName("un HTML cuyo tamaño en bytes excede el límite se rechaza aunque su longitud en "
            + "caracteres no lo haga (caracteres multibyte UTF-8)")
    void rejectsHtmlWhoseByteSizeExceedsTheLimitEvenWhenItsCharacterCountDoesNot() {
        // 'ó' ocupa 2 bytes en UTF-8: 7 caracteres pero 14 bytes, por encima del límite
        // de 10 bytes aunque por debajo si solo se contaran caracteres.
        OpenHtmlToPdfEngine engine = engine(DataSize.ofBytes(10), DataSize.ofMegabytes(1));

        assertThatThrownBy(() -> engine.render("óóóóóóó")).isInstanceOf(PdfRenderException.class)
                .hasMessageContaining("excede el límite");
    }

    @Test
    @DisplayName("agota el tiempo de espera cuando no hay cupo de render disponible")
    void reportsTimeoutWhenNoRenderSlotIsAvailable() throws Exception {
        OpenHtmlToPdfEngine engine = new OpenHtmlToPdfEngine(new PdfProperties(1,
                Duration.ofMillis(1), DataSize.ofMegabytes(1), DataSize.ofMegabytes(1)));
        acquireTheSoleRenderSlot(engine);

        assertThatThrownBy(() -> engine.render("<html><body>PDF</body></html>"))
                .isInstanceOf(PdfRenderException.class)
                .hasMessageContaining("Tiempo de espera agotado");
    }

    @Test
    @DisplayName("una interrupción mientras espera el cupo de render se envuelve como PdfRenderException")
    void wrapsAnInterruptionWhileWaitingForARenderSlotAsPdfRenderException() {
        OpenHtmlToPdfEngine engine = engine(DataSize.ofMegabytes(1), DataSize.ofMegabytes(1));
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> engine.render("<html><body>PDF</body></html>"))
                .isInstanceOf(PdfRenderException.class).hasMessageContaining("interrumpida");

        // La producción restaura el flag de interrupción al capturar
        // InterruptedException;
        // se limpia aquí para no filtrar estado a otros tests del mismo hilo.
        Thread.interrupted();
    }

    private static void acquireTheSoleRenderSlot(OpenHtmlToPdfEngine engine) throws Exception {
        Field field = OpenHtmlToPdfEngine.class.getDeclaredField("renderSlots");
        field.setAccessible(true);
        ((Semaphore) field.get(engine)).acquire();
    }

    private static OpenHtmlToPdfEngine engine(DataSize maxHtmlSize, DataSize maxPdfSize) {
        return new OpenHtmlToPdfEngine(
                new PdfProperties(1, Duration.ofSeconds(1), maxHtmlSize, maxPdfSize));
    }
}
