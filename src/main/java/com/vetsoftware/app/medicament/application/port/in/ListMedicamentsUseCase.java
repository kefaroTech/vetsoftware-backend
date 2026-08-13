package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.dto.PageResult;
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
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<MedicamentDto> listAll(int page, int pageSize);
}
