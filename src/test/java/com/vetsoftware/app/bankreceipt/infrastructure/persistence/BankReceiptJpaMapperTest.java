package com.vetsoftware.app.bankreceipt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import com.vetsoftware.app.bankreceipt.testsupport.BankReceiptMother;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BankReceiptJpaMapper — ida y vuelta dominio/JPA")
class BankReceiptJpaMapperTest {

    private final BankReceiptJpaMapper mapper = new BankReceiptJpaMapper();

    @Nested
    @DisplayName("Ida")
    class Ida {

        @Test
        @DisplayName("lleva los diez campos a la entidad, la version incluida")
        void lleva_los_diez_campos_a_la_entidad() {
            BankReceipt resuelta = BankReceiptMother.enEstado(BankReceiptStatus.IDENTIFIED);

            BankReceiptJpaEntity entidad = mapper.toJpa(resuelta);

            assertThat(entidad.getId()).isEqualTo(resuelta.getId());
            assertThat(entidad.getBankAccountRef()).isEqualTo(BankReceiptMother.CUENTA);
            assertThat(entidad.getBankReference()).isEqualTo(BankReceiptMother.REFERENCIA);
            assertThat(entidad.getReceivedOn()).isEqualTo(BankReceiptMother.RECIBIDA_EL);
            assertThat(entidad.getAmount()).isEqualByComparingTo(BankReceiptMother.IMPORTE);
            assertThat(entidad.getDescription()).isEqualTo(BankReceiptMother.DESCRIPCION);
            assertThat(entidad.getStatus()).isEqualTo(BankReceiptStatus.IDENTIFIED);
            assertThat(entidad.getIdentifiedAt()).isEqualTo(BankReceiptMother.SELLADA_EL);
            assertThat(entidad.getCreatedDate()).isEqualTo(BankReceiptMother.CREADA_EL);
            assertThat(entidad.getVersion()).isEqualTo(resuelta.getVersion());
        }

        @Test
        @DisplayName("la version viaja aunque sea nula: una entrada nueva no la tiene")
        void la_version_viaja_aunque_sea_nula() {
            // Y al reves importa mas: si el mapper NO llevara la version de una entrada
            // ya persistida, Hibernate la trataria como nueva y el `save` que muta el
            // estado se convertiria en un INSERT. El bloqueo optimista dejaria de
            // proteger justo la unica operacion que muta esta tabla.
            assertThat(mapper.toJpa(BankReceiptMother.enLaBandeja()).getVersion()).isNull();
            assertThat(mapper.toJpa(BankReceiptMother.persistida(8730L)).getVersion()).isZero();
        }
    }

    @Nested
    @DisplayName("Vuelta")
    class Vuelta {

        @Test
        @DisplayName("reconstruye el dominio con el mismo contenido")
        void reconstruye_el_dominio_con_el_mismo_contenido() {
            BankReceipt original = BankReceiptMother.enEstado(BankReceiptStatus.DISCARDED);

            BankReceipt vuelta = mapper.toDomain(mapper.toJpa(original));

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getBankReference()).isEqualTo(original.getBankReference());
            assertThat(vuelta.getReceivedOn()).isEqualTo(original.getReceivedOn());
            assertThat(vuelta.getAmount()).isEqualByComparingTo(original.getAmount());
            assertThat(vuelta.getStatus()).isEqualTo(BankReceiptStatus.DISCARDED);
            assertThat(vuelta.getIdentifiedAt()).isEqualTo(original.getIdentifiedAt());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.getVersion()).isEqualTo(original.getVersion());
        }

        @Test
        @DisplayName("un importe negativo sobrevive la ida y la vuelta con su signo")
        void un_importe_negativo_sobrevive_la_ida_y_la_vuelta() {
            BankReceipt cargo = BankReceiptMother.conImporte(new BigDecimal("-45000.00"));

            assertThat(mapper.toDomain(mapper.toJpa(cargo)).getAmount())
                    .isEqualByComparingTo("-45000.00");
        }

        @Test
        @DisplayName("la descripcion nula no se convierte en cadena vacia")
        void la_descripcion_nula_sigue_nula() {
            BankReceipt sinConcepto = new BankReceipt(8731L, BankReceiptMother.CUENTA,
                    BankReceiptMother.REFERENCIA, BankReceiptMother.RECIBIDA_EL,
                    BankReceiptMother.IMPORTE, null, BankReceiptStatus.UNIDENTIFIED, null,
                    BankReceiptMother.CREADA_EL, 0L);

            assertThat(mapper.toDomain(mapper.toJpa(sinConcepto)).getDescription()).isNull();
        }
    }
}
