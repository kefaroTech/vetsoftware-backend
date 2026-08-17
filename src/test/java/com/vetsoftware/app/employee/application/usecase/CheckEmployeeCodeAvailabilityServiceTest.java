package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Chequeo en vivo del formulario de invitación: dice si un código de empleado
 * está libre. El código es único GLOBAL, así que "libre" se decide contra TODAS
 * las filas de {@code employees} —incluidas las de empleados desactivados por
 * soft-delete—, que es lo que hace la query nativa de {@code codeExists}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CheckEmployeeCodeAvailabilityService")
class CheckEmployeeCodeAvailabilityServiceTest {

    private static final String CODIGO = "VV-MARIANA";

    @Mock
    private EmployeeRepository repository;
    @InjectMocks
    private CheckEmployeeCodeAvailabilityService service;

    @Nested
    @DisplayName("Entradas que ni se consultan")
    class EntradasQueNiSeConsultan {

        @Test
        @DisplayName("un código nulo no está disponible y no llega al repositorio")
        void un_codigo_nulo_no_esta_disponible() {
            assertThat(service.isAvailable(null)).isFalse();

            verifyNoInteractions(repository);
        }

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("un código en blanco no está disponible y no llega al repositorio")
        void un_codigo_en_blanco_no_esta_disponible(String codigo) {
            // Sin esta guarda, " " se consultaría contra la BD y volvería "libre":
            // el formulario dejaría enviar un código que el dominio rechaza después.
            assertThat(service.isAvailable(codigo)).isFalse();

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Consulta")
    class Consulta {

        @Test
        @DisplayName("un código que nadie usa está disponible")
        void un_codigo_que_nadie_usa_esta_disponible() {
            when(repository.codeExists(CODIGO)).thenReturn(false);

            assertThat(service.isAvailable(CODIGO)).isTrue();
        }

        @Test
        @DisplayName("un código ya existente no está disponible, aunque su dueño esté desactivado")
        void un_codigo_ya_existente_no_esta_disponible() {
            // codeExists cuenta todas las filas (query nativa que salta el
            // @SQLRestriction): el unique de la columna tampoco distingue bajas, así que
            // el código de un empleado desactivado sigue reservado.
            when(repository.codeExists(CODIGO)).thenReturn(true);

            assertThat(service.isAvailable(CODIGO)).isFalse();
        }

        @Test
        @DisplayName("recorta los espacios antes de consultar, no consulta lo que tecleó el admin")
        void recorta_los_espacios_antes_de_consultar() {
            // El campo del formulario llega con el pegado del portapapeles: si el trim no
            // fuera antes de la query, " VV-MARIANA " saldría libre y el INSERT
            // reventaría contra el unique.
            when(repository.codeExists(CODIGO)).thenReturn(true);

            assertThat(service.isAvailable("  " + CODIGO + "  ")).isFalse();

            verify(repository).codeExists(CODIGO);
        }
    }
}
