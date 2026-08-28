package com.vetsoftware.app.taxreturn.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.taxreturn.application.command.FileTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotEditableException;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotFoundException;
import com.vetsoftware.app.taxreturn.domain.TaxReturnStatus;
import com.vetsoftware.app.taxreturn.testsupport.TaxReturnMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
@DisplayName("FileTaxReturnService")
class FileTaxReturnServiceTest {

    private static final Long ID = 55L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-20T09:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);

    @Mock
    private TaxReturnRepository repository;

    @Captor
    private ArgumentCaptor<TaxReturn> captor;

    private FileTaxReturnService service;

    @BeforeEach
    void setUp() {
        service = new FileTaxReturnService(repository, RELOJ);
    }

    private static FileTaxReturnCommand comando() {
        return new FileTaxReturnCommand(ID, 7L, "RAD-2026-001", "s3://tax-returns/2026-001.pdf",
                LocalDate.of(2029, 3, 20));
    }

    @Nested
    @DisplayName("presentacion de un borrador")
    class Presentacion {

        @Test
        @DisplayName("presenta con la fecha del reloj inyectado, nunca la del cliente")
        void presenta_con_la_fecha_del_reloj_inyectado() {
            TaxReturn borrador = TaxReturnMother.conId(ID, TaxReturnMother.borradorDeRetencion());
            when(repository.findById(ID)).thenReturn(Optional.of(borrador));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando());

            verify(repository).save(captor.capture());
            TaxReturn presentada = captor.getValue();
            assertThat(presentada.getStatus()).isEqualTo(TaxReturnStatus.FILED);
            assertThat(presentada.getFiledAt()).isEqualTo(AHORA);
            assertThat(presentada.getFiledBySystemUserId()).isEqualTo(7L);
            assertThat(presentada.getReceiptRef()).isEqualTo("RAD-2026-001");
            assertThat(presentada.getFileRef()).isEqualTo("s3://tax-returns/2026-001.pdf");
            assertThat(presentada.getFirmezaUntil()).isEqualTo(LocalDate.of(2029, 3, 20));
        }

        @Test
        @DisplayName("declaracion inexistente no se presenta")
        void declaracion_inexistente_no_se_presenta() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(TaxReturnNotFoundException.class)
                    .hasMessageContaining("Tax return not found: " + ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una declaracion ya presentada no se puede volver a presentar")
        void una_declaracion_ya_presentada_no_se_puede_volver_a_presentar() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(TaxReturnMother.retencionPresentada(ID)));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(TaxReturnNotEditableException.class)
                    .hasMessageContaining("cannot be modified while in status FILED");

            verify(repository, never()).save(any());
        }
    }
}
