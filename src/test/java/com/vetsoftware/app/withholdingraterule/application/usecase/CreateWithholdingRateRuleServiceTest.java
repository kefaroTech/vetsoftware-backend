package com.vetsoftware.app.withholdingraterule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.withholdingraterule.application.command.CreateWithholdingRateRuleCommand;
import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import com.vetsoftware.app.withholdingraterule.application.port.out.MunicipalityValidationPort;
import com.vetsoftware.app.withholdingraterule.application.port.out.WithholdingRateRuleRepository;
import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRule;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import com.vetsoftware.app.withholdingraterule.testsupport.WithholdingRateRuleMother;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateWithholdingRateRuleService — alta de una tarifa de retencion")
class CreateWithholdingRateRuleServiceTest {

    /** Reloj congelado: sin el, la fecha sellada seria imposible de afirmar. */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-01-03T13:45:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime SELLADA_EL = LocalDateTime.of(2026, 1, 3, 13, 45);

    @Mock
    private WithholdingRateRuleRepository repository;
    @Mock
    private MunicipalityValidationPort municipalityValidationPort;

    private CreateWithholdingRateRuleService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("traslada los nueve campos del comando al agregado sin cruzarlos")
        void traslada_los_nueve_campos_sin_cruzarlos() {
            service = new CreateWithholdingRateRuleService(repository, municipalityValidationPort,
                    RELOJ);
            when(municipalityValidationPort.existsByDaneCode(WithholdingRateRuleMother.BOGOTA))
                    .thenReturn(true);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            WithholdingRateRuleDto creada = service.execute(WithholdingRateRuleMother.comandoIca());

            ArgumentCaptor<WithholdingRateRule> guardada = ArgumentCaptor
                    .forClass(WithholdingRateRule.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue()).satisfies(regla -> {
                assertThat(regla.getId()).isNull();
                assertThat(regla.getWithholdingType()).isEqualTo(WithholdingType.ICA);
                assertThat(regla.getServiceNature()).isEqualTo(ServiceNature.CONSULTING);
                assertThat(regla.getMunicipalityCode()).isEqualTo(WithholdingRateRuleMother.BOGOTA);
                // El 6,9 por mil entero: si el service tocara la escala, aqui se ve.
                assertThat(regla.getRatePercent()).isEqualByComparingTo("0.69");
                assertThat(regla.getRatePercent().scale()).isEqualTo(6);
                assertThat(regla.getMinimumBaseAmount()).isEqualByComparingTo("213010.00");
                assertThat(regla.getMinimumBaseUvt()).isEqualByComparingTo("4.00");
                assertThat(regla.getLegalReference()).isEqualTo("Acuerdo 65 de 2002");
                assertThat(regla.getValidFrom()).isEqualTo(WithholdingRateRuleMother.DESDE);
                assertThat(regla.getValidTo()).isNull();
            });
            assertThat(creada.ratePercent()).isEqualByComparingTo("0.69");
        }

        @Test
        @DisplayName("sella la fecha de creacion con el reloj inyectado y no con la del sistema")
        void sella_la_fecha_con_el_reloj_inyectado() {
            service = new CreateWithholdingRateRuleService(repository, municipalityValidationPort,
                    RELOJ);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(WithholdingRateRuleMother.comandoNacional());

            ArgumentCaptor<WithholdingRateRule> guardada = ArgumentCaptor
                    .forClass(WithholdingRateRule.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getCreatedDate()).isEqualTo(SELLADA_EL);
        }

        @Test
        @DisplayName("la tarifa nace habilitada y sin version")
        void la_tarifa_nace_habilitada_y_sin_version() {
            service = new CreateWithholdingRateRuleService(repository, municipalityValidationPort,
                    RELOJ);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            WithholdingRateRuleDto creada = service
                    .execute(WithholdingRateRuleMother.comandoNacional());

            assertThat(creada.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un municipio que no existe se rechaza antes de tocar el repositorio")
        void un_municipio_que_no_existe_se_rechaza_antes_de_guardar() {
            // Sin esta comprobacion, fk_withholding_rate_rules_municipality lo
            // pararia igual pero como un error de integridad: un 500 en la cara de
            // quien configura, en vez del «ese municipio no existe» que
            // corresponde.
            service = new CreateWithholdingRateRuleService(repository, municipalityValidationPort,
                    RELOJ);
            when(municipalityValidationPort.existsByDaneCode(WithholdingRateRuleMother.BOGOTA))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.execute(WithholdingRateRuleMother.comandoIca()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Municipality not found: 11001");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una retencion nacional no pregunta por ningun municipio")
        void una_retencion_nacional_no_pregunta_por_ningun_municipio() {
            // El municipio es nulo fuera de ICA, y preguntar por un codigo nulo
            // seria una consulta que siempre falla.
            service = new CreateWithholdingRateRuleService(repository, municipalityValidationPort,
                    RELOJ);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(WithholdingRateRuleMother.comandoNacional());

            verify(municipalityValidationPort, never()).existsByDaneCode(any());
        }

        @Test
        @DisplayName("las invariantes las pone el dominio: un comando invalido no llega a guardarse")
        void un_comando_invalido_no_llega_a_guardarse() {
            // La tarifa como fraccion negativa. Que lo pare el constructor del
            // agregado y no el service es la diferencia entre una regla que no se
            // puede saltar y una comprobacion que otro camino puede evitar.
            service = new CreateWithholdingRateRuleService(repository, municipalityValidationPort,
                    RELOJ);

            assertThatThrownBy(() -> service.execute(comandoConTarifa(new BigDecimal("-1.000000"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("ratePercent must be greater than zero");
            verify(repository, never()).save(any());
        }
    }

    private static CreateWithholdingRateRuleCommand comandoConTarifa(BigDecimal tarifa) {
        return new CreateWithholdingRateRuleCommand(WithholdingType.INCOME_TAX,
                ServiceNature.TECHNICAL_SERVICE, null, tarifa,
                WithholdingRateRuleMother.BASE_EN_PESOS, null, null,
                WithholdingRateRuleMother.DESDE, null);
    }
}
