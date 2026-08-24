package com.vetsoftware.app.subscriptionpayment.testsupport;

import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplication;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentRef;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fixtures de la rodaja del dinero. Valores validos por defecto y un metodo por
 * variante, para que cada test declare solo lo que le importa.
 */
public final class SubscriptionPaymentMother {

    public static final Long EMPRESA = 42L;
    public static final Long OTRA_EMPRESA = 99L;
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 22, 10, 30, 0);

    private SubscriptionPaymentMother() {
    }

    public static BigDecimal pesos(String amount) {
        return new BigDecimal(amount);
    }

    /** Pago manual de 500.000 recien registrado: PENDING, sin pasarela. */
    public static SubscriptionPayment pagoPendiente() {
        return new SubscriptionPayment(7L, EMPRESA, pesos("500000.00"), "COP",
                PaymentMethod.TRANSFER, null, null, AHORA, SubscriptionPaymentStatus.PENDING, null,
                null, AHORA, 0L);
    }

    public static SubscriptionPayment pagoConfirmado(String amount) {
        return pagoEnEstado(amount, SubscriptionPaymentStatus.CONFIRMED);
    }

    public static SubscriptionPayment pagoEnEstado(String amount,
            SubscriptionPaymentStatus status) {
        return new SubscriptionPayment(7L, EMPRESA, pesos(amount), "COP", PaymentMethod.TRANSFER,
                null, null, AHORA, status, null, null, AHORA, 0L);
    }

    /** Pago que llego por pasarela: el par (gateway, referencia) deduplica. */
    public static SubscriptionPayment pagoDePasarela() {
        return new SubscriptionPayment(8L, EMPRESA, pesos("500000.00"), "COP", PaymentMethod.PSE,
                "wompi", "TX-2026-0001", AHORA, SubscriptionPaymentStatus.PENDING, null, null,
                AHORA, 0L);
    }

    /** Mismo aviso de pasarela, pero registrado por OTRA clinica. */
    public static SubscriptionPayment pagoDePasarelaDeOtraEmpresa() {
        return new SubscriptionPayment(9L, OTRA_EMPRESA, pesos("500000.00"), "COP",
                PaymentMethod.PSE, "wompi", "TX-2026-0001", AHORA,
                SubscriptionPaymentStatus.PENDING, null, null, AHORA, 0L);
    }

    /** Factura de 1.000.000 con saldo completo pendiente. */
    public static BillingDocumentRef factura() {
        return new BillingDocumentRef(100L, EMPRESA, "FAC-2026-0001", "INVOICE",
                pesos("1000000.00"), pesos("1000000.00"));
    }

    public static BillingDocumentRef facturaDeOtraEmpresa() {
        return new BillingDocumentRef(101L, OTRA_EMPRESA, "FAC-2026-0002", "INVOICE",
                pesos("1000000.00"), pesos("1000000.00"));
    }

    /** Nota credito de 300.000: salda sin que entre un peso. */
    public static BillingDocumentRef notaCredito() {
        return new BillingDocumentRef(200L, EMPRESA, "NC-2026-0001", "CREDIT_NOTE",
                pesos("300000.00"), pesos("0.00"));
    }

    /** Llave de idempotencia del operador que aplica. */
    public static final String LLAVE = "req-aplicacion-1";

    /** Aplicacion ya persistida de 200.000 desde el pago 7 sobre la factura 100. */
    public static BillingDocumentApplication aplicacionDePago() {
        return new BillingDocumentApplication(500L, EMPRESA, factura(),
                com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind.PAYMENT, 7L,
                null, pesos("200000.00"), null, LLAVE, AHORA, AHORA);
    }
}
