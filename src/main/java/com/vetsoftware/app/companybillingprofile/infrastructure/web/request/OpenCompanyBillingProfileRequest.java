package com.vetsoftware.app.companybillingprofile.infrastructure.web.request;

import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Alta de la ficha de facturacion de la empresa del usuario.
 *
 * <p>
 * <strong>Sin {@code companyId}</strong>, y no por descuido: el request de un
 * recurso scoped nunca lo lleva ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}). Lo
 * inyecta el controller desde el principal y lo revalida el
 * {@code @PreAuthorize} del puerto. Aceptarlo aqui dejaria que un cliente
 * cambiara a nombre de quien se factura en otra clinica.
 *
 * @param verificationDigit
 *            opcional: solo el NIT lo tiene. <strong>No se recalcula</strong>
 *            —el algoritmo del modulo 11 vive en {@code companytaxprofile} y es
 *            el digito de la propia clinica, no el del cliente—, asi que lo que
 *            llega es lo que se guarda
 * @param withholdingAgent
 *            <strong>{@code Boolean} con {@code @NotNull}, nunca un
 *            {@code boolean} primitivo.</strong> Con el primitivo, un cuerpo
 *            que omita el campo se enlaza a {@code false} en silencio y la
 *            ficha queda diciendo que el cliente no es agente de retencion, que
 *            es una afirmacion sobre dinero que nadie hizo. Con el envoltorio,
 *            la omision es un error de campo que el front pinta bajo la casilla
 * @param validFrom
 *            desde cuando rige. Va explicito y no se toma del reloj del
 *            servidor: una ficha puede firmarse hoy para regir desde el primero
 *            del mes que viene
 */
public record OpenCompanyBillingProfileRequest(
        @NotNull(message = "Debes indicar si el tercero es persona natural o juridica.") PersonKind personKind,
        @NotNull(message = "Debes indicar el tipo de documento.") TaxIdKind taxIdKind,
        @NotBlank(message = "El numero de documento es obligatorio.") @Size(max = 50, message = "El numero de documento no puede superar los 50 caracteres.") String taxId,
        @Size(max = 1, message = "El digito de verificacion es un solo caracter.") String verificationDigit,
        @Size(max = 255, message = "La razon social no puede superar los 255 caracteres.") String legalName,
        @Size(max = 80, message = "El primer nombre no puede superar los 80 caracteres.") String firstName,
        @Size(max = 80, message = "Los otros nombres no pueden superar los 80 caracteres.") String middleName,
        @Size(max = 80, message = "El primer apellido no puede superar los 80 caracteres.") String lastName,
        @Size(max = 80, message = "El segundo apellido no puede superar los 80 caracteres.") String secondLastName,
        @NotBlank(message = "La direccion de facturacion es obligatoria.") @Size(max = 255, message = "La direccion no puede superar los 255 caracteres.") String address,
        @NotNull(message = "Debes indicar el municipio de la direccion.") Long cityId,
        @NotBlank(message = "El correo de facturacion es obligatorio.") @Size(max = 160, message = "El correo no puede superar los 160 caracteres.") String billingEmail,
        @NotNull(message = "Debes indicar el regimen fiscal.") TaxRegime taxRegime,
        @NotNull(message = "Debes indicar si el tercero es agente de retencion.") Boolean withholdingAgent,
        @NotNull(message = "Debes indicar desde cuando rige la ficha.") LocalDate validFrom) {
}
