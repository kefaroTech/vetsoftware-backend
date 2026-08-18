package com.vetsoftware.app.membershipsubmodule.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.vetsoftware.app.membershipsubmodule.testsupport.MembershipSubModuleMother;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MembershipSubModule — invariantes y ciclo de vida del agregado")
class MembershipSubModuleTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            MembershipSubModule relacion = MembershipSubModuleMother.activa();

            assertThat(relacion.getId()).isEqualTo(MembershipSubModuleMother.RELATION_ID);
            assertThat(relacion.getMembership()).isEqualTo(MembershipSubModuleMother.PLAN_PREMIUM);
            assertThat(relacion.getSubModule()).isEqualTo(MembershipSubModuleMother.FACTURACION);
            assertThat(relacion.getCreatedDate()).isEqualTo(MembershipSubModuleMother.CREADO);
            assertThat(relacion.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("rechaza membership null")
        void rechaza_membership_null() {
            assertThatThrownBy(() -> new MembershipSubModule(null, null,
                    MembershipSubModuleMother.FACTURACION, MembershipSubModuleMother.CREADO, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("membership is required");
        }

        @Test
        @DisplayName("rechaza subModule null")
        void rechaza_sub_module_null() {
            assertThatThrownBy(
                    () -> new MembershipSubModule(null, MembershipSubModuleMother.PLAN_PREMIUM,
                            null, MembershipSubModuleMother.CREADO, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subModule is required");
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con la fecha del reloj del sistema")
        void create_nace_sin_id_habilitado() {
            MembershipSubModule relacion = MembershipSubModule.create(
                    MembershipSubModuleMother.PLAN_PREMIUM, MembershipSubModuleMother.FACTURACION);

            assertThat(relacion.getId()).isNull();
            assertThat(relacion.isEnabled()).isTrue();
            // create() pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable,
            // asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo"
            // del CLAUDE.md.
            assertThat(relacion.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza membership y subModule y conserva id y createdDate")
        void reemplaza_membership_y_sub_module() {
            MembershipSubModule relacion = MembershipSubModuleMother.activa();

            relacion.update(MembershipSubModuleMother.OTRO_PLAN,
                    MembershipSubModuleMother.INVENTARIO);

            assertThat(relacion.getMembership()).isEqualTo(MembershipSubModuleMother.OTRO_PLAN);
            assertThat(relacion.getSubModule()).isEqualTo(MembershipSubModuleMother.INVENTARIO);
            assertThat(relacion.getId()).isEqualTo(MembershipSubModuleMother.RELATION_ID);
            assertThat(relacion.getCreatedDate()).isEqualTo(MembershipSubModuleMother.CREADO);
        }

        @Test
        @DisplayName("rechaza membership null y no toca el estado previo")
        void rechaza_membership_null_en_update() {
            MembershipSubModule relacion = MembershipSubModuleMother.activa();

            assertThatThrownBy(() -> relacion.update(null, MembershipSubModuleMother.INVENTARIO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("membership is required");

            assertThat(relacion.getMembership()).isEqualTo(MembershipSubModuleMother.PLAN_PREMIUM);
            assertThat(relacion.getSubModule()).isEqualTo(MembershipSubModuleMother.FACTURACION);
        }

        @Test
        @DisplayName("rechaza subModule null y no toca el estado previo")
        void rechaza_sub_module_null_en_update() {
            MembershipSubModule relacion = MembershipSubModuleMother.activa();

            assertThatThrownBy(() -> relacion.update(MembershipSubModuleMother.OTRO_PLAN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subModule is required");

            assertThat(relacion.getMembership()).isEqualTo(MembershipSubModuleMother.PLAN_PREMIUM);
            assertThat(relacion.getSubModule()).isEqualTo(MembershipSubModuleMother.FACTURACION);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            MembershipSubModule relacion = MembershipSubModuleMother.activa();
            relacion.disable();

            relacion.update(MembershipSubModuleMother.OTRO_PLAN,
                    MembershipSubModuleMother.INVENTARIO);

            assertThat(relacion.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            MembershipSubModule relacion = MembershipSubModuleMother.activa();

            relacion.disable();
            assertThat(relacion.isEnabled()).isFalse();
            relacion.disable();
            assertThat(relacion.isEnabled()).isFalse();

            relacion.enable();
            assertThat(relacion.isEnabled()).isTrue();
            relacion.enable();
            assertThat(relacion.isEnabled()).isTrue();
        }
    }
}
