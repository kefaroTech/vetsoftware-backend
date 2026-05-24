package com.vetsoftware.app.infrastructure.pdf;

/**
 * Opciones de render passed a Gotenberg. Las dimensiones aceptan unidades CSS
 * (in, cm, mm, pt, px); por defecto Gotenberg interpreta valores numéricos como inches.
 */
public record PdfOptions(
        String paperWidth,
        String paperHeight,
        String margin,
        boolean printBackground,
        boolean preferCssPageSize
) {
    /** A4 portrait, 20mm de margen, fondos CSS habilitados. */
    public static PdfOptions defaults() {
        return new PdfOptions("8.27in", "11.69in", "0.79in", true, true);
    }
}
