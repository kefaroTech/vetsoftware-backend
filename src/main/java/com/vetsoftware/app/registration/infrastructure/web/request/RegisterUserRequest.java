package com.vetsoftware.app.registration.infrastructure.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "El nombre de la empresa es obligatorio.") @Size(max = 100, message = "El nombre de la empresa no puede superar los 100 caracteres.") String companyName,
        @NotBlank(message = "Debes seleccionar el tipo de documento.") String documentType,
        @NotBlank(message = "El número de identificación de la empresa es obligatorio.") @Size(max = 20, message = "El número de identificación de la empresa no puede superar los 20 caracteres.") String companyIdentifier,
        @Size(max = 200, message = "La dirección de la empresa no puede superar los 200 caracteres.") String companyAddress,
        @Size(max = 30, message = "El teléfono de contacto no puede superar los 30 caracteres.") String companyContactNumber,
        @NotNull(message = "Debes seleccionar la ciudad.") Long cityId,
        @NotBlank(message = "El nombre del empleado es obligatorio.") @Size(max = 100, message = "El nombre del empleado no puede superar los 100 caracteres.") String employeeName,
        @NotBlank(message = "El correo electrónico es obligatorio.") @Email(message = "El correo electrónico no tiene un formato válido.") @Size(max = 100, message = "El correo electrónico no puede superar los 100 caracteres.") String employeeEmail,
        @NotBlank(message = "La contraseña es obligatoria.") @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres.") String password,
        @NotBlank(message = "Debes seleccionar el régimen tributario.") String taxRegime,
        @NotBlank(message = "El correo electrónico fiscal es obligatorio.") @Email(message = "El correo electrónico no tiene un formato válido.") @Size(max = 255, message = "El correo electrónico fiscal no puede superar los 255 caracteres.") String fiscalEmail,
        // Token del challenge reCAPTCHA. Opcional a nivel de bean-validation porque el
        // captcha puede
        // estar
        // deshabilitado por config (dev); cuando esta habilitado, el CaptchaVerifier
        // exige su
        // presencia.
        @Size(max = 4000, message = "La verificación de seguridad no puede superar los 4000 caracteres.") String recaptchaToken) {
}
