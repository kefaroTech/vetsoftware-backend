package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Los medicamentos GLOBALES activos, que es lo que administra la consola de
 * plataforma. No filtra por empresa —no hay ninguna que filtrar: son las filas
 * con {@code company_id} nulo—, asi que va cerrado a {@code hasRole('SYSTEM')}
 * a secas, como exige {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
 *
 * <p>
 * Distinto de {@link ListMedicamentsUseCase#listAll(String, int, int)}, que
 * devuelve TODO el catalogo —globales mas privados de cada empresa— y sirve
 * para dar contexto. Aquel no se duplica aqui: ya es SYSTEM y se reusa tal
 * cual.
 *
 * <p>
 * {@code q} es OPCIONAL: {@code null} o en blanco devuelve el listado completo,
 * que es como se comportaba antes de existir la busqueda. Filtra por SUBCADENA
 * del nombre —no por prefijo— y la comparacion la resuelve la collation de la
 * columna, insensible a caja y acentos. Va en el servidor y no en el navegador
 * porque el cliente solo tiene la pagina que esta viendo: con 153 moleculas y
 * paginas de 20, un filtro en cliente diria «no existe» sobre algo que esta en
 * la pagina 6 y el operador crearia un duplicado.
 */
public interface ListGlobalMedicamentsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<MedicamentDto> listAll(String q, int page, int pageSize);
}
