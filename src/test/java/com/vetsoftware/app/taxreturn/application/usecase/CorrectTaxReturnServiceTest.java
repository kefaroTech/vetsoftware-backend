package com.vetsoftware.app.taxreturn.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.taxreturn.application.command.CorrectTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotEditableException;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotFoundException;
import com.vetsoftware.app.taxreturn.domain.TaxReturnStatus;
import com.vetsoftware.app.taxreturn.testsupport.TaxReturnMother;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrectTaxReturnService")
class CorrectTaxReturnServiceTest {

    private static final Long ID = 61L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-04-10T08:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);

    @Mock
    private TaxReturnRepository repository;

    @Captor
    private ArgumentCaptor<TaxReturn> captor;

    private CorrectTaxReturnService service;

    @BeforeEach
    void setUp() {
        service = new CorrectTaxReturnService(repository, RELOJ);
    }

    private static CorrectTaxReturnCommand comando() {
        return new CorrectTaxReturnCommand(ID, new BigDecimal("200.00"), new BigDecimal("50.00"),
                new BigDecimal("150.00"), BigDecimal.ZERO);
    }

    @Nested
    @DisplayName("correccion de una declaracion presentada")
    class Correccion {

        @Test
        @DisplayName("la anterior pasa a CORRECTED y nace un borrador nuevo que la referencia")
        void la_anterior_pasa_a_corrected_y_nace_un_borrador_nuevo() {
            TaxReturn original = TaxReturnMother.retencionPresentada(ID);
            when(repository.findById(ID)).thenReturn(Optional.of(original));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando());

            verify(repository, times(2)).save(captor.capture());
            List<TaxReturn> guardadas = captor.getAllValues();

            TaxReturn anteriorCorregida = guardadas.get(0);
            assertThat(anteriorCorregida.getId()).isEqualTo(ID);
            assertThat(anteriorCorregida.getStatus()).isEqualTo(TaxReturnStatus.CORRECTED);

            TaxReturn correccion = guardadas.get(1);
            assertThat(correccion.getId()).isNull();
            assertThat(correccion.getStatus()).isEqualTo(TaxReturnStatus.DRAFT);
            assertThat(correccion.getCorrectsReturnId()).isEqualTo(ID);
            assertThat(correccion.getSequenceNumber()).isEqualTo(original.getSequenceNumber() + 1);
            assertThat(correccion.getCreatedDate()).isEqualTo(AHORA);
            assertThat(correccion.getTotalGenerated()).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("un borrador no se corrige: la invariante de estado corta antes de guardar")
        void un_borrador_no_se_corrige() {
            TaxReturn borrador = TaxReturnMother.conId(ID, TaxReturnMother.borradorDeRetencion());
            when(repository.findById(ID)).thenReturn(Optional.of(borrador));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(TaxReturnNotEditableException.class)
                    .hasMessageContaining("cannot be modified while in status DRAFT");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("declaracion inexistente no se corrige")
        void declaracion_inexistente_no_se_corrige() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(TaxReturnNotFoundException.class)
                    .hasMessageContaining("Tax return not found: " + ID);

            verify(repository, never()).save(any());
        }
    }
}
