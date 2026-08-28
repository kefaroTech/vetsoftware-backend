package com.vetsoftware.app.platformtaxprofile.infrastructure.persistence;

import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaRepository;
import com.vetsoftware.app.platformtaxprofile.application.port.out.EconomicActivityQueryPort;
import com.vetsoftware.app.platformtaxprofile.domain.EconomicActivityRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de esta feature que conoce la feature
 * {@code economicactivity}, que es el cruce que el vertical slicing permite:
 * solo {@code infrastructure/persistence} puede importar el
 * {@code XxxJpaRepository} de otra.
 *
 * <p>
 * <strong>El nombre del bean va cualificado a proposito.</strong>
 * {@code companytaxprofile} ya tiene una clase con este mismo nombre simple, y
 * el nombre que Spring deriva por defecto es el simple: sin el nombre
 * explicito, las dos en el mismo escaneo revientan el arranque con un
 * {@code ConflictingBeanDefinitionException} que no dice cual de las dos
 * features sobra. Es la misma precaucion que toma
 * {@code companyBillingProfileJpaCityQueryPort}.
 */
@Component("platformTaxProfileJpaEconomicActivityQueryPort")
public class JpaEconomicActivityQueryPort implements EconomicActivityQueryPort {

    private final EconomicActivityJpaRepository economicActivityJpaRepository;

    public JpaEconomicActivityQueryPort(
            EconomicActivityJpaRepository economicActivityJpaRepository) {
        this.economicActivityJpaRepository = economicActivityJpaRepository;
    }

    /**
     * <strong>Lee la actividad de verdad y no solo comprueba que existe</strong>,
     * que es lo que distingue un {@code QueryPort} de un {@code ValidationPort}: la
     * ficha sale con el codigo y el nombre de la actividad, asi que hace falta el
     * dato y no solo la respuesta.
     *
     * <p>
     * {@code EconomicActivityJpaEntity} lleva
     * {@code @SQLRestriction("enabled = true")}, asi que una actividad dada de baja
     * se comporta aqui como inexistente y el caso de uso contesta que no existe. Es
     * lo correcto: nadie deberia poder registrar la identidad fiscal de la
     * plataforma contra un CIIU retirado del catalogo.
     */
    @Override
    public Optional<EconomicActivityRef> findById(Long economicActivityId) {
        return economicActivityJpaRepository.findById(economicActivityId)
                .map(e -> new EconomicActivityRef(e.getId(), e.getCode(), e.getName()));
    }
}
