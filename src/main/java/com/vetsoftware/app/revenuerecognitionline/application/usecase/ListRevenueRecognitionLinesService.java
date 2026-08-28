package com.vetsoftware.app.revenuerecognitionline.application.usecase;

import com.vetsoftware.app.revenuerecognitionline.application.dto.RevenueRecognitionLineDto;
import com.vetsoftware.app.revenuerecognitionline.application.port.in.ListRevenueRecognitionLinesUseCase;
import com.vetsoftware.app.revenuerecognitionline.application.port.out.RevenueRecognitionLineRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Los dos listados del libro, y el reparto entre ellos es la decision.
 *
 * <ul>
 * <li>{@link #listByCompany(Long, int, int)} <b>filtra de verdad</b> por
 * empresa: el {@code companyId} llega al {@code WHERE} del adaptador. Sirve a
 * {@code ix_rrl_posting (company_id, posting_period)}.</li>
 * <li>{@link #listByPostingPeriod(String, int, int)} no filtra por ninguna: es
 * el cierre mensual de todas las clinicas, sirve a {@code ix_rrl_period} —que
 * no lleva la empresa delante a proposito— y por eso su puerto va cerrado a
 * {@code hasRole('SYSTEM')} a secas, la unica salida que admite
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.</li>
 * </ul>
 */
@Observed(name = "revenue.recognition.list")
@Service
public class ListRevenueRecognitionLinesService implements ListRevenueRecognitionLinesUseCase {

    private final RevenueRecognitionLineRepository repository;

    public ListRevenueRecognitionLinesService(RevenueRecognitionLineRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<RevenueRecognitionLineDto> listByCompany(Long companyId, int page,
            int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(RevenueRecognitionLineDto::from);
    }

    @Override
    public PageResult<RevenueRecognitionLineDto> listByPostingPeriod(String postingPeriod, int page,
            int pageSize) {
        return repository.findAllByPostingPeriod(postingPeriod, page, pageSize)
                .map(RevenueRecognitionLineDto::from);
    }
}
