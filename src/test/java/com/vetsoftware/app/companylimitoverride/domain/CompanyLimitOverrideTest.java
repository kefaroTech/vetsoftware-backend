package com.vetsoftware.app.companylimitoverride.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyLimitOverride — la excepción negociada, con su historia")
class CompanyLimitOverrideTest {

    private static final Long ANA = 42L;
    private static final Long EJE_ANIMAL = 1L;
    private static final Long COMERCIAL = 3L;
    private static final LocalDate CATORCE_DE_MARZO = LocalDate.of(2026, 3, 14);
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 3, 14, 16, 0);

    private static CompanyLimitOverride trescientasMascotas() {
        return CompanyLimitOverride.grant(ANA, EJE_ANIMAL, 300, CATORCE_DE_MARZO,
                OverrideReasonCode.RETENTION,
                "Retención — llamada del 14/03, aprobada por Dirección Comercial", COMERCIAL,
                CREADA);
    }

    @Nested
    @DisplayName("R-LIMIT-34 · sin motivo escrito no hay excepción")
    class MotivoObligatorio {

        @Test
        @DisplayName("crear una excepción de techo sin motivo escrito se rechaza")
        void crear_una_excepcion_de_techo_sin_motivo_escrito_se_rechaza() {
            assertThatThrownBy(() -> CompanyLimitOverride.grant(ANA, EJE_ANIMAL, 300,
                    CATORCE_DE_MARZO, OverrideReasonCode.RETENTION, "   ", COMERCIAL, CREADA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nobody can defend");
        }

        @Test
        @DisplayName("crear una excepción sin código de motivo se rechaza: no se podría agrupar")
        void crear_una_excepcion_sin_codigo_de_motivo_se_rechaza() {
            assertThatThrownBy(() -> CompanyLimitOverride.grant(ANA, EJE_ANIMAL, 300,
                    CATORCE_DE_MARZO, null, "Retención", COMERCIAL, CREADA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason code");
        }

        @Test
        @DisplayName("crear una excepción sin firma se rechaza")
        void crear_una_excepcion_sin_firma_se_rechaza() {
            assertThatThrownBy(() -> CompanyLimitOverride.grant(ANA, EJE_ANIMAL, 300,
                    CATORCE_DE_MARZO, OverrideReasonCode.RETENTION, "Retención", null, CREADA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("granted by system user id");
        }
    }

    @Nested
    @DisplayName("R-LIMIT-35 · «viva» son las dos condiciones")
    class Vigencia {

        @Test
        @DisplayName("una excepción recién concedida está viva")
        void una_excepcion_recien_concedida_esta_viva() {
            assertThat(trescientasMascotas().isAlive()).isTrue();
        }

        @Test
        @DisplayName("una excepción revocada ayer no bloquea abrir otra hoy sobre el mismo eje")
        void una_excepcion_revocada_ayer_no_bloquea_abrir_otra_hoy_sobre_el_mismo_eje() {
            CompanyLimitOverride revocada = trescientasMascotas().revoke(
                    LocalDateTime.of(2026, 6, 1, 9, 0), COMERCIAL,
                    OverrideReasonCode.COMMERCIAL_AGREEMENT, "Pasa a plan de pago");

            assertThat(revocada.isAlive()).isFalse();
            assertThat(revocada.getValidTo()).isEqualTo(LocalDate.of(2026, 6, 1));
        }

        @Test
        @DisplayName("responde qué techo regía el 14 de marzo sin reconstruir nada")
        void responde_que_techo_regia_el_14_de_marzo() {
            CompanyLimitOverride revocada = trescientasMascotas().revoke(
                    LocalDateTime.of(2026, 6, 1, 9, 0), COMERCIAL,
                    OverrideReasonCode.COMMERCIAL_AGREEMENT, "Pasa a plan de pago");

            assertThat(revocada.rulesOn(CATORCE_DE_MARZO)).isTrue();
            assertThat(revocada.rulesOn(LocalDate.of(2026, 3, 13))).isFalse();
            assertThat(revocada.rulesOn(LocalDate.of(2026, 7, 1))).isFalse();
        }

        @Test
        @DisplayName("revocar dos veces se rechaza: movería la fecha auditada")
        void revocar_dos_veces_se_rechaza() {
            CompanyLimitOverride revocada = trescientasMascotas().revoke(
                    LocalDateTime.of(2026, 6, 1, 9, 0), COMERCIAL,
                    OverrideReasonCode.COMMERCIAL_AGREEMENT, "Pasa a plan de pago");

            assertThatThrownBy(() -> revocada.revoke(LocalDateTime.of(2026, 6, 2, 9, 0), COMERCIAL,
                    OverrideReasonCode.OTHER, "otra vez"))
                    .isInstanceOf(OverrideAlreadyRevokedException.class);
        }

        @Test
        @DisplayName("revocar sin motivo escrito se rechaza igual que conceder")
        void revocar_sin_motivo_escrito_se_rechaza() {
            CompanyLimitOverride viva = trescientasMascotas();

            assertThatThrownBy(() -> viva.revoke(LocalDateTime.of(2026, 6, 1, 9, 0), COMERCIAL,
                    OverrideReasonCode.OTHER, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("revoked reason");
        }

        @Test
        @DisplayName("datos de revocación sin fecha de revocación no son una revocación")
        void datos_de_revocacion_sin_fecha_se_rechazan() {
            assertThatThrownBy(() -> new CompanyLimitOverride(1L, ANA, EJE_ANIMAL, 300,
                    CATORCE_DE_MARZO, null, OverrideReasonCode.RETENTION, "Retención", COMERCIAL,
                    COMERCIAL, null, null, null, CREADA, true, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a revocation");
        }

        @Test
        @DisplayName("una vigencia que acaba antes de empezar se rechaza")
        void una_vigencia_invertida_se_rechaza() {
            assertThatThrownBy(() -> new CompanyLimitOverride(1L, ANA, EJE_ANIMAL, 300,
                    CATORCE_DE_MARZO, LocalDate.of(2026, 3, 1), OverrideReasonCode.RETENTION,
                    "Retención", COMERCIAL, null, null, null, null, CREADA, true, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot precede valid from");
        }
    }
}
