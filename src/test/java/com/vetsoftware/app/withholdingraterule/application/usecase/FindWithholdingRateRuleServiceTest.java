package com.vetsoftware.app.withholdingraterule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import com.vetsoftware.app.withholdingraterule.application.port.out.WithholdingRateRuleRepository;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRuleNotFoundException;
import com.vetsoftware.app.withholdingraterule.testsupport.WithholdingRateRuleMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindWithholdingRateRuleService — una tarifa por su id")
class FindWithholdingRateRuleServiceTest {

    private static final Long EMPRESA_DEL_TOKEN = 77L;

    @Mock
    private WithholdingRateRuleRepository repository;

    @InjectMocks
    private FindWithholdingRateRuleService service;

    @Nested
    @DisplayName("Consulta")
    class Consulta {

        @Test
        @DisplayName("devuelve la tarifa con la escala intacta")
        void devuelve_la_tarifa_con_la_escala_intacta() {
            when(repository.findById(8302L))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.ica()));

            WithholdingRateRuleDto dto = service.findById(8302L, EMPRESA_DEL_TOKEN);

            assertThat(dto.id()).isEqualTo(8302L);
            assertThat(dto.ratePercent()).isEqualByComparingTo("0.69");
            assertThat(dto.ratePercent().scale()).isEqualTo(6);
            assertThat(dto.municipalityCode()).isEqualTo("11001");
        }

        @Test
        @DisplayName("una tarifa inexistente sale como no encontrada")
        void una_tarifa_inexistente_sale_como_no_encontrada() {
            when(repository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(404L, EMPRESA_DEL_TOKEN))
                    .isInstanceOf(WithholdingRateRuleNotFoundException.class)
                    .hasMessage("Withholding rate rule not found: 404");
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la empresa autoriza en el puerto y NO llega al repositorio")
        void la_empresa_autoriza_y_no_llega_al_repositorio() {
            // Este caso congela la mitad rara de la feature. El companyId no es un
            // criterio de busqueda: la tabla es un catalogo global sin columna de
            // empresa y la tarifa de un supuesto fiscal es la misma para todas las
            // clinicas. Se gasta entero en @authz.isMyCompany(#companyId) del
            // puerto y despues se descarta. Si algun dia alguien lo pasara al
            // repositorio «para acotar», el listado saldria vacio siempre.
            when(repository.findById(8302L))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.ica()));

            service.findById(8302L, EMPRESA_DEL_TOKEN);

            verify(repository).findById(8302L);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("dos empresas distintas ven exactamente la misma tarifa")
        void dos_empresas_distintas_ven_la_misma_tarifa() {
            when(repository.findById(8302L))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.ica()));

            WithholdingRateRuleDto unaClinica = service.findById(8302L, EMPRESA_DEL_TOKEN);
            WithholdingRateRuleDto otraClinica = service.findById(8302L, 999L);

            assertThat(unaClinica).isEqualTo(otraClinica);
        }
    }
}
