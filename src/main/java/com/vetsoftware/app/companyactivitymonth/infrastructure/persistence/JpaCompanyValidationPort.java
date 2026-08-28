package com.vetsoftware.app.companyactivitymonth.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.companyactivitymonth.application.port.out.CompanyValidationPort;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de esta rodaja que conoce la tabla de {@code company}.
 *
 * <p>
 * Es el cruce de vertical slicing permitido —{@code infrastructure/persistence}
 * puede importar el {@code XxxJpaRepository} de otra feature— y esta acotado a
 * lo minimo: {@code existsById}, una sola consulta que no trae ninguna columna
 * del agregado ajeno. No hay companion VO porque esta feature no usa ni un dato
 * de la empresa; ver {@link CompanyValidationPort}.
 *
 * <p>
 * <strong>Esto no convierte a la rodaja en tenant-aware.</strong>
 * {@code CompanyActivityMonthJpaEntity} sigue sin alcanzar
 * {@code CompanyJpaEntity} por ninguna asociacion: aqui se usa el repositorio
 * de la otra feature, no se cuelga un {@code @ManyToOne}.
 */
@Component("companyActivityMonthCompanyValidationPort")
public class JpaCompanyValidationPort implements CompanyValidationPort {

    private final CompanyJpaRepository companyJpaRepository;

    public JpaCompanyValidationPort(CompanyJpaRepository companyJpaRepository) {
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public boolean existsById(Long companyId) {
        return companyJpaRepository.existsById(companyId);
    }
}
