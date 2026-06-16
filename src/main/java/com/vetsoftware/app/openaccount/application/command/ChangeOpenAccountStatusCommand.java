package com.vetsoftware.app.openaccount.application.command;

public record ChangeOpenAccountStatusCommand(
        Long id,
        String status,
        Long employeeId,
        String reason,
        Long companyId,
        // Solo relevantes al cerrar (CLOSE): qué documento electrónico auto-emitir. `documentType` =
        // "DOC_EQUIV_POS" (venta de mostrador) o "FE_VENTA" (factura); null → DOC_EQUIV_POS por defecto.
        String documentType,
        boolean finalConsumer
) {}
