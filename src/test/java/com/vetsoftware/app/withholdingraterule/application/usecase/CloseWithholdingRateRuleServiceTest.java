package com.vetsoftware.app.withholdingraterule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.withholdingraterule.application.command.CloseWithholdingRateRuleCommand;
import com.vetsoftware.app.withholdingraterule.application.port.out.WithholdingRateRuleRepository;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRule;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRuleAlreadyClosedException;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRuleNotFoundException;
import com.vetsoftware.app.withholdingraterule.testsupport.WithholdingRateRuleMother;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloseWithholdingRateRuleService — cierre de una vigencia")
class CloseWithholdingRateRuleServiceTest {

    private static final Long ID_ABIERTA = 8301L;
    private static final LocalDate CIERRE = LocalDate.of(2027, 1, 1);

    @Mock
    private WithholdingRateRuleRepository repository;

    @InjectMocks
    private CloseWithholdingRateRuleService service;

    @Nested
    @DisplayName("Cierre")
    class Cierre {

        @Test
        @DisplayName("escribe la fecha de fin y conserva la version del bloqueo optimista")
        void escribe_la_fecha_de_fin_y_conserva_la_version() {
            when(repository.findById(ID_ABIERTA))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.nacional()));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(new CloseWithholdingRateRuleCommand(ID_ABIERTA, CIERRE));

            ArgumentCaptor<WithholdingRateRule> guardada = ArgumentCaptor
                    .forClass(WithholdingRateRule.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue()).satisfies(regla -> {
                assertThat(regla.getId()).isEqualTo(ID_ABIERTA);
                assertThat(regla.getValidTo()).isEqualTo(CIERRE);
                assertThat(regla.isOpen()).isFalse();
                // Sin la version, el save seria un insert y quedarian dos
                // vigencias para el mismo supuesto.
                assertThat(regla.getVersion()).isEqualTo(0L);
            });
        }

        @Test
        @DisplayName("no borra ni deshabilita: la fila se queda y solo cambia hasta cuando vale")
        void no_borra_ni_deshabilita() {
            // Una tarifa cerrada sigue siendo la correcta para las facturas de su
            // vigencia. Deshabilitarla dejaria sin explicacion las retenciones ya
            // calculadas.
            when(repository.findById(ID_ABIERTA))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.nacional()));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(new CloseWithholdingRateRuleCommand(ID_ABIERTA, CIERRE));

            ArgumentCaptor<WithholdingRateRule> guardada = ArgumentCaptor
                    .forClass(WithholdingRateRule.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una regla inexistente sale como no encontrada y no se guarda nada")
        void una_regla_inexistente_sale_como_no_encontrada() {
            when(repository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new CloseWithholdingRateRuleCommand(404L, CIERRE)))
                    .isInstanceOf(WithholdingRateRuleNotFoundException.class)
                    .hasMessage("Withholding rate rule not found: 404");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("cerrar una ya cerrada se rechaza: la base no lo impide y perderia la fecha")
        void cerrar_una_ya_cerrada_se_rechaza() {
            // current_rule_marker vale NULL en una regla cerrada y una unicidad
            // sobre columna nula no restringe nada: sin esta negativa el segundo
            // cierre pasaria en silencio y machacaria la fecha del primero.
            when(repository.findById(8303L))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.cerrada()));

            assertThatThrownBy(
                    () -> service.execute(new CloseWithholdingRateRuleCommand(8303L, CIERRE)))
                    .isInstanceOf(WithholdingRateRuleAlreadyClosedException.class);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una fecha de fin anterior a la de inicio la para el dominio")
        void una_fecha_de_fin_anterior_la_para_el_dominio() {
            when(repository.findById(ID_ABIERTA))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.nacional()));

            assertThatThrownBy(() -> service.execute(
                    new CloseWithholdingRateRuleCommand(ID_ABIERTA, LocalDate.of(2025, 6, 1))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("validTo must be after validFrom");
            verify(repository, never()).save(any());
        }
    }
}
