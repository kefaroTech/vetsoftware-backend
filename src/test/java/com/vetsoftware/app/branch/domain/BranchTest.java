package com.vetsoftware.app.branch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Reglas de negocio del agregado {@link Branch} (multi-sucursal). Verifica creación, actualización,
 * ciclo de estado (active) y las invariantes de validación. Cubre CREATE/READ/UPDATE del dominio y la
 * "eliminación" lógica (deactivate), que NO es soft-delete sino un estado de negocio.
 */
class BranchTest {

    private static final CityRef CITY = new CityRef(5L, "Bogotá");
    private static final CompanyRef COMPANY = new CompanyRef(9L, "Vet SAS", "900123456");
    private static final LocalDateTime CREATED = LocalDateTime.of(2020, 1, 1, 10, 0);

    // ---------- create ----------

    @Test
    void create_nace_activa_sin_id_y_estampa_fecha() {
        Branch b = Branch.create("Sede Norte", "NORTE", "Cra 1 #2-3", "3001112233", CITY, COMPANY);

        assertThat(b.getId()).isNull();
        assertThat(b.getName()).isEqualTo("Sede Norte");
        assertThat(b.getCode()).isEqualTo("NORTE");
        assertThat(b.getAddress()).isEqualTo("Cra 1 #2-3");
        assertThat(b.getPhone()).isEqualTo("3001112233");
        assertThat(b.getCity()).isEqualTo(CITY);
        assertThat(b.getCompany()).isEqualTo(COMPANY);
        assertThat(b.isActive()).as("una sucursal recién creada debe nacer operativa").isTrue();
        assertThat(b.getCreatedDate()).isNotNull();
    }

    @Test
    void create_permite_address_y_phone_nulos() {
        Branch b = Branch.create("Sede", "S1", null, null, CITY, COMPANY);
        assertThat(b.getAddress()).isNull();
        assertThat(b.getPhone()).isNull();
    }

    // ---------- update ----------

    @Test
    void update_cambia_campos_mutables_pero_conserva_company_fecha_id_y_estado() {
        Branch b = new Branch(3L, "Old", "OLD", "addr", "phone", CITY, COMPANY, CREATED, false);
        CityRef nuevaCiudad = new CityRef(7L, "Medellín");

        b.update("New", "NEW", "addr2", "phone2", nuevaCiudad);

        assertThat(b.getName()).isEqualTo("New");
        assertThat(b.getCode()).isEqualTo("NEW");
        assertThat(b.getAddress()).isEqualTo("addr2");
        assertThat(b.getPhone()).isEqualTo("phone2");
        assertThat(b.getCity()).isEqualTo(nuevaCiudad);
        // invariantes: update NO toca identidad, empresa, fecha de creación ni el flag operativo.
        assertThat(b.getId()).isEqualTo(3L);
        assertThat(b.getCompany()).isEqualTo(COMPANY);
        assertThat(b.getCreatedDate()).isEqualTo(CREATED);
        assertThat(b.isActive()).as("update no debe reactivar una sucursal desactivada").isFalse();
    }

    @Test
    void update_invalido_es_atomico_no_muta_estado_previo() {
        Branch b = new Branch(3L, "Old", "OLD", "addr", "phone", CITY, COMPANY, CREATED, true);

        assertThatThrownBy(() -> b.update("  ", "NEW", "addr2", "phone2", CITY))
            .isInstanceOf(IllegalArgumentException.class);

        // La validación corre ANTES de asignar: el agregado no debe quedar a medio actualizar.
        assertThat(b.getName()).isEqualTo("Old");
        assertThat(b.getCode()).isEqualTo("OLD");
        assertThat(b.getAddress()).isEqualTo("addr");
    }

    // ---------- ciclo de estado ----------

    @Test
    void activate_y_deactivate_son_idempotentes() {
        Branch b = Branch.create("Sede", "S1", null, null, CITY, COMPANY);

        b.deactivate();
        assertThat(b.isActive()).isFalse();
        b.deactivate();
        assertThat(b.isActive()).as("deactivate repetido no debe romper el estado").isFalse();

        b.activate();
        assertThat(b.isActive()).isTrue();
        b.activate();
        assertThat(b.isActive()).as("activate repetido no debe romper el estado").isTrue();
    }

    // ---------- validaciones ----------

    @Test
    void name_es_obligatorio() {
        assertThatThrownBy(() -> Branch.create(null, "C", null, null, CITY, COMPANY))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name is required");
        assertThatThrownBy(() -> Branch.create("   ", "C", null, null, CITY, COMPANY))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name is required");
    }

    @Test
    void name_maximo_120() {
        assertThat(Branch.create("a".repeat(120), "C", null, null, CITY, COMPANY).getName()).hasSize(120);
        assertThatThrownBy(() -> Branch.create("a".repeat(121), "C", null, null, CITY, COMPANY))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("120");
    }

    @Test
    void code_es_obligatorio() {
        assertThatThrownBy(() -> Branch.create("N", null, null, null, CITY, COMPANY))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("code is required");
        assertThatThrownBy(() -> Branch.create("N", "  ", null, null, CITY, COMPANY))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("code is required");
    }

    @Test
    void code_maximo_30() {
        assertThat(Branch.create("N", "a".repeat(30), null, null, CITY, COMPANY).getCode()).hasSize(30);
        assertThatThrownBy(() -> Branch.create("N", "a".repeat(31), null, null, CITY, COMPANY))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("30");
    }

    @Test
    void address_maximo_255() {
        assertThatThrownBy(() -> Branch.create("N", "C", "a".repeat(256), null, CITY, COMPANY))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("255");
    }

    @Test
    void phone_maximo_30() {
        assertThatThrownBy(() -> Branch.create("N", "C", null, "a".repeat(31), CITY, COMPANY))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("30");
    }

    @Test
    void city_es_obligatoria() {
        assertThatThrownBy(() -> Branch.create("N", "C", null, null, null, COMPANY))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("city is required");
    }

    @Test
    void company_es_obligatoria() {
        assertThatThrownBy(() -> Branch.create("N", "C", null, null, CITY, null))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("company is required");
    }
}
