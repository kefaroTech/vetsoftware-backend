package com.vetsoftware.app.securityincident.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.securityincident.application.port.out.CompanyValidationPort;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de esta rodaja que conoce a {@code company}.
 *
 * <p>
 * Se apoya en {@code existsById} y <strong>no lee un solo getter de la entidad
 * ajena</strong>: a la puente no le hace falta ningun campo de la clinica —la
 * archiva por id— y depender de la forma de una entidad de otra feature es como
 * un cambio inocente alli rompe esto. Es un {@code ValidationPort} y no un
 * {@code QueryPort} por eso mismo.
 *
 * <p>
 * <strong>Importar {@code CompanyJpaRepository} aqui es el unico cruce que el
 * vertical slicing permite</strong>, y solo desde
 * {@code infrastructure/persistence}. Lo que no se hace es colgar un
 * {@code @ManyToOne} a {@code CompanyJpaEntity} desde la entidad de la puente:
 * eso activaria las cuatro reglas de BE-COV sobre la feature entera.
 *
 * <p>
 * El nombre de bean va cualificado porque el vertical slicing repite nombres de
 * clase entre features: sin el, este {@code JpaCompanyValidationPort} y
 * cualquier otro colisionarian por nombre simple.
 */
@Component("securityIncidentJpaCompanyValidationPort")
public class JpaCompanyValidationPort implements CompanyValidationPort {

    private final CompanyJpaRepository companyJpaRepository;

    public JpaCompanyValidationPort(CompanyJpaRepository companyJpaRepository) {
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public boolean existsById(Long companyId) {
        return companyId != null && companyJpaRepository.existsById(companyId);
    }
}
