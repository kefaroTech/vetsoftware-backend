package com.vetsoftware.app.revenuerecognitionline.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.revenuerecognitionline.application.dto.RevenueRecognitionLineDto;
import com.vetsoftware.app.revenuerecognitionline.application.port.out.RevenueRecognitionLineRepository;
import com.vetsoftware.app.revenuerecognitionline.domain.RevenueRecognitionLine;
import com.vetsoftware.app.revenuerecognitionline.testsupport.RevenueRecognitionLineMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Los dos listados y el reparto entre ellos es la decision: uno filtra de
 * verdad por empresa, el otro es el barrido de plataforma del cierre mensual.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListRevenueRecognitionLinesService")
class ListRevenueRecognitionLinesServiceTest {

    @Mock
    private RevenueRecognitionLineRepository repository;

    @InjectMocks
    private ListRevenueRecognitionLinesService service;

    @Nested
    @DisplayName("listByCompany")
    class PorEmpresa {

        @Test
        @DisplayName("filtra por companyId en el WHERE del adaptador y traduce a DTO")
        void filtra_por_company_id_y_traduce_a_dto() {
            PageResult<RevenueRecognitionLine> pagina = new PageResult<>(
                    List.of(RevenueRecognitionLineMother.renglon()), 0, 20, 1L, 1);
            when(repository.findAllByCompanyId(RevenueRecognitionLineMother.COMPANY_ID, 0, 20))
                    .thenReturn(pagina);

            PageResult<RevenueRecognitionLineDto> resultado = service
                    .listByCompany(RevenueRecognitionLineMother.COMPANY_ID, 0, 20);

            assertThat(resultado.content()).extracting(RevenueRecognitionLineDto::id)
                    .containsExactly(RevenueRecognitionLineMother.LINE_ID);
            assertThat(resultado.totalElements()).isEqualTo(1L);
            verify(repository).findAllByCompanyId(RevenueRecognitionLineMother.COMPANY_ID, 0, 20);
            verifyNoMoreInteractions(repository);
        }
    }

    @Nested
    @DisplayName("listByPostingPeriod")
    class PorPeriodoContable {

        @Test
        @DisplayName("barre todas las clinicas del periodo contable, sin acotar por empresa")
        void barre_todas_las_clinicas_del_periodo_contable() {
            PageResult<RevenueRecognitionLine> pagina = new PageResult<>(
                    List.of(RevenueRecognitionLineMother.renglon()), 0, 20, 1L, 1);
            when(repository.findAllByPostingPeriod(RevenueRecognitionLineMother.POSTING_PERIOD, 0,
                    20)).thenReturn(pagina);

            PageResult<RevenueRecognitionLineDto> resultado = service
                    .listByPostingPeriod(RevenueRecognitionLineMother.POSTING_PERIOD, 0, 20);

            assertThat(resultado.content()).extracting(RevenueRecognitionLineDto::postingPeriod)
                    .containsExactly(RevenueRecognitionLineMother.POSTING_PERIOD);
            verify(repository).findAllByPostingPeriod(RevenueRecognitionLineMother.POSTING_PERIOD,
                    0, 20);
            verifyNoMoreInteractions(repository);
        }
    }
}
