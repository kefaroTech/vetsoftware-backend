package com.vetsoftware.app.taxreturn.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.taxreturn.application.port.out.MunicipalityValidationPort;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code city}.
 *
 * <p>
 * Se apoya en {@code existsByDaneCode} —el mismo derivado que ya usan
 * {@code withholdingraterule} y {@code documentwithholding}— y no lee un solo
 * getter de la entidad ajena: a esta declaracion no le hace falta ningun campo
 * del municipio, se archiva bajo su codigo DIVIPOLA.
 *
 * <p>
 * El nombre de bean va cualificado porque el vertical slicing repite nombres de
 * clase entre features: sin el, los tres {@code JpaMunicipalityValidationPort}
 * del bloque fiscal colisionarian por nombre simple.
 */
@Component("taxReturnJpaMunicipalityValidationPort")
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
