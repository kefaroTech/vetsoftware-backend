package com.vetsoftware.app.documentwithholding.application.port.in;

import com.vetsoftware.app.documentwithholding.application.command.RegisterDocumentWithholdingCommand;
import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RegisterDocumentWithholdingUseCase {

    /**
     * Registra la retencion que un cliente practico sobre una factura de cobro.
     *
     * <p>
     * <strong>Cerrado a {@code hasRole('SYSTEM')} a secas, y la ausencia de un
     * camino de tenant es la decision, no un olvido.</strong> Registrar una
     * retencion es declarar que una factura quedo saldada por un importe que nunca
     * entro a la caja: es el mismo poder que dar por pagada una factura. Lo ejerce
     * tesoreria de la plataforma, cotejando contra el documento que el cliente
     * envio. Una clinica que pudiera escribir sus propias retenciones podria darse
     * por saldada sin haber pagado.
     *
     * <p>
     * <strong>Este parrafo existe para el dia que llegue la peticion.</strong> Una
     * clinica pide registrar ella misma la retencion que le practicaron; quien la
     * atienda no lee el changelog, lee este puerto. Abrirlo no es sembrar un
     * permiso: es cambiar quien puede declarar saldada una factura, y obliga a
     * decidir antes que evidencia se exige para creerselo.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    DocumentWithholdingDto execute(RegisterDocumentWithholdingCommand command);
}
