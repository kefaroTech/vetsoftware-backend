package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.ReactivateCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyAuditPort;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Devuelve al registro una empresa archivada.
 *
 * <p>
 * <b>Reactiva con un {@code UPDATE} y no con un {@code findById} + {@code
 * save}</b>, y no es una optimizacion:
 * {@code @SQLRestriction("enabled = true")} hace que una empresa archivada
 * <b>no exista</b> para ninguna consulta JPA de la entidad, {@code findById}
 * incluido. Leerla primero para modificarla es literalmente imposible; el
 * camino es el mismo de {@code ReactivateCityService} y sus hermanos.
 *
 * <p>
 * <b>Por eso el «no existe» se decide contando filas.</b> Cero filas
 * actualizadas significa o que el id no existe o que la empresa ya estaba
 * activa, y las dos respuestas son la misma para quien llama: no hay nada que
 * restaurar. El {@code findById} de despues ya ve la fila —acaba de quedar
 * {@code enabled = true}— y es el que devuelve la ficha con su version nueva.
 */
@Observed(name = "company.reactivate")
@Service
public class ReactivateCompanyService implements ReactivateCompanyUseCase {

    private final CompanyRepository repository;
    private final CompanyAuditPort audit;

    public ReactivateCompanyService(CompanyRepository repository, CompanyAuditPort audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Override
    @Transactional
    public CompanyDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new CompanyNotFoundException(id);
        CompanyDto dto = CompanyDto
                .from(repository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id)));
        audit.companyReactivated(dto.id(), dto.name(), dto.identifier());
        return dto;
    }
}
