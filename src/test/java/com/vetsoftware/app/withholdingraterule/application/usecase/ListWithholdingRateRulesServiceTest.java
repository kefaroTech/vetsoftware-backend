package com.vetsoftware.app.withholdingraterule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import com.vetsoftware.app.withholdingraterule.application.port.out.WithholdingRateRuleRepository;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRule;
import com.vetsoftware.app.withholdingraterule.testsupport.WithholdingRateRuleMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListWithholdingRateRulesService — el catalogo que lee el tenant")
class ListWithholdingRateRulesServiceTest {

    private static final Long EMPRESA_DEL_TOKEN = 77L;

    @Mock
    private WithholdingRateRuleRepository repository;

    @InjectMocks
    private ListWithholdingRateRulesService service;

    @Nested
    @DisplayName("Listado")
    class Listado {

        @Test
        @DisplayName("conserva los totales de la consulta y no los del contenido mapeado")
        void conserva_los_totales_de_la_consulta() {
            PageResult<WithholdingRateRule> pagina = PageResult.of(
                    List.of(WithholdingRateRuleMother.nacional(), WithholdingRateRuleMother.ica()),
                    2, 5, 11L);
            when(repository.findAllEnabled(2, 5)).thenReturn(pagina);

            PageResult<WithholdingRateRuleDto> resultado = service.listAvailable(EMPRESA_DEL_TOKEN,
                    2, 5);

            assertThat(resultado.content()).hasSize(2);
            assertThat(resultado.page()).isEqualTo(2);
            assertThat(resultado.pageSize()).isEqualTo(5);
            // Recalcular los totales sobre el contenido paginado es como se acaba
            // reportando «2 de 2» en un catalogo de once.
            assertThat(resultado.totalElements()).isEqualTo(11L);
            assertThat(resultado.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("traduce cada regla a su DTO sin tocar la escala de la tarifa")
        void traduce_cada_regla_sin_tocar_la_escala() {
            when(repository.findAllEnabled(0, 20))
                    .thenReturn(PageResult.of(List.of(WithholdingRateRuleMother.ica()), 0, 20, 1L));

            PageResult<WithholdingRateRuleDto> resultado = service.listAvailable(EMPRESA_DEL_TOKEN,
                    0, 20);

            assertThat(resultado.content()).singleElement().satisfies(dto -> {
                assertThat(dto.ratePercent()).isEqualByComparingTo("0.69");
                assertThat(dto.ratePercent().scale()).isEqualTo(6);
            });
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la empresa autoriza en el puerto y NO viaja al repositorio")
        void la_empresa_autoriza_y_no_viaja_al_repositorio() {
            // El companyId llega al puerto porque LISTADOS_SIN_EMPRESA_SOLO_SYSTEM
            // mira si el puerto lo transporta antes que ninguna otra cosa: un
            // listado que no lo transporta solo lo puede servir hasRole('SYSTEM') a
            // secas y el tenant se quedaria sin poder leer el catalogo. Y no filtra
            // porque no hay por donde: la tabla no tiene columna de empresa.
            when(repository.findAllEnabled(0, 20)).thenReturn(PageResult.empty(0, 20));

            service.listAvailable(EMPRESA_DEL_TOKEN, 0, 20);

            verify(repository).findAllEnabled(0, 20);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("dos empresas distintas reciben exactamente el mismo catalogo")
        void dos_empresas_distintas_reciben_el_mismo_catalogo() {
            when(repository.findAllEnabled(0, 20)).thenReturn(
                    PageResult.of(List.of(WithholdingRateRuleMother.nacional()), 0, 20, 1L));

            PageResult<WithholdingRateRuleDto> unaClinica = service.listAvailable(EMPRESA_DEL_TOKEN,
                    0, 20);
            PageResult<WithholdingRateRuleDto> otraClinica = service.listAvailable(999L, 0, 20);

            assertThat(unaClinica.content()).isEqualTo(otraClinica.content());
        }
    }
}
