package com.vetsoftware.app.infrastructure.web;

import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.function.Function;

/**
 * La página tal como sale por HTTP: el ÚNICO contrato de paginación de la capa
 * web, y el nombre que ven los dos frontends en el OpenAPI
 * ({@code PageResponseAnimalResponse}, {@code PageResponseOwnerResponse}…).
 *
 * <p>
 * Es gemelo de {@link PageResult} a propósito: aquel es el de dentro y este el
 * de la frontera. Mantenerlos separados es lo que permite que un
 * {@code XxxResponse} cambie de forma sin tocar el caso de uso, y al revés. Lo
 * que no se sostiene es que cada feature declare los suyos: por eso hay
 * exactamente uno de cada, y {@code PAGINACION_CON_UN_SOLO_CONTRATO} (ArchUnit)
 * rompe el build si aparece un tercero.
 */
public record PageResponse<T>(List<T> content, int page, int pageSize, long totalElements,
        int totalPages) {

    /**
     * Traduce la página de la aplicación a la de la web mapeando cada elemento.
     *
     * <p>
     * Sustituye al bloque de cuatro líneas que cada controller repetía para
     * arrastrar los mismos cinco campos. El detalle que ese bloque se equivocaba en
     * silencio: los totales son los de la consulta, no los del contenido mapeado.
     */
    public static <S, T> PageResponse<T> from(PageResult<S> result,
            Function<? super S, ? extends T> mapper) {
        return new PageResponse<T>(result.content().stream().<T>map(mapper).toList(), result.page(),
                result.pageSize(), result.totalElements(), result.totalPages());
    }

    /** Misma traducción cuando el caso de uso ya devuelve el tipo de la web. */
    public static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(result.content(), result.page(), result.pageSize(),
                result.totalElements(), result.totalPages());
    }
}
