package com.vetsoftware.app.withholdingraterule.application.usecase;

import com.vetsoftware.app.withholdingraterule.application.command.CreateWithholdingRateRuleCommand;
import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import com.vetsoftware.app.withholdingraterule.application.port.in.CreateWithholdingRateRuleUseCase;
import com.vetsoftware.app.withholdingraterule.application.port.out.MunicipalityValidationPort;
import com.vetsoftware.app.withholdingraterule.application.port.out.WithholdingRateRuleRepository;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRule;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Da de alta una tarifa de retencion.
 *
 * <p>
 * <strong>El service hace exactamente dos cosas, y ninguna es validar la
 * tarifa.</strong> Comprueba el municipio —que es un hecho externo, hay que
 * preguntarselo a otra tabla— y sella la fecha de creacion con el reloj
 * inyectado. Todo lo demas —que el municipio solo venga con {@code ICA}, que el
 * porcentaje este entre cero y cien con seis decimales, que haya al menos una
 * base minima, que la vigencia no se cierre antes de abrirse— son invariantes y
 * viven en el constructor de {@link WithholdingRateRule}. Ahi no se pueden
 * saltar; aqui si, llamando al constructor desde otro sitio.
 *
 * <p>
 * <strong>La unicidad no se comprueba preguntando antes, y es a
 * proposito.</strong> Las dos que importan
 * —{@code uq_withholding_rate_rules_case} y
 * {@code uq_withholding_rate_rules_current}— las cuida la base sobre columnas
 * generadas. Un {@code exists} previo seria una comprobacion que dos peticiones
 * concurrentes pasarian las dos, y dejaria dos vigencias abiertas para el mismo
 * supuesto: justo lo que el changeset 317 se propuso cerrar. Aqui el duplicado
 * llega como violacion de integridad, que es la unica respuesta que no miente.
 */
@Observed(name = "withholding.rate.rule.create")
@Service
public class CreateWithholdingRateRuleService implements CreateWithholdingRateRuleUseCase {

    private final WithholdingRateRuleRepository repository;
    private final MunicipalityValidationPort municipalityValidationPort;
    private final Clock clock;

    public CreateWithholdingRateRuleService(WithholdingRateRuleRepository repository,
            MunicipalityValidationPort municipalityValidationPort, Clock clock) {
        this.repository = repository;
        this.municipalityValidationPort = municipalityValidationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public WithholdingRateRuleDto execute(CreateWithholdingRateRuleCommand command) {
        validateMunicipality(command);
        WithholdingRateRule rule = WithholdingRateRule.create(command.withholdingType(),
                command.serviceNature(), command.municipalityCode(), command.ratePercent(),
                command.minimumBaseAmount(), command.minimumBaseUvt(), command.legalReference(),
                command.validFrom(), command.validTo(), LocalDateTime.now(clock));
        return WithholdingRateRuleDto.from(repository.save(rule));
    }

    /**
     * El municipio es opcional —solo lo llevan las de {@code ICA}— pero si viene
     * tiene que existir en {@code cities.dane_code}, o la clave foranea lo
     * rechazaria mas tarde y como un error de integridad en vez de como el «ese
     * municipio no existe» que corresponde.
     *
     * <p>
     * Que el codigo solo sea legitimo para {@code ICA} no se comprueba aqui: lo
     * hace el dominio, y comprobarlo dos veces invitaria a que un dia solo quedara
     * la copia de este lado.
     */
    private void validateMunicipality(CreateWithholdingRateRuleCommand command) {
        if (command.municipalityCode() == null)
            return;
        if (!municipalityValidationPort.existsByDaneCode(command.municipalityCode()))
            throw new IllegalArgumentException(
                    "Municipality not found: " + command.municipalityCode());
    }
}
