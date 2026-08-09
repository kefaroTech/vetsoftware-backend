package com.vetsoftware.app.electronicdocument.domain;

/**
 * La venta pidió descontar unas unidades del kardex y el ledger descontó otras.
 *
 * <p>
 * No es una condición de negocio: es la firma de un defecto de software. El POS
 * nunca se frena por falta de stock —permite negativo—, así que la única razón
 * para que lo descontado no coincida con lo vendido es que una salida se haya
 * ignorado por el camino. Eso fue exactamente BE-01, y estuvo meses sin
 * detectarse porque no fallaba nada: el inventario simplemente se iba
 * sobrestimando.
 *
 * <p>
 * Se lanza dentro de la transacción de la venta, así que la venta entera se
 * deshace. Es deliberado: negarle el cobro al cajero es incómodo una vez;
 * descuadrar el inventario en silencio se paga durante meses y no se puede
 * reconstruir después.
 */
public class StockDiscountMismatchException extends RuntimeException {
    public StockDiscountMismatchException(Long documentId, Long productId, int expected,
            int actual) {
        super("El kardex no descontó lo vendido en el documento " + documentId + ", producto "
                + productId + ": se esperaban " + expected + " unidades y se descontaron " + actual
                + ".");
    }
}
