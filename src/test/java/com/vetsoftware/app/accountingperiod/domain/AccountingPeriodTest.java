package com.vetsoftware.app.accountingperiod.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.accountingperiod.testsupport.AccountingPeriodMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AccountingPeriod — el mes contable y su ciclo de cierre")
class AccountingPeriodTest {

    private static final Long ID = 8800L;
    private static final LocalDateTime CIERRE = AccountingPeriodMother.CERRADO_EL;
    private static final LocalDateTime REAPERTURA = AccountingPeriodMother.REABIERTO_EL;

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("un mes nace OPEN, sin cierre y sin reapertura")
        void un_mes_nace_abierto_y_limpio() {
            AccountingPeriod marzo = AccountingPeriod.open(AccountingPeriodMother.MARZO,
                    AccountingPeriodMother.CREADO_EL);

            assertThat(marzo.getId()).isNull();
            assertThat(marzo.getPeriodKey()).isEqualTo(AccountingPeriodMother.MARZO);
            assertThat(marzo.getStatus()).isEqualTo(AccountingPeriodStatus.OPEN);
            assertThat(marzo.getClosedAt()).isNull();
            assertThat(marzo.getClosedBySystemUserId()).isNull();
            assertThat(marzo.getReopenedAt()).isNull();
            assertThat(marzo.getReopenedBySystemUserId()).isNull();
            assertThat(marzo.getReopenedReason()).isNull();
            assertThat(marzo.getCreatedDate()).isEqualTo(AccountingPeriodMother.CREADO_EL);
            assertThat(marzo.getVersion()).isNull();
        }

        @Test
        @DisplayName("un mes recien abierto admite registros")
        void un_mes_recien_abierto_admite_registros() {
            assertThat(AccountingPeriodMother.abierto().acceptsPostings()).isTrue();
            assertThat(AccountingPeriodMother.abierto().isReopened()).isFalse();
        }

        @Test
        @DisplayName("sin clave de mes no hay periodo")
        void sin_clave_de_mes_no_hay_periodo() {
            assertThatThrownBy(() -> AccountingPeriod.open(null, AccountingPeriodMother.CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("periodKey is required");
        }
    }

    @Nested
    @DisplayName("Cierre en blando")
    class CierreEnBlando {

        @Test
        @DisplayName("cerrar sella el estado, la hora y la firma, y deja de admitir registros")
        void cerrar_sella_estado_hora_y_firma() {
            AccountingPeriod marzo = AccountingPeriodMother.persistidoAbierto(ID);

            marzo.softClose(AccountingPeriodMother.CERRADO_POR, CIERRE);

            assertThat(marzo.getStatus()).isEqualTo(AccountingPeriodStatus.SOFT_CLOSED);
            assertThat(marzo.getClosedAt()).isEqualTo(CIERRE);
            assertThat(marzo.getClosedBySystemUserId())
                    .isEqualTo(AccountingPeriodMother.CERRADO_POR);
            assertThat(marzo.acceptsPostings()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = AccountingPeriodStatus.class, names = {"SOFT_CLOSED", "LOCKED"})
        @DisplayName("cerrar un mes ya cerrado o declarado es un conflicto, no una repeticion")
        void cerrar_un_mes_ya_cerrado_es_un_conflicto(AccountingPeriodStatus estado) {
            // El cierre mensual disparado dos veces —a mano y por el programador de
            // tareas— tiene que decir que el trabajo ya estaba hecho, no volver a
            // firmar el mes con otra hora y otra persona.
            AccountingPeriod cerrado = enEstado(estado);

            assertThatThrownBy(
                    () -> cerrado.softClose(AccountingPeriodMother.REABIERTO_POR, REAPERTURA))
                    .isInstanceOf(AccountingPeriodAlreadyClosedException.class)
                    .hasMessageContaining("is already closed with status " + estado);
            assertThat(cerrado.getClosedAt()).isEqualTo(CIERRE);
            assertThat(cerrado.getClosedBySystemUserId())
                    .isEqualTo(AccountingPeriodMother.CERRADO_POR);
        }

        @Test
        @DisplayName("cerrar sin firma se rechaza: la columna del firmante es obligatoria")
        void cerrar_sin_firma_se_rechaza() {
            AccountingPeriod marzo = AccountingPeriodMother.persistidoAbierto(ID);

            assertThatThrownBy(() -> marzo.softClose(null, CIERRE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("closedBySystemUserId is required");
            assertThat(marzo.getStatus()).isEqualTo(AccountingPeriodStatus.OPEN);
        }

        @Test
        @DisplayName("cerrar sin hora se rechaza y el mes sigue abierto")
        void cerrar_sin_hora_se_rechaza() {
            AccountingPeriod marzo = AccountingPeriodMother.persistidoAbierto(ID);

            assertThatThrownBy(() -> marzo.softClose(AccountingPeriodMother.CERRADO_POR, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("closedAt is required");
            assertThat(marzo.getStatus()).isEqualTo(AccountingPeriodStatus.OPEN);
        }
    }

    @Nested
    @DisplayName("Declaracion")
    class Declaracion {

        @Test
        @DisplayName("declarar un mes abierto lo sella y lo bloquea en un solo acto")
        void declarar_un_mes_abierto_lo_sella_y_lo_bloquea() {
            AccountingPeriod marzo = AccountingPeriodMother.persistidoAbierto(ID);

            marzo.lock(AccountingPeriodMother.CERRADO_POR, CIERRE);

            assertThat(marzo.getStatus()).isEqualTo(AccountingPeriodStatus.LOCKED);
            assertThat(marzo.getClosedAt()).isEqualTo(CIERRE);
            assertThat(marzo.getClosedBySystemUserId())
                    .isEqualTo(AccountingPeriodMother.CERRADO_POR);
        }

        @Test
        @DisplayName("declarar un mes ya cerrado NO reescribe quien lo cerro")
        void declarar_un_mes_ya_cerrado_no_reescribe_la_firma() {
            // La tabla guarda UN cierre, no una pila. Sobrescribirlo aqui borraria a
            // quien cerro el mes para poner a quien lo declaro, y la firma del cierre es
            // justo el dato por el que se pregunta.
            AccountingPeriod marzo = AccountingPeriodMother.cerradoEnBlando(ID);

            marzo.lock(AccountingPeriodMother.REABIERTO_POR, REAPERTURA);

            assertThat(marzo.getStatus()).isEqualTo(AccountingPeriodStatus.LOCKED);
            assertThat(marzo.getClosedAt()).isEqualTo(CIERRE);
            assertThat(marzo.getClosedBySystemUserId())
                    .isEqualTo(AccountingPeriodMother.CERRADO_POR);
        }

        @Test
        @DisplayName("declarar dos veces es un conflicto")
        void declarar_dos_veces_es_un_conflicto() {
            AccountingPeriod declarado = AccountingPeriodMother.declarado(ID);

            assertThatThrownBy(() -> declarado.lock(AccountingPeriodMother.CERRADO_POR, REAPERTURA))
                    .isInstanceOf(AccountingPeriodAlreadyClosedException.class)
                    .hasMessageContaining("is already closed with status LOCKED");
        }
    }

    @Nested
    @DisplayName("Reapertura")
    class Reapertura {

        @Test
        @DisplayName("reabrir vuelve a OPEN y CONSERVA el cierre previo como registro")
        void reabrir_conserva_el_cierre_previo() {
            // Borrar closedAt dejaria una reapertura indistinguible de un mes que nunca
            // se cerro, y con ella la firma del cierre que se deshizo.
            AccountingPeriod marzo = AccountingPeriodMother.cerradoEnBlando(ID);

            marzo.reopen(AccountingPeriodMother.REABIERTO_POR, REAPERTURA,
                    AccountingPeriodMother.MOTIVO);

            assertThat(marzo.getStatus()).isEqualTo(AccountingPeriodStatus.OPEN);
            assertThat(marzo.acceptsPostings()).isTrue();
            assertThat(marzo.isReopened()).isTrue();
            assertThat(marzo.getClosedAt()).isEqualTo(CIERRE);
            assertThat(marzo.getClosedBySystemUserId())
                    .isEqualTo(AccountingPeriodMother.CERRADO_POR);
            assertThat(marzo.getReopenedAt()).isEqualTo(REAPERTURA);
            assertThat(marzo.getReopenedBySystemUserId())
                    .isEqualTo(AccountingPeriodMother.REABIERTO_POR);
            assertThat(marzo.getReopenedReason()).isEqualTo(AccountingPeriodMother.MOTIVO);
        }

        @Test
        @DisplayName("un mes LOCKED no se reabre nunca, y lo dice con su propia excepcion")
        void un_mes_declarado_no_se_reabre_nunca() {
            AccountingPeriod declarado = AccountingPeriodMother.declarado(ID);

            assertThatThrownBy(() -> declarado.reopen(AccountingPeriodMother.REABIERTO_POR,
                    REAPERTURA, AccountingPeriodMother.MOTIVO))
                    .isInstanceOf(LockedAccountingPeriodCannotBeReopenedException.class)
                    .hasMessageContaining("is locked and cannot be reopened");
            assertThat(declarado.getStatus()).isEqualTo(AccountingPeriodStatus.LOCKED);
            assertThat(declarado.getReopenedAt()).isNull();
        }

        @Test
        @DisplayName("reabrir un mes que ya estaba abierto dice que no estaba cerrado")
        void reabrir_un_mes_abierto_dice_que_no_estaba_cerrado() {
            // Decir aqui «ya esta cerrado» mandaria a quien lo lee a buscar el problema
            // donde no esta.
            AccountingPeriod abierto = AccountingPeriodMother.persistidoAbierto(ID);

            assertThatThrownBy(() -> abierto.reopen(AccountingPeriodMother.REABIERTO_POR,
                    REAPERTURA, AccountingPeriodMother.MOTIVO))
                    .isInstanceOf(AccountingPeriodNotClosedException.class)
                    .hasMessageContaining("is not closed, its status is OPEN");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("reabrir sin motivo escrito se rechaza y el mes sigue cerrado")
        void reabrir_sin_motivo_se_rechaza(String motivo) {
            // Un cierre que cualquiera deshace sin decir por que no significa nada: es
            // la operacion que un revisor mira primero.
            AccountingPeriod marzo = AccountingPeriodMother.cerradoEnBlando(ID);

            assertThatThrownBy(
                    () -> marzo.reopen(AccountingPeriodMother.REABIERTO_POR, REAPERTURA, motivo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reopenedReason is required");
            assertThat(marzo.getStatus()).isEqualTo(AccountingPeriodStatus.SOFT_CLOSED);
        }

        @Test
        @DisplayName("un motivo de mas de 255 caracteres se rechaza antes de llegar a la base")
        void un_motivo_demasiado_largo_se_rechaza() {
            AccountingPeriod marzo = AccountingPeriodMother.cerradoEnBlando(ID);

            assertThatThrownBy(() -> marzo.reopen(AccountingPeriodMother.REABIERTO_POR, REAPERTURA,
                    "M".repeat(256))).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reopenedReason must be 255 chars or less");
        }

        @Test
        @DisplayName("una reapertura anterior al cierre invierte el orden de los hechos y se rechaza")
        void una_reapertura_anterior_al_cierre_se_rechaza() {
            AccountingPeriod marzo = AccountingPeriodMother.cerradoEnBlando(ID);

            assertThatThrownBy(() -> marzo.reopen(AccountingPeriodMother.REABIERTO_POR,
                    CIERRE.minusSeconds(1), AccountingPeriodMother.MOTIVO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reopenedAt cannot be before closedAt");
            assertThat(marzo.getStatus()).isEqualTo(AccountingPeriodStatus.SOFT_CLOSED);
        }

        @Test
        @DisplayName("reabrir en el mismo instante del cierre si vale: la condicion es >=")
        void reabrir_en_el_mismo_instante_del_cierre_vale() {
            // Espejo literal de la constraint, que dice reopened_at >= closed_at. Un
            // cierre y una reapertura en el mismo segundo es raro y no es imposible.
            AccountingPeriod marzo = AccountingPeriodMother.cerradoEnBlando(ID);

            marzo.reopen(AccountingPeriodMother.REABIERTO_POR, CIERRE,
                    AccountingPeriodMother.MOTIVO);

            assertThat(marzo.getReopenedAt()).isEqualTo(CIERRE);
        }

        @Test
        @DisplayName("reabrir sin firma se rechaza y el mes sigue cerrado")
        void reabrir_sin_firma_se_rechaza() {
            AccountingPeriod marzo = AccountingPeriodMother.cerradoEnBlando(ID);

            assertThatThrownBy(() -> marzo.reopen(null, REAPERTURA, AccountingPeriodMother.MOTIVO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reopenedBySystemUserId is required");
            assertThat(marzo.getStatus()).isEqualTo(AccountingPeriodStatus.SOFT_CLOSED);
        }

        @Test
        @DisplayName("reabrir sin hora se rechaza y el mes sigue cerrado")
        void reabrir_sin_hora_se_rechaza() {
            AccountingPeriod marzo = AccountingPeriodMother.cerradoEnBlando(ID);

            assertThatThrownBy(() -> marzo.reopen(AccountingPeriodMother.REABIERTO_POR, null,
                    AccountingPeriodMother.MOTIVO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reopenedAt is required");
            assertThat(marzo.getStatus()).isEqualTo(AccountingPeriodStatus.SOFT_CLOSED);
        }

        @Test
        @DisplayName("un mes reabierto se puede volver a cerrar y la reapertura queda registrada")
        void un_mes_reabierto_se_puede_volver_a_cerrar() {
            // El ciclo completo de la ficha: cerrar, reabrir con motivo, cerrar otra
            // vez. La segunda firma sustituye a la primera —hay una sola columna de
            // cierre— y el rastro de la reapertura se conserva entero.
            AccountingPeriod marzo = AccountingPeriodMother.reabierto(ID);

            marzo.softClose(AccountingPeriodMother.REABIERTO_POR, REAPERTURA.plusDays(1));

            assertThat(marzo.getStatus()).isEqualTo(AccountingPeriodStatus.SOFT_CLOSED);
            assertThat(marzo.getClosedAt()).isEqualTo(REAPERTURA.plusDays(1));
            assertThat(marzo.getClosedBySystemUserId())
                    .isEqualTo(AccountingPeriodMother.REABIERTO_POR);
            assertThat(marzo.getReopenedReason()).isEqualTo(AccountingPeriodMother.MOTIVO);
        }
    }

    @Nested
    @DisplayName("Reconstruccion — espejo de las dos CHECK")
    class Reconstruccion {

        @Test
        @DisplayName("un mes abierto no puede llevar cierre si nunca se reabrio")
        void un_mes_abierto_no_puede_llevar_cierre() {
            assertThatThrownBy(() -> new AccountingPeriod(ID, AccountingPeriodMother.MARZO,
                    AccountingPeriodStatus.OPEN, CIERRE, AccountingPeriodMother.CERRADO_POR, null,
                    null, null, AccountingPeriodMother.CREADO_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("an open period cannot carry a closure");
        }

        @ParameterizedTest
        @EnumSource(value = AccountingPeriodStatus.class, names = {"SOFT_CLOSED", "LOCKED"})
        @DisplayName("un mes cerrado o declarado no puede quedarse sin cierre")
        void un_mes_cerrado_no_puede_quedarse_sin_cierre(AccountingPeriodStatus estado) {
            assertThatThrownBy(() -> new AccountingPeriod(ID, AccountingPeriodMother.MARZO, estado,
                    null, null, null, null, null, AccountingPeriodMother.CREADO_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("closedAt is required once closed");
        }

        @Test
        @DisplayName("la hora y la firma del cierre van juntas o no van")
        void la_hora_y_la_firma_del_cierre_van_juntas() {
            assertThatThrownBy(() -> new AccountingPeriod(ID, AccountingPeriodMother.MARZO,
                    AccountingPeriodStatus.SOFT_CLOSED, CIERRE, null, null, null, null,
                    AccountingPeriodMother.CREADO_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("closedAt and closedBySystemUserId must be set together");
        }

        @Test
        @DisplayName("los tres campos de reapertura van juntos o no van")
        void los_tres_campos_de_reapertura_van_juntos() {
            assertThatThrownBy(() -> new AccountingPeriod(ID, AccountingPeriodMother.MARZO,
                    AccountingPeriodStatus.OPEN, CIERRE, AccountingPeriodMother.CERRADO_POR,
                    REAPERTURA, AccountingPeriodMother.REABIERTO_POR, null,
                    AccountingPeriodMother.CREADO_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be set together");
        }

        @Test
        @DisplayName("una reapertura sin cierre previo es imposible de reconstruir")
        void una_reapertura_sin_cierre_previo_es_imposible() {
            assertThatThrownBy(() -> new AccountingPeriod(ID, AccountingPeriodMother.MARZO,
                    AccountingPeriodStatus.OPEN, null, null, REAPERTURA,
                    AccountingPeriodMother.REABIERTO_POR, AccountingPeriodMother.MOTIVO,
                    AccountingPeriodMother.CREADO_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must keep its previous closure");
        }

        @Test
        @DisplayName("un mes reabierto SI conserva su cierre, que es la unica divergencia con el CHECK")
        void un_mes_reabierto_conserva_su_cierre() {
            // Es la forma que chk_accounting_periods_closure rechaza hoy y la unica que
            // tiene sentido de negocio. Ver el javadoc de la clase y el caso homonimo de
            // AccountingPeriodPersistenceIT.
            AccountingPeriod reabierto = AccountingPeriodMother.reabierto(ID);

            assertThat(reabierto.getStatus()).isEqualTo(AccountingPeriodStatus.OPEN);
            assertThat(reabierto.getClosedAt()).isEqualTo(CIERRE);
            assertThat(reabierto.isReopened()).isTrue();
        }

        @Test
        @DisplayName("sin estado no hay periodo")
        void sin_estado_no_hay_periodo() {
            assertThatThrownBy(() -> new AccountingPeriod(ID, AccountingPeriodMother.MARZO, null,
                    null, null, null, null, null, AccountingPeriodMother.CREADO_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status is required");
        }
    }

    private static AccountingPeriod enEstado(AccountingPeriodStatus estado) {
        return new AccountingPeriod(ID, AccountingPeriodMother.MARZO, estado, CIERRE,
                AccountingPeriodMother.CERRADO_POR, null, null, null,
                AccountingPeriodMother.CREADO_EL, 0L);
    }
}
