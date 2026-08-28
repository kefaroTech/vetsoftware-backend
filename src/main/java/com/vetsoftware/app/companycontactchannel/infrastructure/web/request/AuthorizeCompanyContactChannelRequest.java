package com.vetsoftware.app.companycontactchannel.infrastructure.web.request;

import com.vetsoftware.app.companycontactchannel.domain.ContactChannelType;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <strong>Sin {@code companyId}, y no es una omision cosmetica.</strong> La
 * empresa la pone el servidor desde el token
 * ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}): si viajara aqui, un cliente podria
 * sembrar canales de contacto en la ficha de otra empresa, que no es leer dato
 * ajeno sino escribir por donde se le avisa a la competencia.
 *
 * <p>
 * <strong>Sin {@code authorizedAt} y sin {@code isPrimary}.</strong> La fecha
 * la pone el reloj del servidor porque es la que decide si un aviso ya enviado
 * estaba permitido —aceptarla aqui seria dejar antedatar el consentimiento—, y
 * el canal nace no primario porque designar el principal tiene su propio
 * endpoint: escondido en el alta, un {@code POST} rutinario desviaria la
 * facturacion de la empresa sin que nadie lo lea como lo que es.
 *
 * @param address
 *            el correo, el movil o el identificador del canal. No se valida
 *            aqui la forma segun el tipo —un {@code @Email} solo tendria
 *            sentido para {@code EMAIL}— y hacerlo condicional en el binder
 *            partiria el mensaje de error en dos formas distintas para el mismo
 *            campo
 * @param authorizationEvidence
 *            con que se demuestra el consentimiento: el formulario firmado, la
 *            clausula del contrato, la grabacion. Obligatorio, porque una
 *            autorizacion sin respaldo no sirve para lo unico que esta tabla
 *            existe
 */
public record AuthorizeCompanyContactChannelRequest(
        @NotNull(message = "Debes indicar el tipo de canal.") ContactChannelType channelType,
        @NotBlank(message = "Debes indicar la direccion del canal.") @Size(max = 160, message = "La direccion no puede superar los 160 caracteres.") String address,
        @NotNull(message = "Debes indicar para que se autorizo el canal.") ContactPurpose purpose,
        @NotBlank(message = "Debes indicar con que se demuestra la autorizacion.") @Size(max = 255, message = "La evidencia no puede superar los 255 caracteres.") String authorizationEvidence) {
}
