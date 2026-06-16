package com.vetsoftware.app.openaccount.application.event;

/**
 * Evento publicado cuando una cuenta se cierra (CLOSE) y debe auto-emitirse su documento electrónico.
 * Es un simple portador de datos (no acopla a la feature electronicdocument): lo consume un listener
 * en esa feature TRAS el commit del cierre, para que la emisión vea la cuenta ya cerrada y no bloquee
 * la venta si la DIAN/numeración fallan.
 *
 * `documentType`: "DOC_EQUIV_POS" o "FE_VENTA".
 */
public record OpenAccountClosedForEmissionEvent(
        Long openAccountId,
        Long companyId,
        String documentType,
        boolean finalConsumer
) {}
