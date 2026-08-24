package com.vetsoftware.app.submodule.infrastructure.web.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code sellable} y {@code readOnlyCapable} son primitivos a proposito: un
 * cuerpo que los omita los deja en {@code false}, que es exactamente el default
 * seguro de la especificacion —«no se vende» y «no sabe funcionar en solo
 * lectura»—.
 */
public record CreateSubModuleRequest(
        @NotBlank(message = "El nombre del submódulo es obligatorio.") @Size(max = 100, message = "El nombre del submódulo no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "El código del submódulo es obligatorio.") @Size(max = 50, message = "El código del submódulo no puede superar los 50 caracteres.") String code,
        @NotNull(message = "Debes seleccionar el módulo.") Long moduleId,
        // Jackson 3 trae FAIL_ON_NULL_FOR_PRIMITIVES ACTIVADO (al reves que Jackson 2):
        // sin @JsonSetter, omitir la bandera responde 400 «Cannot map `null` into type
        // `boolean`» en vez de caer al default seguro que declara la especificacion.
        // Mismo patron que CreateAppointmentRequest.forceOverlap.
        @JsonSetter(nulls = Nulls.AS_EMPTY) boolean sellable,
        @JsonSetter(nulls = Nulls.AS_EMPTY) boolean readOnlyCapable) {
}
