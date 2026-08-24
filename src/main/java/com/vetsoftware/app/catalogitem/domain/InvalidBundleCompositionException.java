package com.vetsoftware.app.catalogitem.domain;

/**
 * Lo que un {@code CHECK} no puede comprobar de {@code bundle_components}: que
 * el padre sea de tipo {@code BUNDLE} y que el componente no sea otro
 * {@code BUNDLE}.
 *
 * <p>
 * No es declarable en la base porque exige leer
 * {@code catalog_items.item_type}, y el manual de MySQL 8.4 excluye de un
 * {@code CHECK} las «columns in other tables». Baja al caso de uso, que ya
 * tiene los dos articulos cargados.
 *
 * <p>
 * Los paquetes anidados se rechazan porque el desglose de una cotizacion
 * tendria que recorrerse en profundidad y el precio de un pack dejaria de ser
 * la suma de sus lineas.
 */
public class InvalidBundleCompositionException extends RuntimeException {
    public InvalidBundleCompositionException(String message) {
        super(message);
    }
}
