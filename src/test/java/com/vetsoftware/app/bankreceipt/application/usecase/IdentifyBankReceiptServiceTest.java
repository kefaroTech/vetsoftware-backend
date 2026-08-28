package com.vetsoftware.app.bankreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.bankreceipt.application.command.IdentifyBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.application.port.out.BankReceiptRepository;
import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptAlreadyResolvedException;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptNotFoundException;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import com.vetsoftware.app.bankreceipt.testsupport.BankReceiptMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Sacar una entrada de la bandeja.
 *
 * <p>
 * <b>El caso que justifica el reloj inyectado es el de la zona horaria.</b> La
 * JVM de produccion corre en UTC y {@code ClockConfig} fija
 * {@code America/Bogota} justamente para que «hoy» signifique lo mismo aqui que
 * en la pantalla del operario. Este test usa un {@code Clock.fixed} en esa zona
 * y afirma el instante exacto: un {@code LocalDateTime.now()} pelado sellaria
 * cinco horas adelantado y una entrada resuelta a las 19:30 del ultimo dia del
 * mes contaria en el mes siguiente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdentifyBankReceiptService — el sello sale del reloj del negocio")
class IdentifyBankReceiptServiceTest {

    private static final Long ID = 8701L;

    /** 2026-03-31T23:30Z, que en Bogota es todavia el dia 31 a las 18:30. */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-31T23:30:00Z"),
            java.time.ZoneId.of("America/Bogota"));

    private static final LocalDateTime SELLO_ESPERADO = LocalDateTime.of(2026, 3, 31, 18, 30, 0);

    @Mock
    private BankReceiptRepository repository;

    private IdentifyBankReceiptService service;

    @BeforeEach
    void servicio() {
        service = new IdentifyBankReceiptService(repository, RELOJ);
    }

    @Nested
    @DisplayName("Identificacion")
    class Identificacion {

        @Test
        @DisplayName("pasa a IDENTIFIED y sella la hora del reloj del negocio")
        void pasa_a_identified_y_sella_la_hora_del_negocio() {
            when(repository.findById(ID)).thenReturn(Optional.of(BankReceiptMother.persistida(ID)));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            BankReceiptDto resuelta = service.execute(new IdentifyBankReceiptCommand(ID));

            ArgumentCaptor<BankReceipt> guardada = ArgumentCaptor.forClass(BankReceipt.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getStatus()).isEqualTo(BankReceiptStatus.IDENTIFIED);
            // 18:30 del 31 de marzo, no 23:30 ni el 1 de abril.
            assertThat(guardada.getValue().getIdentifiedAt()).isEqualTo(SELLO_ESPERADO);
            assertThat(resuelta.identifiedAt()).isEqualTo(SELLO_ESPERADO);
            assertThat(resuelta.status()).isEqualTo(BankReceiptStatus.IDENTIFIED);
        }

        @Test
        @DisplayName("no toca ningun otro campo de la entrada")
        void no_toca_ningun_otro_campo() {
            when(repository.findById(ID)).thenReturn(Optional.of(BankReceiptMother.persistida(ID)));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            BankReceiptDto resuelta = service.execute(new IdentifyBankReceiptCommand(ID));

            assertThat(resuelta.bankReference()).isEqualTo(BankReceiptMother.REFERENCIA);
            assertThat(resuelta.amount()).isEqualByComparingTo(BankReceiptMother.IMPORTE);
            assertThat(resuelta.receivedOn()).isEqualTo(BankReceiptMother.RECIBIDA_EL);
            assertThat(resuelta.createdDate()).isEqualTo(BankReceiptMother.CREADA_EL);
        }
    }

    @Nested
    @DisplayName("Rechazos")
    class Rechazos {

        @Test
        @DisplayName("una entrada inexistente sale 404 y no escribe")
        void una_entrada_inexistente_no_escribe() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new IdentifyBankReceiptCommand(ID)))
                    .isInstanceOf(BankReceiptNotFoundException.class)
                    .hasMessageContaining("Bank receipt not found: " + ID);

            verify(repository, never()).save(any());
        }

        @ParameterizedTest
        @EnumSource(value = BankReceiptStatus.class, names = {"IDENTIFIED", "DISCARDED"})
        @DisplayName("una entrada que ya salio de la bandeja es un conflicto y no escribe")
        void una_entrada_ya_resuelta_es_un_conflicto(BankReceiptStatus resuelto) {
            // El operario ve una bandeja que se le quedo vieja en pantalla y pulsa sobre
            // algo que un compañero ya archivo. Lo que necesita es refrescar, no
            // corregir un campo: por eso es conflicto y no cuerpo mal formado.
            when(repository.findById(ID))
                    .thenReturn(Optional.of(BankReceiptMother.enEstado(resuelto)));

            assertThatThrownBy(() -> service.execute(new IdentifyBankReceiptCommand(ID)))
                    .isInstanceOf(BankReceiptAlreadyResolvedException.class);

            verify(repository, never()).save(any());
        }
    }
}
