package com.vetsoftware.app.catalogitemaihint.domain;

/**
 * Se intento cerrar la vigencia de una revision que ya estaba cerrada.
 *
 * <p>
 * Lo lanza el propio agregado, asi que cubre tambien el camino que ninguna
 * consulta previa puede ver: dos peticiones simultaneas que cargan la misma
 * revision vigente. La primera la sucede; la segunda choca aqui o, si llega mas
 * lejos, contra el {@code @Version} de la fila.
 */
public class CatalogItemAiHintAlreadySupersededException extends RuntimeException {

    public CatalogItemAiHintAlreadySupersededException(Long catalogItemId, int hintRevision) {
        super("AI hint revision " + hintRevision + " of catalog item " + catalogItemId
                + " was already superseded");
    }
}
