package com.vetsoftware.app.economicactivity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Entidad real, sin mocks. Las cuatro invariantes de {@code code}/{@code name}
 * estan duplicadas entre el constructor y {@code update(...)}, y cada una es
 * una condicion {@code A || B}: por eso el catalogo de casos cubre por separado
 * "es null", "esta en blanco pero no null" y "es valido", que son las tres
 * ramas reales del operador corto-circuito.
 */
@DisplayName("EconomicActivity")
class EconomicActivityTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static EconomicActivity nueva(String code, String name) {
        return new EconomicActivity(1L, code, name, CREADO, null, true);
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("crea la actividad con los campos dados")
        void crea_la_actividad_con_los_campos_dados() {
            EconomicActivity actividad = nueva("0111", "Cultivo de cereales");

            assertThat(actividad.getId()).isEqualTo(1L);
            assertThat(actividad.getCode()).isEqualTo("0111");
            assertThat(actividad.getName()).isEqualTo("Cultivo de cereales");
            assertThat(actividad.getCreatedDate()).isEqualTo(CREADO);
            assertThat(actividad.isEnabled()).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza codigo vacio o en blanco")
        void rechaza_codigo_vacio(String codigo) {
            assertThatThrownBy(() -> nueva(codigo, "Cultivo de cereales"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");
        }

        @Test
        @DisplayName("rechaza codigo de mas de 20 caracteres")
        void rechaza_codigo_demasiado_largo() {
            String codigo = "x".repeat(21);

            assertThatThrownBy(() -> nueva(codigo, "Cultivo de cereales"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code must be 20 chars or less");
        }

        @Test
        @DisplayName("acepta codigo de exactamente 20 caracteres")
        void acepta_codigo_de_veinte_caracteres() {
            String codigo = "x".repeat(20);

            assertThat(nueva(codigo, "Cultivo de cereales").getCode()).isEqualTo(codigo);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza nombre vacio o en blanco")
        void rechaza_nombre_vacio(String nombre) {
            assertThatThrownBy(() -> nueva("0111", nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("rechaza nombre de mas de 150 caracteres")
        void rechaza_nombre_demasiado_largo() {
            String nombre = "x".repeat(151);

            assertThatThrownBy(() -> nueva("0111", nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name must be 150 chars or less");
        }

        @Test
        @DisplayName("acepta nombre de exactamente 150 caracteres")
        void acepta_nombre_de_ciento_cincuenta_caracteres() {
            String nombre = "x".repeat(150);

            assertThat(nueva("0111", nombre).getName()).isEqualTo(nombre);
        }

        @Test
        @DisplayName("create genera una actividad habilitada, sin id y con fecha de creacion")
        void create_genera_una_actividad_habilitada_sin_id() {
            EconomicActivity actividad = EconomicActivity.create("0111", "Cultivo de cereales");

            assertThat(actividad.getId()).isNull();
            assertThat(actividad.getCode()).isEqualTo("0111");
            assertThat(actividad.getName()).isEqualTo("Cultivo de cereales");
            assertThat(actividad.isEnabled()).isTrue();
            assertThat(actividad.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("create rechaza codigo vacio igual que el constructor")
        void create_rechaza_codigo_vacio() {
            assertThatThrownBy(() -> EconomicActivity.create("", "Cultivo de cereales"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("update cambia codigo y nombre conservando el resto")
        void update_cambia_codigo_y_nombre() {
            EconomicActivity actividad = nueva("0111", "Cultivo de cereales");

            actividad.update("0112", "Cultivo de hortalizas");

            assertThat(actividad.getCode()).isEqualTo("0112");
            assertThat(actividad.getName()).isEqualTo("Cultivo de hortalizas");
            assertThat(actividad.getId()).isEqualTo(1L);
            assertThat(actividad.getCreatedDate()).isEqualTo(CREADO);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza actualizar a un codigo vacio o en blanco")
        void rechaza_actualizar_a_codigo_vacio(String codigo) {
            EconomicActivity actividad = nueva("0111", "Cultivo de cereales");

            assertThatThrownBy(() -> actividad.update(codigo, "Cultivo de hortalizas"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");
        }

        @Test
        @DisplayName("rechaza actualizar a un codigo de mas de 20 caracteres")
        void rechaza_actualizar_a_codigo_demasiado_largo() {
            EconomicActivity actividad = nueva("0111", "Cultivo de cereales");
            String codigo = "x".repeat(21);

            assertThatThrownBy(() -> actividad.update(codigo, "Cultivo de hortalizas"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code must be 20 chars or less");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza actualizar a un nombre vacio o en blanco")
        void rechaza_actualizar_a_nombre_vacio(String nombre) {
            EconomicActivity actividad = nueva("0111", "Cultivo de cereales");

            assertThatThrownBy(() -> actividad.update("0112", nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("rechaza actualizar a un nombre de mas de 150 caracteres")
        void rechaza_actualizar_a_nombre_demasiado_largo() {
            EconomicActivity actividad = nueva("0111", "Cultivo de cereales");
            String nombre = "x".repeat(151);

            assertThatThrownBy(() -> actividad.update("0112", nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name must be 150 chars or less");
        }

        @Test
        @DisplayName("una actualizacion invalida no deja el codigo ni el nombre a medias")
        void una_actualizacion_invalida_no_muta_el_estado() {
            EconomicActivity actividad = nueva("0111", "Cultivo de cereales");

            assertThatThrownBy(() -> actividad.update(null, "Cultivo de hortalizas"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(actividad.getCode()).isEqualTo("0111");
            assertThat(actividad.getName()).isEqualTo("Cultivo de cereales");
        }
    }

    @Nested
    @DisplayName("estado habilitado")
    class EstadoHabilitado {

        @Test
        @DisplayName("disable apaga el estado habilitado")
        void disable_apaga_el_estado() {
            EconomicActivity actividad = nueva("0111", "Cultivo de cereales");

            actividad.disable();

            assertThat(actividad.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable prende el estado habilitado")
        void enable_prende_el_estado() {
            EconomicActivity actividad = new EconomicActivity(1L, "0111", "Cultivo de cereales",
                    CREADO, null, false);

            actividad.enable();

            assertThat(actividad.isEnabled()).isTrue();
        }
    }
}
