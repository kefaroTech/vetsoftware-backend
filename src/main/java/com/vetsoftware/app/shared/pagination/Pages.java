package com.vetsoftware.app.shared.pagination;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * El puente entre Spring Data y {@link PageResult}, y el único sitio donde se
 * acota lo que pide el cliente.
 *
 * <p>
 * Los dos métodos existen porque los treinta adaptadores JPA escribían lo mismo
 * a mano, y no todos igual: unos normalizaban el índice negativo y topaban el
 * tamaño, y otros pasaban los parámetros crudos a {@code PageRequest.of}. Esa
 * diferencia no es de estilo — sin tope, {@code ?pageSize=100000} devuelve la
 * tabla entera y deshace el trabajo de paginar; sin normalizar,
 * {@code ?page=-1} revienta con {@code IllegalArgumentException} desde dentro
 * de Spring Data.
 *
 * <p>
 * Vive en {@code shared/pagination} junto al contrato al que sirve, pero solo
 * lo importa {@code infrastructure/persistence}: la capa de aplicación se queda
 * con {@link PageResult}, que no conoce el framework.
 */
public final class Pages {

    /** Tamaño de página cuando el cliente no pide uno válido. */
    public static final int DEFAULT_SIZE = 20;

    /**
     * Tope duro de filas por página. No es un límite de UI: es lo que impide que un
     * query param convierta un listado paginado en un {@code SELECT *}.
     */
    public static final int MAX_SIZE = 200;

    private Pages() {
    }

    /** Página normalizada y acotada, sin orden explícito. */
    public static PageRequest request(int page, int pageSize) {
        return PageRequest.of(normalizePage(page), normalizeSize(pageSize));
    }

    /**
     * Página normalizada y acotada con su orden. El orden lo decide cada adaptador
     * —un censo se lee por nombre, un historial por fecha— pero siempre debe ser
     * total: sin un desempate estable, dos páginas consecutivas pueden repetir u
     * omitir filas.
     */
    public static PageRequest request(int page, int pageSize, Sort sort) {
        return PageRequest.of(normalizePage(page), normalizeSize(pageSize), sort);
    }

    /** Índice de página utilizable: nunca negativo. */
    public static int normalizePage(int page) {
        return Math.max(page, 0);
    }

    /**
     * Tamaño de página utilizable: por defecto si no es válido, tope si se pasa.
     */
    public static int normalizeSize(int pageSize) {
        return pageSize <= 0 ? DEFAULT_SIZE : Math.min(pageSize, MAX_SIZE);
    }

    /**
     * Convierte la página de Spring Data en la de la aplicación aplicando el mapeo
     * de cada elemento. Los metadatos salen del {@code Page}, que ya refleja el
     * tamaño acotado por {@link #request}.
     */
    public static <E, D> PageResult<D> result(Page<E> page,
            Function<? super E, ? extends D> mapper) {
        return new PageResult<D>(page.getContent().stream().<D>map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Misma conversión cuando el contenido no sale de mapear la página elemento a
     * elemento —el caso de las consultas en dos pasos, que paginan ids y luego
     * cargan las filas—: los metadatos siguen viniendo del {@code Page} de ids.
     */
    public static <D> PageResult<D> result(Page<?> page, List<D> content) {
        return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
