package com.vetsoftware.app.supplier.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupplierRequest(
        @NotBlank(message = "El nombre del proveedor es obligatorio.") @Size(max = 150, message = "El nombre del proveedor no puede superar los 150 caracteres.") String name,
        @Size(max = 30, message = "El NIT no puede superar los 30 caracteres.") String taxId,
        @Size(max = 100, message = "El nombre de contacto no puede superar los 100 caracteres.") String contactName,
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres.") String phone,
        @Size(max = 150, message = "El correo electrónico no puede superar los 150 caracteres.") String email,
        @Size(max = 200, message = "La dirección no puede superar los 200 caracteres.") String address,
        @Min(value = 0, message = "Los días de plazo de pago no pueden ser negativos.") Integer paymentTermsDays,
        @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres.") String notes) {
}
