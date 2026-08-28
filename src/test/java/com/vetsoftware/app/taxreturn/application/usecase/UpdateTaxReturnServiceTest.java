package com.vetsoftware.app.taxreturn.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.taxreturn.application.command.UpdateTaxReturnAmountsCommand;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotEditableException;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotFoundException;
import com.vetsoftware.app.taxreturn.testsupport.TaxReturnMother;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateTaxReturnService")
class UpdateTaxReturnServiceTest {

    private static final Long ID = 41L;

    @Mock
    private TaxReturnRepository repository;

    @InjectMocks
    private UpdateTaxReturnService service;

    @Captor
    private ArgumentCaptor<TaxReturn> captor;

    private static UpdateTaxReturnAmountsCommand comando() {
        return new UpdateTaxReturnAmountsCommand(ID, new BigDecimal("300.00"),
                new BigDecimal("50.00"), new BigDecimal("250.00"), BigDecimal.ZERO);
    }

    @Nested
    @DisplayName("correccion de importes de un borrador")
    class Correccion {

        @Test
        @DisplayName("actualiza los importes del borrador encontrado y lo persiste")
        void actualiza_los_importes_y_los_persiste() {
            TaxReturn borrador = TaxReturnMother.conId(ID, TaxReturnMother.borradorDeRetencion());
            when(repository.findById(ID)).thenReturn(Optional.of(borrador));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TaxReturnDto dto = service.execute(comando());

            verify(repository).save(captor.capture());
            TaxReturn actualizado = captor.getValue();
            assertThat(actualizado.getTotalGenerated()).isEqualByComparingTo("300.00");
            assertThat(actualizado.getTotalDeductible()).isEqualByComparingTo("50.00");
            assertThat(actualizado.getBalancePayable()).isEqualByComparingTo("250.00");
            assertThat(actualizado.getBalanceCredit()).isEqualByComparingTo("0");
            assertThat(dto.totalGenerated()).isEqualByComparingTo("300.00");
        }

        @Test
        @DisplayName("declaracion inexistente no llega a actualizarse")
        void declaracion_inexistente_no_llega_a_actualizarse() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(TaxReturnNotFoundException.class)
                    .hasMessageContaining("Tax return not found: " + ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una declaracion ya presentada no admite correccion de importes")
        void una_declaracion_presentada_no_admite_correccion() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(TaxReturnMother.retencionPresentada(ID)));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(TaxReturnNotEditableException.class)
                    .hasMessageContaining("cannot be modified while in status FILED");

            verify(repository, never()).save(any());
        }
    }
}
