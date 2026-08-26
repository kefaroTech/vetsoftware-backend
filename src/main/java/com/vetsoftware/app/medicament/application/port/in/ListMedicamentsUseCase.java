package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListMedicamentsUseCase {
    /**
     * Catalogo GLOBAL, de todas las empresas. Solo {@code ROLE_SYSTEM}.
     *
     * <p>
     * Antes tambien lo abria {@code prescription.read}, que es un permiso de
     * empleado: cualquier veterinario podia listar los medicamentos privados de
     * OTRAS empresas, porque este listado no filtra por tenant. Lo que un tenant
     * necesita es {@code /medicaments/available}, que devuelve los generales mas
     * los suyos y si filtra.
     *
     * <p>
     * Pagina ademas porque sin tope trae la tabla entera de la plataforma.
     *
     * <p>
     * {@code q} es OPCIONAL: {@code null} o en blanco devuelve el listado completo,
     * que es como se comportaba antes de existir la busqueda. Filtra por SUBCADENA
     * del nombre —no por prefijo— y la comparacion la resuelve la collation de la
     * columna, insensible a caja y acentos. Va en el servidor y no en el navegador
     * porque el cliente solo tiene la pagina que esta viendo: un filtro en cliente
     * diria «no existe» sobre algo que esta en la pagina 6, y el operador crearia
     * un duplicado.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<MedicamentDto> listAll(String q, int page, int pageSize);
}
