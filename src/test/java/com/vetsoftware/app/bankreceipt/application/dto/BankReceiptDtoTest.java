package com.vetsoftware.app.bankreceipt.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import com.vetsoftware.app.bankreceipt.testsupport.BankReceiptMother;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BankReceiptDto — proyeccion de la entrada del extracto")
class BankReceiptDtoTest {

    @Nested
    @DisplayName("Proyeccion")
    class Proyeccion {

        @Test
        @DisplayName("traslada los nueve campos sin cruzar las tres fechas")
        void traslada_los_nueve_campos_sin_cruzar_las_fechas() {
            // receivedOn, identifiedAt y createdDate son tres instantes distintos a
            // proposito: cruzarlos compila sin una queja y solo se ve aqui.
            BankReceipt entrada = BankReceiptMother.enEstado(BankReceiptStatus.IDENTIFIED);

            BankReceiptDto dto = BankReceiptDto.from(entrada);

            assertThat(dto.id()).isEqualTo(entrada.getId());
            assertThat(dto.bankAccountRef()).isEqualTo(BankReceiptMother.CUENTA);
            assertThat(dto.bankReference()).isEqualTo(BankReceiptMother.REFERENCIA);
            assertThat(dto.receivedOn()).isEqualTo(BankReceiptMother.RECIBIDA_EL);
            assertThat(dto.amount()).isEqualByComparingTo(BankReceiptMother.IMPORTE);
            assertThat(dto.description()).isEqualTo(BankReceiptMother.DESCRIPCION);
            assertThat(dto.status()).isEqualTo(BankReceiptStatus.IDENTIFIED);
            assertThat(dto.identifiedAt()).isEqualTo(BankReceiptMother.SELLADA_EL);
            assertThat(dto.createdDate()).isEqualTo(BankReceiptMother.CREADA_EL);
        }

        @Test
        @DisplayName("una entrada en la bandeja sale con identifiedAt nulo")
        void una_entrada_en_la_bandeja_sale_sin_sello() {
            assertThat(BankReceiptDto.from(BankReceiptMother.enLaBandeja()).identifiedAt())
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Lo que NO publica")
    class LoQueNoPublica {

        @Test
        @DisplayName("no expone la version: es la barandilla del bloqueo, no un dato")
        void no_expone_la_version() {
            // Publicarla invitaria a un cliente a devolverla y a construir un control de
            // concurrencia paralelo al que ya hace Hibernate.
            assertThat(Arrays.stream(BankReceiptDto.class.getRecordComponents())
                    .map(RecordComponent::getName)).doesNotContain("version").hasSize(9);
        }
    }
}
