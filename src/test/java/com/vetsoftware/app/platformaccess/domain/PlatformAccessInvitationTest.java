package com.vetsoftware.app.platformaccess.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.platformaccess.testsupport.PlatformAccessMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La invitación es la credencial que convierte a quien la posee en
 * superadministrador. Sus invariantes son las que impiden que exista una fila
 * capaz de crear una cuenta dos veces, o de crearla sin dejar rastro de qué
 * usuario salió de ella.
 */
@DisplayName("PlatformAccessInvitation — invariantes de la credencial de alta")
class PlatformAccessInvitationTest {

    private static final LocalDateTime CREADA = PlatformAccessMother.AHORA.minusHours(1);
    private static final LocalDateTime EXPIRA = PlatformAccessMother.AHORA.plusDays(7);
    private static final String HASH = PlatformAccessMother.hashDe("invitacion");

    @Nested
    @DisplayName("emision")
    class Emision {

        @Test
        @DisplayName("issue nace sin consumir, sin usuario y sin id")
        void issue_nace_sin_consumir() {
            PlatformAccessInvitation invitacion = PlatformAccessInvitation.issue(4271L, HASH,
                    CREADA, EXPIRA);

            assertThat(invitacion.getId()).isNull();
            assertThat(invitacion.getAccessRequestId()).isEqualTo(4271L);
            assertThat(invitacion.getTokenHash()).isEqualTo(HASH);
            assertThat(invitacion.getConsumedAt()).isNull();
            assertThat(invitacion.getSystemUserId()).isNull();
            assertThat(invitacion.isConsumed()).isFalse();
        }

        @Test
        @DisplayName("una invitacion sin solicitud no puede existir: nadie sabria a quien pertenece")
        void sin_solicitud_no_puede_existir() {
            assertThatThrownBy(() -> PlatformAccessInvitation.issue(null, HASH, CREADA, EXPIRA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("accessRequestId");
        }

        @Test
        @DisplayName("un hash que no mide 64 hex se rechaza: la columna es UNIQUE de 64")
        void un_hash_de_otra_longitud_se_rechaza() {
            assertThatThrownBy(() -> PlatformAccessInvitation.issue(4271L, "corto", CREADA, EXPIRA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("64 char hex digest");
        }

        @Test
        @DisplayName("un hash nulo se rechaza igual que uno corto")
        void un_hash_nulo_se_rechaza() {
            assertThatThrownBy(() -> PlatformAccessInvitation.issue(4271L, null, CREADA, EXPIRA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("64 char hex digest");
        }

        @Test
        @DisplayName("sin fecha de creacion no hay contra que medir la caducidad")
        void sin_fecha_de_creacion_se_rechaza() {
            assertThatThrownBy(() -> PlatformAccessInvitation.issue(4271L, HASH, null, EXPIRA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("createdDate");
        }

        @Test
        @DisplayName("no puede caducar antes de emitirse")
        void no_puede_caducar_antes_de_emitirse() {
            assertThatThrownBy(
                    () -> PlatformAccessInvitation.issue(4271L, HASH, CREADA, CREADA.minusDays(1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expiresAt must be after createdDate");
        }

        @Test
        @DisplayName("caducar en el mismo instante de emitirse tampoco vale")
        void caducar_en_el_mismo_instante_no_vale() {
            assertThatThrownBy(() -> PlatformAccessInvitation.issue(4271L, HASH, CREADA, CREADA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expiresAt must be after createdDate");
        }
    }

    @Nested
    @DisplayName("el par consumo/usuario")
    class ParDeConsumo {

        @Test
        @DisplayName("consumida sin usuario es una cuenta que nadie sabe cual fue")
        void consumida_sin_usuario_se_rechaza() {
            assertThatThrownBy(() -> new PlatformAccessInvitation(88L, 4271L, HASH, EXPIRA,
                    PlatformAccessMother.AHORA, null, CREADA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consumedAt and systemUserId must be set together");
        }

        @Test
        @DisplayName("usuario sin fecha de consumo es una cuenta creada sin rastro de cuando")
        void usuario_sin_consumo_se_rechaza() {
            assertThatThrownBy(() -> new PlatformAccessInvitation(88L, 4271L, HASH, EXPIRA, null,
                    9001L, CREADA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consumedAt and systemUserId must be set together");
        }

        @Test
        @DisplayName("no se puede consumir antes de emitirse")
        void no_se_puede_consumir_antes_de_emitirse() {
            assertThatThrownBy(() -> new PlatformAccessInvitation(88L, 4271L, HASH, EXPIRA,
                    CREADA.minusMinutes(1), 9001L, CREADA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consumedAt cannot precede createdDate");
        }
    }

    @Nested
    @DisplayName("isUsable — la puerta que abre el alta")
    class Usabilidad {

        @Test
        @DisplayName("una invitacion viva y sin consumir es usable")
        void una_invitacion_viva_es_usable() {
            assertThat(PlatformAccessMother.invitacionViva().isUsable(PlatformAccessMother.AHORA))
                    .isTrue();
        }

        @Test
        @DisplayName("una invitacion ya consumida deja de ser usable aunque no haya caducado")
        void una_invitacion_consumida_no_es_usable() {
            PlatformAccessInvitation consumida = PlatformAccessMother.invitacionConsumida();

            assertThat(consumida.isConsumed()).isTrue();
            assertThat(consumida.isUsable(PlatformAccessMother.AHORA)).isFalse();
        }

        @Test
        @DisplayName("una invitacion caducada deja de ser usable aunque no se haya consumido")
        void una_invitacion_caducada_no_es_usable() {
            assertThat(
                    PlatformAccessMother.invitacionCaducada().isUsable(PlatformAccessMother.AHORA))
                    .isFalse();
        }

        @Test
        @DisplayName("el instante exacto de caducidad todavia vale: el corte es estrictamente posterior")
        void el_instante_exacto_de_caducidad_todavia_vale() {
            PlatformAccessInvitation invitacion = PlatformAccessInvitation.issue(4271L, HASH,
                    CREADA, EXPIRA);

            assertThat(invitacion.isUsable(EXPIRA)).isTrue();
            assertThat(invitacion.isUsable(EXPIRA.plusNanos(1000))).isFalse();
        }
    }
}
