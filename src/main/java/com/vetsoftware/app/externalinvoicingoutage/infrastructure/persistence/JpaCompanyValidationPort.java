package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.CompanyValidationPort;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce la tabla de {@code company}, y el
 * unico cruce de vertical slicing que la feature se permite:
 * {@code infrastructure/persistence} puede importar
 * {@code otraFeature.infrastructure.persistence.XxxJpaRepository}.
 *
 * <p>
 * <strong>Que sea este fichero y no la entidad es toda la diferencia.</strong>
 * Un {@code @ManyToOne} a {@code CompanyJpaEntity} en
 * {@link ExternalInvoicingOutageCompanyJpaEntity} haria que la feature entera
 * <em>alcanzara</em> companies y activaria las cuatro reglas duras de BE-COV
 * sobre las dos tablas, incluida la que no tiene empresa que acotar. Un import
 * de repositorio aqui no crea asociacion ninguna: no hay grafo que recorrer.
 *
 * <p>
 * <strong>{@code existsById} y no {@code findById}</strong>: la puente guarda
 * un {@code Long} y no usa un solo campo de la empresa, asi que traerse la fila
 * entera —con su {@code @EntityGraph} de ciudad— seria pagar un {@code JOIN}
 * para tirar el resultado.
 */
@Component("externalInvoicingOutageJpaCompanyValidationPort")
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
