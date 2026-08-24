package com.vetsoftware.app.entitlement.domain;

import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.AHORA;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.COMPANY_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.SUBSCRIPTION_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.historiaClinica;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("CompanyEntitlement — las invariantes son el espejo de los CHECK")
class CompanyEntitlementTest {

    private static CompanyEntitlement valido() {
        return CompanyEntitlement.derived(COMPANY_ID, historiaClinica(), AccessLevel.FULL,
                EntitlementSource.SUBSCRIPTION, SUBSCRIPTION_ID, 900L, AHORA.minusDays(10), null,
                AHORA);
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("la ventana tiene que cerrar despues de abrir")
        void la_ventana_tiene_que_cerrar_despues_de_abrir() {
            assertThatThrownBy(() -> CompanyEntitlement.derived(COMPANY_ID, historiaClinica(),
                    AccessLevel.FULL, EntitlementSource.SUBSCRIPTION, SUBSCRIPTION_ID, 900L, AHORA,
                    AHORA, AHORA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("valid until");
        }

        @ParameterizedTest
        @EnumSource(value = EntitlementSource.class, names = {"SUBSCRIPTION", "TRIAL"})
        @DisplayName("un permiso que dice venir del contrato tiene que traer el contrato")
        void un_permiso_del_contrato_tiene_que_traer_el_contrato(EntitlementSource source) {
            assertThatThrownBy(() -> CompanyEntitlement.derived(COMPANY_ID, historiaClinica(),
                    AccessLevel.FULL, source, null, 900L, AHORA.minusDays(10), null, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subscription id");
        }

        @ParameterizedTest
        @EnumSource(value = EntitlementSource.class, names = {"CORE", "MANUAL_GRANT"})
        @DisplayName("el nucleo y la concesion a mano pueden no citar contrato")
        void el_nucleo_y_la_concesion_a_mano_pueden_no_citar_contrato(EntitlementSource source) {
            CompanyEntitlement permiso = CompanyEntitlement.derived(COMPANY_ID, historiaClinica(),
                    AccessLevel.FULL, source, null, null, AHORA.minusDays(10), null, AHORA);

            assertThat(permiso.getSubscriptionId()).isNull();
        }

        @Test
        @DisplayName("sin empresa no hay permiso: es una tabla multi-tenant")
        void sin_empresa_no_hay_permiso() {
            assertThatThrownBy(() -> CompanyEntitlement.derived(null, historiaClinica(),
                    AccessLevel.FULL, EntitlementSource.CORE, null, null, AHORA, null, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id");
        }
    }

    @Nested
    @DisplayName("Ventana de validez")
    class VentanaDeValidez {

        @Test
        @DisplayName("sin fecha de fin la ventana no se cierra nunca")
        void sin_fecha_de_fin_la_ventana_no_se_cierra() {
            assertThat(valido().isActiveAt(AHORA.plusYears(5))).isTrue();
        }

        @Test
        @DisplayName("antes de abrir todavia no concede nada")
        void antes_de_abrir_no_concede_nada() {
            assertThat(valido().isActiveAt(AHORA.minusDays(11))).isFalse();
        }

        @Test
        @DisplayName("el submodulo oculto no concede aunque su ventana este abierta")
        void el_submodulo_oculto_no_concede() {
            CompanyEntitlement oculto = CompanyEntitlement.derived(COMPANY_ID, historiaClinica(),
                    AccessLevel.NONE, EntitlementSource.SUBSCRIPTION, SUBSCRIPTION_ID, 900L,
                    AHORA.minusDays(10), null, AHORA);

            assertThat(oculto.isActiveAt(AHORA)).isTrue();
            assertThat(oculto.grantsAt(AHORA)).isFalse();
        }
    }
}
