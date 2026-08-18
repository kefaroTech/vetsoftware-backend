package com.vetsoftware.app.medicament.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Medicament")
class MedicamentTest {

    private static final CompanyRef COMPANY = new CompanyRef(9L, "Clinica Norte", "900123456");

    @Nested
    @DisplayName("create")
    class Creacion {

        @Test
        @DisplayName("crea un medicamento general sin empresa, habilitado")
        void crea_un_medicamento_general_sin_empresa() {
            Medicament medicamento = Medicament.create("Amoxicilina", "Antibiotico", null, true);

            assertThat(medicamento.getName()).isEqualTo("Amoxicilina");
            assertThat(medicamento.getDescription()).isEqualTo("Antibiotico");
            assertThat(medicamento.getCompany()).isNull();
            assertThat(medicamento.isGeneral()).isTrue();
            assertThat(medicamento.isEnabled()).isTrue();
            assertThat(medicamento.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("crea un medicamento propio de una empresa")
        void crea_un_medicamento_propio_de_empresa() {
            Medicament medicamento = Medicament.create("Suero", "Formula propia", COMPANY, false);

            assertThat(medicamento.getCompany()).isEqualTo(COMPANY);
            assertThat(medicamento.isGeneral()).isFalse();
        }

        @Test
        @DisplayName("acepta description nula")
        void acepta_description_nula() {
            Medicament medicamento = Medicament.create("Suero", null, COMPANY, false);

            assertThat(medicamento.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("invariante general/company — mutuamente excluyentes")
    class InvarianteGeneralCompany {

        @Test
        @DisplayName("general=true con company no nula es invalido")
        void general_true_con_company_es_invalido() {
            assertThatThrownBy(() -> Medicament.create("Amoxicilina", null, COMPANY, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("general medicament cannot have company");
        }

        @Test
        @DisplayName("general=false sin company es invalido")
        void general_false_sin_company_es_invalido() {
            assertThatThrownBy(() -> Medicament.create("Suero", null, null, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-general medicament requires company");
        }

        @Test
        @DisplayName("general=true sin company es valido")
        void general_true_sin_company_es_valido() {
            assertThat(Medicament.create("Amoxicilina", null, null, true).isGeneral()).isTrue();
        }

        @Test
        @DisplayName("general=false con company es valido")
        void general_false_con_company_es_valido() {
            assertThat(Medicament.create("Suero", null, COMPANY, false).getCompany())
                    .isEqualTo(COMPANY);
        }
    }

    @Nested
    @DisplayName("validaciones de name/description")
    class ValidacionesDeCampos {

        @ParameterizedTest
        @CsvSource({"'', is blank", "'   ', is blank"})
        @DisplayName("rechaza name nulo o en blanco")
        void rechaza_name_nulo_o_blanco(String name, String ignored) {
            assertThatThrownBy(() -> Medicament.create(name, null, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("rechaza name nulo")
        void rechaza_name_nulo() {
            assertThatThrownBy(() -> Medicament.create(null, null, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("rechaza name de mas de 200 caracteres")
        void rechaza_name_demasiado_largo() {
            String largo = "a".repeat(201);

            assertThatThrownBy(() -> Medicament.create(largo, null, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name must be 200 chars or less");
        }

        @Test
        @DisplayName("acepta name de exactamente 200 caracteres")
        void acepta_name_de_200_caracteres() {
            String limite = "a".repeat(200);

            assertThat(Medicament.create(limite, null, null, true).getName()).hasSize(200);
        }

        @Test
        @DisplayName("rechaza description de mas de 500 caracteres")
        void rechaza_description_demasiado_larga() {
            String largo = "a".repeat(501);

            assertThatThrownBy(() -> Medicament.create("Amoxicilina", largo, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description must be 500 chars or less");
        }

        @Test
        @DisplayName("acepta description de exactamente 500 caracteres")
        void acepta_description_de_500_caracteres() {
            String limite = "a".repeat(500);

            assertThat(Medicament.create("Amoxicilina", limite, null, true).getDescription())
                    .hasSize(500);
        }
    }

    @Nested
    @DisplayName("update")
    class Actualizacion {

        @Test
        @DisplayName("actualiza name, description, company y general")
        void actualiza_los_campos() {
            Medicament medicamento = Medicament.create("Amoxicilina", null, null, true);

            medicamento.update("Suero", "Formula propia", COMPANY, false);

            assertThat(medicamento.getName()).isEqualTo("Suero");
            assertThat(medicamento.getDescription()).isEqualTo("Formula propia");
            assertThat(medicamento.getCompany()).isEqualTo(COMPANY);
            assertThat(medicamento.isGeneral()).isFalse();
        }

        @Test
        @DisplayName("update tambien exige la invariante general/company")
        void update_exige_la_invariante() {
            Medicament medicamento = Medicament.create("Amoxicilina", null, null, true);

            assertThatThrownBy(() -> medicamento.update("Amoxicilina", null, COMPANY, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("general medicament cannot have company");
        }
    }

    @Nested
    @DisplayName("enable / disable")
    class HabilitarDeshabilitar {

        @Test
        @DisplayName("disable apaga enabled y enable lo vuelve a encender")
        void disable_y_enable_alternan_el_estado() {
            Medicament medicamento = Medicament.create("Amoxicilina", null, null, true);

            medicamento.disable();
            assertThat(medicamento.isEnabled()).isFalse();

            medicamento.enable();
            assertThat(medicamento.isEnabled()).isTrue();
        }
    }

    @Test
    @DisplayName("el constructor completo respeta el id y la fecha de creacion dados")
    void el_constructor_completo_respeta_id_y_fecha() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 1, 0, 0);

        Medicament medicamento = new Medicament(42L, "Amoxicilina", null, null, true, creado,
                false);

        assertThat(medicamento.getId()).isEqualTo(42L);
        assertThat(medicamento.getCreatedDate()).isEqualTo(creado);
        assertThat(medicamento.isEnabled()).isFalse();
    }
}
