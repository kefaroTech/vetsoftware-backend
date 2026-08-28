package com.vetsoftware.app.accountingperiod.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * <strong>Sin {@code companyId}, y aqui no hay ninguno que pudiera ir.</strong>
 * La regla dura {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} mira todo
 * {@code @RequestBody} sin mirar la ruta ni el rol, pero en este caso ni
 * siquiera hace falta invocarla: la tabla no tiene empresa porque el calendario
 * contable es de la plataforma.
 *
 * <p>
 * <strong>Sin {@code closedBySystemUserId} ni ningun otro campo de
 * firma.</strong> Un mes nace abierto: dejar que el cuerpo trajera un estado
 * inicial permitiria crear un mes ya cerrado, y dejar que trajera la firma
 * permitiria cerrar en nombre de otro.
 *
 * @param periodKey
 *            el {@code @Pattern} repite el {@code CHECK} del esquema <strong>a
 *            proposito</strong>, aunque el value object del dominio lo vuelva a
 *            comprobar. Sin el, un {@code 2026-13} saldria como el
 *            {@code IllegalArgumentException} generico del dominio —un 400 con
 *            otra forma— en vez del error de campo que el front sabe pintar
 *            bajo el input. Con el, el binder lo caza nombrando el campo. La
 *            red de abajo sigue siendo la que manda: el {@code @Valid} solo
 *            protege la entrada por HTTP, y hay callers que no pasan por aqui
 */
public record OpenAccountingPeriodRequest(
        @NotBlank(message = "Debes indicar el mes contable.") @Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "El mes contable debe tener la forma aaaa-MM, por ejemplo 2026-03.") String periodKey) {
}
