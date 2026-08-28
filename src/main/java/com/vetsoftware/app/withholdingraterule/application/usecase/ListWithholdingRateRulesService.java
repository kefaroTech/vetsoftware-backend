package com.vetsoftware.app.withholdingraterule.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import com.vetsoftware.app.withholdingraterule.application.port.in.ListWithholdingRateRulesUseCase;
import com.vetsoftware.app.withholdingraterule.application.port.out.WithholdingRateRuleRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * El catalogo de tarifas, paginado.
 *
 * <p>
 * <strong>No hay un {@code ListAllWithholdingRateRulesUseCase} hermano, y no es
 * un olvido.</strong> En las demas features el par existe porque el listado del
 * tenant esta acotado por empresa y el de plataforma no; aqui los dos serian la
 * misma consulta sobre la misma tabla sin empresa. Duplicarlo solo repartiria
 * el mismo catalogo en dos endpoints con dos gates distintos que mantener
 * sincronizados.
 *
 * <p>
 * El {@code companyId} entra, autoriza en el puerto y no llega al repositorio:
 * ver {@link ListWithholdingRateRulesUseCase#listAvailable(Long, int, int)}.
 */
@Observed(name = "withholding.rate.rule.list")
@Service
public class ListWithholdingRateRulesService implements ListWithholdingRateRulesUseCase {

    private final WithholdingRateRuleRepository repository;

    public ListWithholdingRateRulesService(WithholdingRateRuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<WithholdingRateRuleDto> listAvailable(Long companyId, int page,
            int pageSize) {
        return repository.findAllEnabled(page, pageSize).map(WithholdingRateRuleDto::from);
    }
}
