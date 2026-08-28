package com.vetsoftware.app.bankreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.bankreceipt.application.command.DiscardBankReceiptCommand;
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
import java.time.ZoneOffset;
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
 * Archivar una entrada que no corresponde a ningun cliente.
 *
 * <p>
 * <b>Lo que este test congela y una revision humana no ve</b> es que descartar
 * <em>tambien</em> sella {@code identifiedAt}. Leyendo el nombre de la columna,
 * lo natural seria dejarla nula al descartar —«no se identifico a nadie»— y esa
 * fila la rechaza {@code chk_bank_receipts_identified} en el {@code UPDATE},
 * con un error que no explica nada. La columna no guarda «quien fue», guarda
 * «cuando se dejo de buscar».
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiscardBankReceiptService — archivar sin borrar")
class DiscardBankReceiptServiceTest {

    private static final Long ID = 8702L;

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-04-02T15:10:00Z"),
            ZoneOffset.UTC);

    private static final LocalDateTime SELLO_ESPERADO = LocalDateTime.of(2026, 4, 2, 15, 10, 0);

    @Mock
    private BankReceiptRepository repository;

    private DiscardBankReceiptService service;

    @BeforeEach
    void servicio() {
        service = new DiscardBankReceiptService(repository, RELOJ);
    }

    @Nested
    @DisplayName("Descarte")
    class Descarte {

        @Test
        @DisplayName("pasa a DISCARDED y sella la misma columna que identificar")
        void pasa_a_discarded_y_sella_la_misma_columna() {
            when(repository.findById(ID)).thenReturn(Optional.of(BankReceiptMother.persistida(ID)));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            BankReceiptDto archivada = service.execute(new DiscardBankReceiptCommand(ID));

            ArgumentCaptor<BankReceipt> guardada = ArgumentCaptor.forClass(BankReceipt.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getStatus()).isEqualTo(BankReceiptStatus.DISCARDED);
            assertThat(guardada.getValue().getIdentifiedAt()).isEqualTo(SELLO_ESPERADO);
            assertThat(archivada.status()).isEqualTo(BankReceiptStatus.DISCARDED);
            assertThat(archivada.identifiedAt()).isEqualTo(SELLO_ESPERADO);
        }

        @Test
        @DisplayName("la fila se queda: descartar NO es un borrado logico")
        void la_fila_se_queda() {
            // No hay `enabled`, no hay `delete` en el puerto y el importe sigue ahi. Si
            // alguien convirtiera esto en una baja, el cuadre perderia la explicacion de
            // por que ese dinero no se atribuyo a nadie.
            when(repository.findById(ID)).thenReturn(Optional.of(BankReceiptMother.persistida(ID)));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            BankReceiptDto archivada = service.execute(new DiscardBankReceiptCommand(ID));

            assertThat(archivada.id()).isEqualTo(ID);
            assertThat(archivada.amount()).isEqualByComparingTo(BankReceiptMother.IMPORTE);
            assertThat(archivada.bankReference()).isEqualTo(BankReceiptMother.REFERENCIA);
        }
    }

    @Nested
    @DisplayName("Rechazos")
    class Rechazos {

        @Test
        @DisplayName("una entrada inexistente sale 404 y no escribe")
        void una_entrada_inexistente_no_escribe() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new DiscardBankReceiptCommand(ID)))
                    .isInstanceOf(BankReceiptNotFoundException.class);

            verify(repository, never()).save(any());
        }

        @ParameterizedTest
        @EnumSource(value = BankReceiptStatus.class, names = {"IDENTIFIED", "DISCARDED"})
        @DisplayName("una entrada que ya salio de la bandeja es un conflicto y no escribe")
        void una_entrada_ya_resuelta_es_un_conflicto(BankReceiptStatus resuelto) {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(BankReceiptMother.enEstado(resuelto)));

            assertThatThrownBy(() -> service.execute(new DiscardBankReceiptCommand(ID)))
                    .isInstanceOf(BankReceiptAlreadyResolvedException.class);

            verify(repository, never()).save(any());
        }
    }
}
