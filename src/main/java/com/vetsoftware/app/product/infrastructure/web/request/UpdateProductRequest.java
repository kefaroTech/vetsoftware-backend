package com.vetsoftware.app.product.infrastructure.web.request;

import com.vetsoftware.app.product.domain.TaxTreatment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "El nombre del producto es obligatorio.") @Size(max = 100, message = "El nombre del producto no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "El código del producto es obligatorio.") @Size(max = 50, message = "El código del producto no puede superar los 50 caracteres.") String code,
        @NotNull(message = "El precio de venta es obligatorio.") @DecimalMin(value = "0.0", message = "El precio de venta no puede ser negativo.") BigDecimal salePrice,
        @Size(max = 10, message = "El código de la unidad de medida no puede superar los 10 caracteres.") String baseUnitMeasureCode,
        @Size(max = 150, message = "El proveedor no puede superar los 150 caracteres.") String provider,
        Long supplierId,
        @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres.") String notes,
        @NotNull(message = "Debes indicar el tratamiento de impuestos.") TaxTreatment taxTreatment,
        @NotNull(message = "Debes seleccionar una categoría de producto.") Long productCategoryId,
        Long taxId,
        // La version del bloqueo optimista no la teclea nadie: la manda el front tal
        // como la recibio en el GET. Si falta es que el formulario se cargo mal, y el
        // mensaje tiene que decir que hacer, no describir el campo.
        @NotNull(message = "No se pudo identificar la versión del producto. Vuelve a cargarlo e inténtalo de nuevo.") Long version) {
}
