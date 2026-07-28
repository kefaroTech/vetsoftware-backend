package com.vetsoftware.app.infrastructure.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Renderizador PDF puro Java basado en OpenHTMLToPDF y Apache PDFBox.
 *
 * <p>La concurrencia y el tamaño de salida están acotados para proteger la memoria del
 * proceso del backend frente a reportes anormalmente grandes.
 */
@Component
public class OpenHtmlToPdfEngine implements HtmlToPdfEngine {

    private final PdfProperties properties;
    private final Semaphore renderSlots;
    private final int maxHtmlBytes;
    private final int maxPdfBytes;

    public OpenHtmlToPdfEngine(PdfProperties properties) {
        this.properties = properties;
        this.renderSlots = new Semaphore(properties.maxConcurrentRenders(), true);
        this.maxHtmlBytes = Math.toIntExact(properties.maxHtmlSize().toBytes());
        this.maxPdfBytes = Math.toIntExact(properties.maxPdfSize().toBytes());
    }

    @Override
    public byte[] render(String html) {
        validateHtmlSize(html);
        acquireRenderSlot();
        try (LimitedByteArrayOutputStream output = new LimitedByteArrayOutputStream(maxPdfBytes)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
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
            throw new PdfRenderException("No fue posible generar el PDF con OpenHTMLToPDF", exception);
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

    private void acquireRenderSlot() {
        try {
            boolean acquired = renderSlots.tryAcquire(
                    properties.acquireTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new PdfRenderException(
                        "Tiempo de espera agotado para generar el PDF");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PdfRenderException(
                    "La generación del PDF fue interrumpida", exception);
        }
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
                        "El PDF excede el límite configurado de "
                                + maximumBytes
                                + " bytes");
            }
        }
    }
}
