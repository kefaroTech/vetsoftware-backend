package com.vetsoftware.app.bankreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.application.port.out.BankReceiptRepository;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptNotFoundException;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import com.vetsoftware.app.bankreceipt.testsupport.BankReceiptMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La carga por id.
 *
 * <p>
 * <b>Es ancha porque no existe otra, y eso no es una fuga.</b>
 * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} marca al servicio que conoce la
 * variante ancha teniendo disponible la acotada; aqui el puerto de salida no
 * declara ninguna acotada porque la tabla no tiene empresa. Lo que sostiene el
 * aislamiento es el {@code hasRole('SYSTEM')} a secas del puerto de entrada, no
 * un {@code WHERE}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FindBankReceiptService — lectura por id")
class FindBankReceiptServiceTest {

    private static final Long ID = 8703L;

    @Mock
    private BankReceiptRepository repository;
    @InjectMocks
    private FindBankReceiptService service;

    @Nested
    @DisplayName("Lectura")
    class Lectura {

        @Test
        @DisplayName("devuelve la entrada con cada campo trasladado al dto")
        void devuelve_la_entrada_campo_a_campo() {
            when(repository.findById(ID)).thenReturn(Optional.of(BankReceiptMother.persistida(ID)));

            BankReceiptDto entrada = service.findById(ID);

            assertThat(entrada.id()).isEqualTo(ID);
            assertThat(entrada.bankAccountRef()).isEqualTo(BankReceiptMother.CUENTA);
            assertThat(entrada.bankReference()).isEqualTo(BankReceiptMother.REFERENCIA);
            assertThat(entrada.receivedOn()).isEqualTo(BankReceiptMother.RECIBIDA_EL);
            assertThat(entrada.amount()).isEqualByComparingTo(BankReceiptMother.IMPORTE);
            assertThat(entrada.description()).isEqualTo(BankReceiptMother.DESCRIPCION);
            assertThat(entrada.status()).isEqualTo(BankReceiptStatus.UNIDENTIFIED);
            assertThat(entrada.identifiedAt()).isNull();
            assertThat(entrada.createdDate()).isEqualTo(BankReceiptMother.CREADA_EL);
        }
    }

    @Nested
    @DisplayName("Rechazos")
    class Rechazos {

        @Test
        @DisplayName("un id que no existe lanza la excepcion de dominio con el id en el mensaje")
        void un_id_que_no_existe_lanza_la_excepcion_de_dominio() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(ID))
                    .isInstanceOf(BankReceiptNotFoundException.class)
                    .hasMessageContaining("Bank receipt not found: " + ID);
        }
    }
}
