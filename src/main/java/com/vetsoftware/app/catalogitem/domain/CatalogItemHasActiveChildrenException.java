package com.vetsoftware.app.catalogitem.domain;

/**
 * Impide desactivar un articulo del que todavia cuelgan filas activas.
 *
 * <p>
 * Las FK del modelo son {@code ON DELETE RESTRICT}, pero el borrado de este
 * slice es <em>logico</em>: la fila no se va, asi que la base no dice nada. Sin
 * esta comprobacion, dar de baja un articulo deja sus puentes, sus dependencias
 * y sus componentes apuntando a algo que la aplicacion ya no ve — y el
 * configurador resolveria dependencias contra un articulo retirado.
 */
public class CatalogItemHasActiveChildrenException extends RuntimeException {
    public CatalogItemHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete catalog item " + id + ": has active " + childType + " children");
    }
}
