package com.vetsoftware.app.withholdingraterule.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.withholdingraterule.application.port.out.MunicipalityValidationPort;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code city}.
 *
 * <p>
 * Se apoya en {@code existsByDaneCode} y no lee un solo getter de la entidad
 * ajena: a esta tarifa no le hace falta ningun campo del municipio —se archiva
 * bajo su codigo DIVIPOLA— y depender de la forma de una entidad de otra
 * feature es como un cambio inocente alli rompe esto.
 *
 * <p>
 * <strong>El metodo derivado vive en {@code CityJpaRepository} y no aqui, y hay
 * un motivo de coordinacion:</strong> el {@code XxxJpaRepository} de una
 * feature se declara una sola vez, asi que dos slices fiscales anadiendole el
 * mismo metodo serian un conflicto de escritura sobre ese archivo y no dos
 * metodos. {@code document_withholdings} apunta a la misma columna y usa el
 * mismo derivado.
 *
 * <p>
 * El nombre de bean va cualificado porque el vertical slicing repite nombres de
 * clase entre features: sin el, dos {@code JpaMunicipalityValidationPort} en
 * paquetes distintos colisionarian por nombre simple.
 */
@Component("withholdingRateRuleJpaMunicipalityValidationPort")
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
