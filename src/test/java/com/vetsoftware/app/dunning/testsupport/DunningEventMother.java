package com.vetsoftware.app.dunning.testsupport;

import com.vetsoftware.app.dunning.domain.BillingDocumentRef;
import com.vetsoftware.app.dunning.domain.DunningChannel;
import com.vetsoftware.app.dunning.domain.DunningEvent;
import com.vetsoftware.app.dunning.domain.DunningEventType;
import com.vetsoftware.app.dunning.domain.SubscriptionRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class DunningEventMother {

    public static final Long EMPRESA = 42L;
    public static final Long OTRA_EMPRESA = 99L;
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 22, 10, 30, 0);

    private DunningEventMother() {
    }

    public static SubscriptionRef contrato() {
        return new SubscriptionRef(11L, EMPRESA, "SUS-2026-00184", "PAST_DUE");
    }

    public static SubscriptionRef contratoDeOtraEmpresa() {
        return new SubscriptionRef(12L, OTRA_EMPRESA, "SUS-2026-00999", "ACTIVE");
    }

    public static BillingDocumentRef factura() {
        return new BillingDocumentRef(100L, EMPRESA, "FAC-2026-0001", new BigDecimal("250000.00"));
    }

    public static BillingDocumentRef facturaDeOtraEmpresa() {
        return new BillingDocumentRef(101L, OTRA_EMPRESA, "FAC-2026-0009",
                new BigDecimal("250000.00"));
    }

    /** Recordatorio por correo a los 5 dias de mora. */
    public static DunningEvent recordatorio() {
        return DunningEvent.record(EMPRESA, contrato(), factura(), DunningEventType.REMINDER_SENT,
                5, DunningChannel.EMAIL, "Primer aviso", AHORA, AHORA);
    }
}
