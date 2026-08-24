package com.vetsoftware.app.configurator.application.dto;

/**
 * Estado de una fila <strong>ignorando el borrado lógico</strong>: su id y si
 * sigue activa.
 *
 * <p>
 * Existe por la misma trampa que documenta su gemelo de {@code catalogitem}, y
 * lleva su mismo nombre a propósito: las tres tablas del configurador llevan
 * {@code enabled} con {@code @SQLRestriction} y una UNIQUE que <em>no</em>
 * incluye esa columna. Al dar de baja una pregunta, una opción o un efecto la
 * fila no se va: queda invisible para la aplicación y sigue ocupando la clave
 * única. Volver a crear el mismo código con un {@code INSERT} choca contra una
 * fila que nadie puede ver, y lo que le llega al administrador es una violación
 * de integridad sin explicación posible — un 409 con
 * {@code DATA_INTEGRITY_VIOLATION} y el detalle genérico
 * {@code Database constraint violation}.
 *
 * <p>
 * Los casos de uso de alta consultan primero este estado y
 * <strong>reactivan</strong> la fila en vez de insertar otra. Los adaptadores
 * lo resuelven con consulta nativa, que es la única forma de esquivar el
 * {@code @SQLRestriction} de la entidad.
 *
 * @see com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository#findAnyByCode(String)
 */
public record LinkStateDto(Long id, boolean enabled) {
}
