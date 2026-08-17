package com.vetsoftware.app.shared.pagination;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados: el ÚNICO contrato de paginación de la capa de
 * aplicación.
 *
 * <p>
 * Vive en {@code shared} por el mismo criterio que
 * {@link com.vetsoftware.app.shared.domain.Money}: es un tipo <em>sin semántica
 * de negocio</em>. «Una lista con su total y su número de página» no significa
 * nada distinto en animales que en facturas, así que duplicarlo por feature no
 * protege ningún límite — solo obliga a escribir treinta y seis veces cada
 * regla transversal que toque un listado. El vertical slicing justifica
 * duplicar tipos de dominio; no justifica duplicar infraestructura de
 * colección.
 *
 * <p>
 * Es deliberadamente pobre: cero Spring, cero JPA. Lo que necesita el
 * {@code Page} de Spring Data vive en {@link Pages}, del lado de
 * infraestructura, para que un caso de uso pueda devolver una página sin
 * arrastrar el framework a {@code application}.
 *
 * @param content
 *            los elementos de esta página (inmutable; nunca {@code null})
 * @param page
 *            índice de página, base 0
 * @param pageSize
 *            tamaño de página efectivo, ya acotado por {@link Pages}
 * @param totalElements
 *            filas totales que satisfacen la consulta, no las de esta página
 * @param totalPages
 *            páginas totales con ese tamaño
 */
public record PageResult<T>(List<T> content, int page, int pageSize, long totalElements,
        int totalPages) {

    public PageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }

    /**
     * Transforma el contenido conservando intactos los metadatos de paginación.
     *
     * <p>
     * Es el único comportamiento del tipo, y el que hace que un caso de uso pueda
     * escribir {@code repository.findAll(...).map(AnimalDto::from)} sin recalcular
     * totales: recalcularlos sobre el contenido ya paginado es como se acaba
     * reportando «20 de 20» en un listado de cincuenta mil.
     */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<R>(content.stream().<R>map(mapper).toList(), page, pageSize,
                totalElements, totalPages);
    }

    /**
     * Página construida a mano: deriva {@code totalPages} del total y el tamaño en
     * lugar de aceptarlo como dato suelto. Para lo que ya viene de Spring Data usa
     * {@link Pages#result}, que toma los metadatos del propio {@code Page}.
     */
    public static <T> PageResult<T> of(List<T> content, int page, int pageSize,
            long totalElements) {
        int safeSize = pageSize <= 0 ? Pages.DEFAULT_SIZE : pageSize;
        int totalPages = (int) ((totalElements + safeSize - 1) / safeSize);
        return new PageResult<>(content, page, safeSize, totalElements, totalPages);
    }

    /** Página vacía que conserva la posición pedida: 0 elementos, 0 páginas. */
    public static <T> PageResult<T> empty(int page, int pageSize) {
        return new PageResult<>(List.of(), page, pageSize, 0L, 0);
    }
}
