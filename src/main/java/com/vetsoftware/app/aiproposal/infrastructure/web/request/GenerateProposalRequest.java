package com.vetsoftware.app.aiproposal.infrastructure.web.request;

import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * La pantalla 1.
 *
 * <p>
 * &#9940; <strong>Sin {@code companyId} y sin token.</strong> No hay empresa a
 * la que pertenecer -un prospecto no es un cliente- y todavia no hay propuesta
 * que identificar: el token nace en la respuesta.
 *
 * <p>
 * <strong>El minimo son 15 caracteres, no 30.</strong> Es el unico de los tres
 * numeros que llegaron a circular que traia argumento escrito: "Clinica de
 * barrio, consulta general y vacunas" son 38 caracteres y bastan para proponer
 * historia clinica, vacunacion y agenda, asi que un umbral alto castiga a quien
 * escribe bien y corto -que es el usuario con prisa que este producto tiene-.
 * El suelo esta en 15 porque "hola" no es informacion, y el texto corto pero
 * pobre lo resuelve el modelo con {@code understood = false}, que es una
 * pantalla util y no un 400.
 *
 * <p>
 * &#9888; Las restricciones solo se evaluan con {@code @Valid} delante del
 * {@code @RequestBody}: sin el, el binder no dispara el validador y lo que se
 * lee perfecto en el diff no lo comprueba nadie. Lo vigila
 * {@code CUERPO_CON_RESTRICCIONES_SE_VALIDA}.
 *
 * @param billingCycle
 *            &#9940; <strong>OPCIONAL, y por defecto {@code MONTHLY}: no puede
 *            ser obligatorio.</strong> Este record es un esquema del contrato y
 *            los dos fronts lo atan; declararlo {@code @NotNull} convierte en
 *            400 <em>toda</em> peticion de cualquier cliente ya desplegado que
 *            no lo mande —incluida la landing publica, que es el endpoint mas
 *            expuesto del producto— por un campo que hasta hoy no existia. Con
 *            el nulo permitido el cambio es estrictamente aditivo: quien no lo
 *            manda recibe exactamente lo que recibia ayer, que era mensual, y
 *            el defecto se aplica en {@code GenerateProposalCommand} y no aqui
 *            para que ningun otro llamante del caso de uso pueda saltarselo.
 *            <p>
 *            El precio de esa eleccion, escrito: un cliente que <em>queria</em>
 *            anual y escribe mal el campo recibe mensual en silencio en vez de
 *            un 400. Se acepta porque el binder de Spring si rechaza con 400 un
 *            valor <em>presente</em> y no reconocido —{@code "YEARLY"},
 *            {@code "anual"}—, asi que el unico caso silencioso es la ausencia
 *            total, que es justo el cliente viejo al que hay que no romper.
 */
public record GenerateProposalRequest(@NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 15, max = 1000) String description,
        @NotEmpty @Valid List<LegalAcceptanceRequest> acceptances,
        ProposalBillingCycle billingCycle) {
}
