package com.vetsoftware.app.companybillingprofile.infrastructure.web.request;

import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Cambio de datos de facturacion. <strong>No es un {@code PATCH} ni un
 * {@code PUT}</strong>: cierra la ficha vigente y abre otra.
 *
 * <p>
 * <strong>Trae la ficha entera, no solo los campos que cambian.</strong> Un
 * cuerpo parcial obligaria a copiar de la vieja lo que no viene, y ese arrastre
 * silencioso es como se acaba facturando a la sociedad nueva en la direccion de
 * la anterior. Quien abre una ficha la declara completa.
 *
 * @param effectiveFrom
 *            desde cuando rige la nueva, y tambien la fecha con la que se
 *            cierra la anterior —el intervalo es semiabierto, asi que no queda
 *            hueco ni solape—. Tiene que ser <strong>estrictamente
 *            posterior</strong> al inicio de la vigente: una ficha abierta hoy
 *            no se puede suceder hoy porque
 *            {@code chk_company_billing_profiles_validity} exige
 *            {@code valid_to > valid_from}. Si no lo es, se responde 409 con la
 *            primera fecha posible en el mensaje, y <em>no</em> se corre la
 *            fecha por cuenta propia
 */
public record SucceedCompanyBillingProfileRequest(
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
        @NotNull(message = "Debes indicar desde cuando rige la ficha nueva.") LocalDate effectiveFrom) {
}
