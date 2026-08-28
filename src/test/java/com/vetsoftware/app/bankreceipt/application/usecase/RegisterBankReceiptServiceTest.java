package com.vetsoftware.app.bankreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.bankreceipt.application.command.RegisterBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.application.port.out.BankReceiptRepository;
import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptAlreadyRegisteredException;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import com.vetsoftware.app.bankreceipt.testsupport.BankReceiptMother;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La carga de una linea del extracto.
 *
 * <p>
 * <b>Lo que esta clase vigila y no ve nadie mas</b> es que la comprobacion de
 * duplicado use la referencia <em>tal como llega</em>. La columna es
 * {@code ascii_bin} y la unicidad es del par (referencia, fecha): normalizar
 * aqui a mayusculas —el reflejo habitual con un codigo de texto— haria que la
 * segunda consignacion del dia se rechazara como duplicada de la primera. Es un
 * defecto de una linea que no se ve en ninguna revision.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterBankReceiptService — carga del extracto")
class RegisterBankReceiptServiceTest {

    /** 2026-03-07 08:45:00 en la zona del negocio, no en UTC. */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-07T08:45:00Z"),
            ZoneOffset.UTC);

    @Mock
    private BankReceiptRepository repository;

    private RegisterBankReceiptService service;

    @BeforeEach
    void servicio() {
        // El Clock no es un puerto: se inyecta de verdad y fijo, para que createdDate
        // sea afirmable sin depender del reloj de la maquina.
        service = new RegisterBankReceiptService(repository, RELOJ);
    }

    @Nested
    @DisplayName("Alta")
    class Alta {

        @Test
        @DisplayName("guarda la entrada en la bandeja y sella createdDate con el reloj inyectado")
        void guarda_la_entrada_en_la_bandeja_con_el_reloj_inyectado() {
            when(repository.existsByBankReferenceAndReceivedOn(BankReceiptMother.REFERENCIA,
                    BankReceiptMother.RECIBIDA_EL)).thenReturn(false);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            BankReceiptDto creada = service.execute(comando(BankReceiptMother.IMPORTE));

            ArgumentCaptor<BankReceipt> guardada = ArgumentCaptor.forClass(BankReceipt.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue()).satisfies(entrada -> {
                assertThat(entrada.getBankAccountRef()).isEqualTo(BankReceiptMother.CUENTA);
                assertThat(entrada.getBankReference()).isEqualTo(BankReceiptMother.REFERENCIA);
                assertThat(entrada.getReceivedOn()).isEqualTo(BankReceiptMother.RECIBIDA_EL);
                assertThat(entrada.getAmount()).isEqualByComparingTo(BankReceiptMother.IMPORTE);
                assertThat(entrada.getStatus()).isEqualTo(BankReceiptStatus.UNIDENTIFIED);
                assertThat(entrada.getIdentifiedAt()).isNull();
                assertThat(entrada.getCreatedDate())
                        .isEqualTo(LocalDateTime.of(2026, 3, 7, 8, 45, 0));
            });
            assertThat(creada.status()).isEqualTo(BankReceiptStatus.UNIDENTIFIED);
        }

        @Test
        @DisplayName("un importe negativo llega al dominio con su signo y se guarda")
        void un_importe_negativo_llega_con_su_signo() {
            // Ningun paso del camino —ni el request, ni el command, ni el service— puede
            // filtrar el signo: un cargo del banco es una linea legitima del extracto.
            when(repository.existsByBankReferenceAndReceivedOn(BankReceiptMother.REFERENCIA,
                    BankReceiptMother.RECIBIDA_EL)).thenReturn(false);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            BankReceiptDto cargo = service.execute(comando(new BigDecimal("-45000.00")));

            assertThat(cargo.amount()).isEqualByComparingTo("-45000.00");
        }
    }

    @Nested
    @DisplayName("Duplicados")
    class Duplicados {

        @Test
        @DisplayName("recargar el mismo extracto es un conflicto y NO escribe")
        void recargar_el_mismo_extracto_es_un_conflicto_y_no_escribe() {
            when(repository.existsByBankReferenceAndReceivedOn(BankReceiptMother.REFERENCIA,
                    BankReceiptMother.RECIBIDA_EL)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(comando(BankReceiptMother.IMPORTE)))
                    .isInstanceOf(BankReceiptAlreadyRegisteredException.class)
                    .hasMessageContaining(BankReceiptMother.REFERENCIA)
                    .hasMessageContaining("2026-03-05");

            // La mitad del valor del caso: que el duplicado no llegue a la base.
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("la comprobacion usa la referencia tal cual, sin normalizar mayusculas")
        void la_comprobacion_usa_la_referencia_tal_cual() {
            // Con STRICT_STUBS este stub solo casa si el service pregunta por la cadena
            // EXACTA. Un toUpperCase() metido en el service dejaria el stub sin usar y
            // el caso caeria: es la red de la comparacion ascii_bin.
            when(repository.existsByBankReferenceAndReceivedOn("trx-2026-03-0099a",
                    BankReceiptMother.RECIBIDA_EL)).thenReturn(false);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            BankReceiptDto creada = service.execute(new RegisterBankReceiptCommand(
                    BankReceiptMother.CUENTA, "trx-2026-03-0099a", BankReceiptMother.RECIBIDA_EL,
                    BankReceiptMother.IMPORTE, BankReceiptMother.DESCRIPCION));

            assertThat(creada.bankReference()).isEqualTo("trx-2026-03-0099a");
        }
    }

    @Nested
    @DisplayName("Validaciones del dominio")
    class ValidacionesDelDominio {

        @Test
        @DisplayName("un importe de cero lo para el dominio y no se guarda nada")
        void un_importe_de_cero_lo_para_el_dominio() {
            when(repository.existsByBankReferenceAndReceivedOn(BankReceiptMother.REFERENCIA,
                    BankReceiptMother.RECIBIDA_EL)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comando(BigDecimal.ZERO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount cannot be zero");

            verify(repository, never()).save(any());
        }
    }

    private static RegisterBankReceiptCommand comando(BigDecimal importe) {
        return new RegisterBankReceiptCommand(BankReceiptMother.CUENTA,
                BankReceiptMother.REFERENCIA, LocalDate.of(2026, 3, 5), importe,
                BankReceiptMother.DESCRIPCION);
    }
}
