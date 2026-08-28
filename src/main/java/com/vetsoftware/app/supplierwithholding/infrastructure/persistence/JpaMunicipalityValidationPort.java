package com.vetsoftware.app.supplierwithholding.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.supplierwithholding.application.port.out.MunicipalityValidationPort;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code city}.
 *
 * <p>
 * Se apoya en {@code existsByDaneCode} —el mismo derivado que ya usan
 * {@code withholdingraterule}, {@code documentwithholding} y {@code taxreturn}—
 * y no lee un solo getter de la entidad ajena.
 *
 * <p>
 * El nombre de bean va cualificado porque el vertical slicing repite nombres de
 * clase entre features: sin el, los cuatro
 * {@code JpaMunicipalityValidationPort} del bloque fiscal colisionarian por
 * nombre simple.
 */
@Component("supplierWithholdingJpaMunicipalityValidationPort")
public class JpaMunicipalityValidationPort implements MunicipalityValidationPort {

    private final CityJpaRepository cityJpaRepository;

    public JpaMunicipalityValidationPort(CityJpaRepository cityJpaRepository) {
        this.cityJpaRepository = cityJpaRepository;
    }

    @Override
    public boolean existsByDaneCode(String daneCode) {
        return daneCode != null && cityJpaRepository.existsByDaneCode(daneCode);
    }
}
