package com.vetsoftware.app.aiproposal.infrastructure.web;

import com.vetsoftware.app.aiproposal.application.command.SuppressProposalDataCommand;
import com.vetsoftware.app.aiproposal.application.port.in.SuppressProposalDataUseCase;
import com.vetsoftware.app.aiproposal.infrastructure.web.request.SuppressProposalDataRequest;
import com.vetsoftware.app.aiproposal.infrastructure.web.response.ProposalSuppressionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara operativa de la retencion: supresion a peticion del titular.
 *
 * <p>
 * <strong>Controller aparte del asistente a proposito.</strong>
 * {@code AssistantController} sirve las cuatro rutas anonimas del prospecto;
 * esto es {@code SYSTEM} y no comparte ni publico ni gate. Mezclarlos deja un
 * metodo cerrado dentro de una clase cuyo javadoc dice "anonima", que es como
 * se abre el siguiente por descuido.
 *
 * <p>
 * &#9940; <strong>{@code POST} y no {@code DELETE}</strong>: el correo tiene
 * que viajar en el cuerpo -ver {@code SuppressProposalDataRequest}- y un
 * {@code DELETE} con cuerpo no lo respetan ni los proxies ni todos los
 * clientes. Y la ruta es {@code /assistant/proposals/suppress}, en plural: el
 * singular {@code /assistant/proposal} es el path exacto que
 * {@code LoginRateLimitFilter} casa con {@code equals} para el cupo del
 * endpoint de pago.
 *
 * <p>
 * <strong>No esta en {@code PublicRoutes.BUSINESS}</strong>, asi que el
 * {@code AuthFilter} exige JWT antes de que el {@code @PreAuthorize} del puerto
 * llegue a evaluarse.
 */
@RestController
@RequestMapping("/assistant/proposals")
public class AiProposalRetentionController {

    private final SuppressProposalDataUseCase suppressUseCase;

    public AiProposalRetentionController(SuppressProposalDataUseCase suppressUseCase) {
        this.suppressUseCase = suppressUseCase;
    }

    /**
     * Devuelve 200 con los contadores incluso cuando no habia nada que borrar. Un
     * 404 para "ese correo no esta" convertiria este endpoint en un oraculo que
     * responde si una direccion pidio propuesta alguna vez.
     */
    @PostMapping("/suppress")
    public ProposalSuppressionResponse suppress(
            @Valid @RequestBody SuppressProposalDataRequest request) {
        return ProposalSuppressionResponse.from(
                suppressUseCase.execute(new SuppressProposalDataCommand(request.contactEmail())));
    }
}
