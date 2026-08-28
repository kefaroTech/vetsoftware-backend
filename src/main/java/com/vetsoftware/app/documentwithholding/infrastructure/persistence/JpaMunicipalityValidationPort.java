package com.vetsoftware.app.documentwithholding.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.documentwithholding.application.port.out.MunicipalityValidationPort;
import org.springframework.stereotype.Component;

/**
 * Existencia del municipio por su codigo DIVIPOLA.
 *
 * <p>
 * <strong>{@code existsByDaneCode} vive en {@code CityJpaRepository} y no
 * aqui</strong>, y esa decision es de la otra feature: el
 * {@code XxxJpaRepository} de una feature se declara una sola vez, asi que dos
 * slices fiscales anadiendole el mismo metodo derivado serian un conflicto de
 * escritura sobre el mismo archivo, no dos metodos. Este adaptador lo consume y
 * no lo redeclara.
 *
 * <p>
 * <strong>Sin empresa, y la FK tampoco es compuesta.</strong> La geografia es
 * un catalogo global: un municipio no pertenece a ninguna clinica.
 */
@Component("documentWithholdingJpaMunicipalityValidationPort")
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
