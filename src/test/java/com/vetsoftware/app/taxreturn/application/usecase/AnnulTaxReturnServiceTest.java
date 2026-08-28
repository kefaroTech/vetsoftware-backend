package com.vetsoftware.app.taxreturn.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotEditableException;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotFoundException;
import com.vetsoftware.app.taxreturn.domain.TaxReturnStatus;
import com.vetsoftware.app.taxreturn.testsupport.TaxReturnMother;
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
@DisplayName("AnnulTaxReturnService")
class AnnulTaxReturnServiceTest {

    private static final Long ID = 73L;

    @Mock
    private TaxReturnRepository repository;

    @InjectMocks
    private AnnulTaxReturnService service;

    @Captor
    private ArgumentCaptor<TaxReturn> captor;

    @Nested
    @DisplayName("anulacion de un borrador")
    class Anulacion {

        @Test
        @DisplayName("anula el borrador: libera el hueco y borra los datos de presentacion")
        void anula_el_borrador() {
            TaxReturn borrador = TaxReturnMother.conId(ID, TaxReturnMother.borradorDeRetencion());
            when(repository.findById(ID)).thenReturn(Optional.of(borrador));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(ID);

            verify(repository).save(captor.capture());
            TaxReturn anulada = captor.getValue();
            assertThat(anulada.getStatus()).isEqualTo(TaxReturnStatus.ANNULLED);
            assertThat(anulada.getFiledAt()).isNull();
            assertThat(anulada.getFirmezaUntil()).isNull();
            assertThat(anulada.isCurrent()).isFalse();
        }

        @Test
        @DisplayName("declaracion inexistente no se anula")
        void declaracion_inexistente_no_se_anula() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ID))
                    .isInstanceOf(TaxReturnNotFoundException.class)
                    .hasMessageContaining("Tax return not found: " + ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una declaracion presentada no se anula: se corrige")
        void una_declaracion_presentada_no_se_anula() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(TaxReturnMother.retencionPresentada(ID)));

            assertThatThrownBy(() -> service.execute(ID))
                    .isInstanceOf(TaxReturnNotEditableException.class)
                    .hasMessageContaining("cannot be modified while in status FILED");

            verify(repository, never()).save(any());
        }
    }
}
