package com.vetsoftware.app.catalogitem.application.dto;

/**
 * Estado de una fila puente <strong>ignorando el borrado lógico</strong>: su id
 * y si sigue activa.
 *
 * <p>
 * Existe por una trampa concreta de este slice. Las tres tablas puente llevan
 * {@code enabled} con {@code @SQLRestriction} y una UNIQUE sobre el par (o la
 * terna) de claves foráneas. Al dar de baja un vínculo la fila <em>no se
 * va</em>: queda invisible para la aplicación y sigue ocupando la clave única.
 * Volver a crear el mismo vínculo con un {@code INSERT} choca contra una fila
 * que nadie puede ver, y lo que le llega al administrador del catálogo es una
 * violación de integridad sin explicación posible.
 *
 * <p>
 * Los tres casos de uso de alta consultan primero este estado y
 * <strong>reactivan</strong> la fila en vez de insertar otra. Los adaptadores
 * lo resuelven con consulta nativa, que es la única forma de esquivar el
 * {@code @SQLRestriction} de la entidad.
 */
public record LinkStateDto(Long id, boolean enabled) {
}
