package com.vetsoftware.app.accountingperiod.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
import com.vetsoftware.app.accountingperiod.testsupport.AccountingPeriodMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AccountingPeriodJpaMapper — la frontera dominio/JPA")
class AccountingPeriodJpaMapperTest {

    private static final Long ID = 8800L;

    private final AccountingPeriodJpaMapper mapper = new AccountingPeriodJpaMapper();

    @Nested
    @DisplayName("Hacia la entidad JPA")
    class HaciaLaEntidad {

        @Test
        @DisplayName("lleva los diez campos del periodo reabierto, la version incluida")
        void lleva_los_diez_campos_con_la_version() {
            // Sin la version en el ida, cada save de un periodo ya persistido le daria a
            // Hibernate una version nula y el UPDATE se convertiria en INSERT: el
            // bloqueo optimista dejaria de proteger justo las dos operaciones que mutan
            // el estado.
            AccountingPeriodJpaEntity entity = mapper.toJpa(AccountingPeriodMother.reabierto(ID));

            assertThat(entity.getId()).isEqualTo(ID);
            assertThat(entity.getPeriodKey()).isEqualTo("2026-03");
            assertThat(entity.getStatus()).isEqualTo(AccountingPeriodStatus.OPEN);
            assertThat(entity.getClosedAt()).isEqualTo(AccountingPeriodMother.CERRADO_EL);
            assertThat(entity.getClosedBySystemUserId())
                    .isEqualTo(AccountingPeriodMother.CERRADO_POR);
            assertThat(entity.getReopenedAt()).isEqualTo(AccountingPeriodMother.REABIERTO_EL);
            assertThat(entity.getReopenedBySystemUserId())
                    .isEqualTo(AccountingPeriodMother.REABIERTO_POR);
            assertThat(entity.getReopenedReason()).isEqualTo(AccountingPeriodMother.MOTIVO);
            assertThat(entity.getCreatedDate()).isEqualTo(AccountingPeriodMother.CREADO_EL);
            assertThat(entity.getVersion()).isZero();
        }

        @Test
        @DisplayName("la clave del mes se aplana a la cadena que guarda el CHAR(7)")
        void la_clave_se_aplana_a_cadena() {
            assertThat(mapper.toJpa(AccountingPeriodMother.abierto()).getPeriodKey())
                    .isEqualTo("2026-03");
        }

        @Test
        @DisplayName("un periodo sin persistir viaja con id y version nulos")
        void un_periodo_sin_persistir_viaja_con_id_y_version_nulos() {
            AccountingPeriodJpaEntity entity = mapper.toJpa(AccountingPeriodMother.abierto());

            assertThat(entity.getId()).isNull();
            assertThat(entity.getVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("Hacia el dominio")
    class HaciaElDominio {

        @Test
        @DisplayName("la ida y vuelta conserva los diez campos")
        void la_ida_y_vuelta_conserva_los_diez_campos() {
            AccountingPeriod original = AccountingPeriodMother.reabierto(ID);

            AccountingPeriod vuelta = mapper.toDomain(mapper.toJpa(original));

            assertThat(vuelta.getId()).isEqualTo(ID);
            assertThat(vuelta.getPeriodKey()).isEqualTo(AccountingPeriodMother.MARZO);
            assertThat(vuelta.getStatus()).isEqualTo(AccountingPeriodStatus.OPEN);
            assertThat(vuelta.getClosedAt()).isEqualTo(AccountingPeriodMother.CERRADO_EL);
            assertThat(vuelta.getClosedBySystemUserId())
                    .isEqualTo(AccountingPeriodMother.CERRADO_POR);
            assertThat(vuelta.getReopenedAt()).isEqualTo(AccountingPeriodMother.REABIERTO_EL);
            assertThat(vuelta.getReopenedBySystemUserId())
                    .isEqualTo(AccountingPeriodMother.REABIERTO_POR);
            assertThat(vuelta.getReopenedReason()).isEqualTo(AccountingPeriodMother.MOTIVO);
            assertThat(vuelta.getCreatedDate()).isEqualTo(AccountingPeriodMother.CREADO_EL);
            assertThat(vuelta.getVersion()).isZero();
        }

        @Test
        @DisplayName("la cadena de la columna vuelve a ser el value object del dominio")
        void la_cadena_vuelve_a_ser_el_value_object() {
            // La vuelta pasa por AccountingPeriodKey.of, que revalida el formato: una
            // fila escrita por SQL crudo entraria si no al dominio como un mes
            // imposible, y el fallo apareceria mucho despues al comparar claves.
            AccountingPeriod vuelta = mapper
                    .toDomain(mapper.toJpa(AccountingPeriodMother.cerradoEnBlando(ID)));

            assertThat(vuelta.getPeriodKey()).isEqualTo(AccountingPeriodMother.MARZO);
            assertThat(vuelta.getPeriodKey().value()).isEqualTo("2026-03");
        }

        @Test
        @DisplayName("un mes abierto vuelve sin cierre y sin reapertura")
        void un_mes_abierto_vuelve_limpio() {
            AccountingPeriod vuelta = mapper
                    .toDomain(mapper.toJpa(AccountingPeriodMother.persistidoAbierto(ID)));

            assertThat(vuelta.getClosedAt()).isNull();
            assertThat(vuelta.getClosedBySystemUserId()).isNull();
            assertThat(vuelta.getReopenedAt()).isNull();
            assertThat(vuelta.getReopenedBySystemUserId()).isNull();
            assertThat(vuelta.getReopenedReason()).isNull();
            assertThat(vuelta.acceptsPostings()).isTrue();
        }
    }
}
