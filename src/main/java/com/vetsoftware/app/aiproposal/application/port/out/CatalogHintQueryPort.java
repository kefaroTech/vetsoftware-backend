package com.vetsoftware.app.aiproposal.application.port.out;

import java.util.Map;

/**
 * Los hints que el prompt le ensena al modelo sobre cada articulo, por codigo.
 *
 * <p>
 * <strong>No son {@code short_description} ni
 * {@code long_description}</strong>, y hay tres motivos independientes (plan
 * S5.2): esas dos son <em>copy de cliente</em> —las sirve
 * {@code GET /catalog}—, el acoplamiento duele en las dos direcciones
 * —marketing retoca una frase y el golden set queda invalido sin que nadie lo
 * note— y no hay donde versionar, porque {@code catalog_items.version} es el
 * bloqueo optimista de la fila entera.
 *
 * <p>
 * ⛔ <strong>Un mapa vacio es un estado LEGITIMO, no un fallo.</strong> El
 * changeset 382 condiciona su {@code INSERT} a que exista un
 * {@code system_users} habilitado —{@code published_by_system_user_id} es
 * {@code NOT NULL} con FK— y en una base recien migrada esa tabla esta vacia,
 * <strong>incluida la de Testcontainers que corre {@code mvn verify}</strong>.
 * El changeset no inserta nada, en silencio y a proposito. La feature nace muda
 * hasta que alguien publique una cuenta de sistema real.
 *
 * <p>
 * <strong>Y mudo significa degradar, no improvisar.</strong> Rellenar el hueco
 * con {@code short_description} seria reintroducir por la puerta de atras
 * exactamente el acoplamiento que S5.2 rechaza, y con un agravante: el prompt
 * pareceria completo. Sin hints se responde por el camino determinista y se
 * dice.
 */
public interface CatalogHintQueryPort {

    /**
     * La revision vigente de cada articulo, por codigo. Solo las no superadas
     * ({@code superseded_at IS NULL}), que es lo que garantiza
     * {@code uq_catalog_item_ai_hints_current}.
     */
    Map<String, String> findCurrentHints();
}
