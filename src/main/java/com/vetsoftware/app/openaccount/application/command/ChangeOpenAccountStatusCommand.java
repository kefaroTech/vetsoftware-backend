package com.vetsoftware.app.openaccount.application.command;

/**
 * El {@code closedById} es <b>quien cierra</b>, no un recurso que elija el
 * cliente: lo rellena el controller con {@code authz.currentEmployeeId()} y el
 * request REST no lo transporta. Se llama asi —y no {@code employeeId}— porque
 * ese nombre lo hacia indistinguible de un id de empleado elegido por el
 * atacante, que es justo lo que las reglas de la familia «por id» tienen que
 * poder seguir marcando. Mismo criterio que
 * {@code CreateOpenAccountCommand.createdById} y
 * {@code SuspendHospitalizationMedicationCommand.suspendedById}.
 */
public record ChangeOpenAccountStatusCommand(Long id, String status, Long closedById, String reason,
        Long companyId,
        // Solo relevantes al cerrar (CLOSE): qué documento electrónico auto-emitir.
        // `documentType` =
        // "DOC_EQUIV_POS" (venta de mostrador) o "FE_VENTA" (factura); null →
        // DOC_EQUIV_POS por
        // defecto.
        String documentType, boolean finalConsumer,
        // Versión esperada de la cuenta (opt-in): si se envía y no coincide con la
        // actual, se rechaza
        // temprano con CONCURRENT_MODIFICATION. null = sin chequeo temprano.
        Long expectedVersion) {
}
