package com.vetsoftware.app.companybillingprofile.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.companybillingprofile.application.port.out.CityQueryPort;
import com.vetsoftware.app.companybillingprofile.domain.CityRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de esta feature que conoce la feature {@code city}, que es
 * el cruce que el vertical slicing permite: solo
 * {@code infrastructure/persistence} puede importar el {@code XxxJpaRepository}
 * de otra.
 *
 * <p>
 * <strong>El nombre del bean va cualificado a proposito.</strong> Hay al menos
 * cuatro clases llamadas {@code JpaCityQueryPort} en el arbol —{@code branch},
 * {@code company}, {@code owner} y esta—, y el nombre que Spring deriva por
 * defecto es el simple. Sin el nombre explicito, dos de ellas en el mismo
 * escaneo revientan el arranque con un
 * {@code ConflictingBeanDefinitionException} que no dice cual de las dos
 * features sobra.
 */
@Component("companyBillingProfileJpaCityQueryPort")
public class JpaCityQueryPort implements CityQueryPort {

    private final CityJpaRepository cityJpaRepository;

    public JpaCityQueryPort(CityJpaRepository cityJpaRepository) {
        this.cityJpaRepository = cityJpaRepository;
    }

    /**
     * <strong>Lee el municipio de verdad y no solo comprueba que existe</strong>,
     * que es lo que distingue un {@code QueryPort} de un {@code ValidationPort}: la
     * direccion de facturacion sale con el nombre del municipio, asi que hace falta
     * el dato y no solo la respuesta.
     *
     * <p>
     * {@code CityJpaEntity} lleva {@code @SQLRestriction("enabled = true")}, asi
     * que un municipio dado de baja se comporta aqui como inexistente y el caso de
     * uso contesta que no existe. Es lo correcto: nadie deberia poder abrir una
     * ficha nueva contra un municipio retirado del catalogo.
     */
    @Override
    public Optional<CityRef> findById(Long cityId) {
        return cityJpaRepository.findById(cityId).map(e -> new CityRef(e.getId(), e.getName()));
    }
}
