package com.vetsoftware.app.withholdingraterule.application.usecase;

import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import com.vetsoftware.app.withholdingraterule.application.port.in.FindWithholdingRateRuleUseCase;
import com.vetsoftware.app.withholdingraterule.application.port.out.WithholdingRateRuleRepository;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Una tarifa por su id.
 *
 * <p>
 * <strong>El {@code companyId} no se usa en el cuerpo, y eso es
 * correcto.</strong> Se gasta entero en el {@code @PreAuthorize} del puerto
 * ({@code @authz.isMyCompany(#companyId)}), que comprueba que quien pregunta
 * declara su propia empresa; despues no queda nada que filtrar, porque
 * {@code withholding_rate_rules} no tiene columna de empresa y la tarifa de un
 * supuesto fiscal es la misma para todas las clinicas.
 *
 * <p>
 * Es el parametro que un {@code find usages} da por muerto. No lo esta:
 * quitarlo elimina la mitad del gate y deja el permiso
 * {@code withholdingRateRule.read} alcanzable por un empleado de cualquier
 * empresa. El razonamiento completo esta en
 * {@link FindWithholdingRateRuleUseCase#findById(Long, Long)}.
 */
@Observed(name = "withholding.rate.rule.find")
@Service
public class FindWithholdingRateRuleService implements FindWithholdingRateRuleUseCase {

    private final WithholdingRateRuleRepository repository;

    public FindWithholdingRateRuleService(WithholdingRateRuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public WithholdingRateRuleDto findById(Long id, Long companyId) {
        return repository.findById(id).map(WithholdingRateRuleDto::from)
                .orElseThrow(() -> new WithholdingRateRuleNotFoundException(id));
    }
}
