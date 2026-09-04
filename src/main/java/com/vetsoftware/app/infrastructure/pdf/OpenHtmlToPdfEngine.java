package com.vetsoftware.app.infrastructure.pdf;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Renderizador PDF puro Java basado en OpenHTMLToPDF y Apache PDFBox.
 *
 * <p>
 * La concurrencia y el tamaño de salida están acotados para proteger la memoria
 * del proceso del backend frente a reportes anormalmente grandes.
 */
@Component
public class OpenHtmlToPdfEngine implements HtmlToPdfEngine {

    private static final List<FontCut> BRAND_FONTS = List.of(
            new FontCut("/fonts/Inter-Regular.ttf", "Inter", 400),
            new FontCut("/fonts/Inter-SemiBold.ttf", "Inter", 600),
            new FontCut("/fonts/Inter-Bold.ttf", "Inter", 700),
            new FontCut("/fonts/Poppins-Regular.ttf", "Poppins", 400),
            new FontCut("/fonts/Poppins-SemiBold.ttf", "Poppins", 600),
            new FontCut("/fonts/Poppins-Bold.ttf", "Poppins", 700),
            new FontCut("/fonts/JetBrainsMono-Regular.ttf", "JetBrains Mono", 400));

    private final PdfProperties properties;
    private final Semaphore renderSlots;
    private final int maxHtmlBytes;
    private final int maxPdfBytes;
    private final Map<FontCut, byte[]> brandFonts;

    public OpenHtmlToPdfEngine(PdfProperties properties) {
        this.properties = properties;
        this.renderSlots = new Semaphore(properties.maxConcurrentRenders(), true);
        this.maxHtmlBytes = Math.toIntExact(properties.maxHtmlSize().toBytes());
        this.maxPdfBytes = Math.toIntExact(properties.maxPdfSize().toBytes());
        this.brandFonts = loadBrandFonts();
    }

    @Override
    public byte[] render(String html) {
        validateHtmlSize(html);
        acquireRenderSlot();
        try (LimitedByteArrayOutputStream output = new LimitedByteArrayOutputStream(maxPdfBytes)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            registerBrandFonts(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();

            byte[] pdf = output.toByteArray();
            if (pdf.length == 0) {
                throw new PdfRenderException("OpenHTMLToPDF generó un documento vacío");
            }
            return pdf;
        } catch (PdfRenderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PdfRenderException("No fue posible generar el PDF con OpenHTMLToPDF",
                    exception);
        } finally {
            renderSlots.release();
        }
    }

    private void validateHtmlSize(String html) {
        if (html == null || html.isBlank()) {
            throw new PdfRenderException("El HTML del documento está vacío");
        }
        if (html.length() > maxHtmlBytes
                || html.getBytes(StandardCharsets.UTF_8).length > maxHtmlBytes) {
            throw new PdfRenderException(
                    "El HTML excede el límite configurado de " + maxHtmlBytes + " bytes");
        }
    }

    private void registerBrandFonts(PdfRendererBuilder builder) {
        brandFonts.forEach((cut, data) -> builder.useFont(() -> new ByteArrayInputStream(data),
                cut.family(), cut.weight(), FontStyle.NORMAL, true));
    }

    /**
     * Se leen al construir el bean, no en cada render: openhtmltopdf sustituye una
     * familia que no puede resolver por Helvetica <strong>sin emitir ningún
     * aviso</strong>, así que una fuente ausente tiene que impedir el arranque en
     * vez de degradar en silencio un PDF que ya está en manos del cliente.
     */
    private static Map<FontCut, byte[]> loadBrandFonts() {
        Map<FontCut, byte[]> loaded = new LinkedHashMap<>();
        BRAND_FONTS.forEach(cut -> loaded.put(cut, read(cut.resource())));
        return Collections.unmodifiableMap(loaded);
    }

    private static byte[] read(String resource) {
        try (InputStream stream = OpenHtmlToPdfEngine.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Falta la fuente embebida " + resource);
            }
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible leer la fuente " + resource, exception);
        }
    }

    private void acquireRenderSlot() {
        try {
            boolean acquired = renderSlots.tryAcquire(properties.acquireTimeout().toMillis(),
                    TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new PdfRenderException("Tiempo de espera agotado para generar el PDF");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PdfRenderException("La generación del PDF fue interrumpida", exception);
        }
    }

    private record FontCut(String resource, String family, int weight) {
    }

    private static final class LimitedByteArrayOutputStream extends ByteArrayOutputStream {
        private final int maximumBytes;

        private LimitedByteArrayOutputStream(int maximumBytes) {
            super(Math.min(maximumBytes, 32 * 1024));
            this.maximumBytes = maximumBytes;
        }

        @Override
        public synchronized void write(int value) {
            ensureCapacityFor(1);
            super.write(value);
        }

        @Override
        public synchronized void write(byte[] buffer, int offset, int length) {
            ensureCapacityFor(length);
            super.write(buffer, offset, length);
        }

        private void ensureCapacityFor(int additionalBytes) {
            if (additionalBytes < 0 || count > maximumBytes - additionalBytes) {
                throw new PdfRenderException(
                        "El PDF excede el límite configurado de " + maximumBytes + " bytes");
            }
        }
    }
}
