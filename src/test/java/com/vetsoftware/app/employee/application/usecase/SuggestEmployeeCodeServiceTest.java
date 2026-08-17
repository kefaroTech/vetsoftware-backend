package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.testsupport.EmployeeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Sugerencia de código que ve el admin al invitar staff. Es el único caller de
 * producción de {@code EmployeeCodeGenerator.generateAvailable}, así que aquí
 * se fija el contrato del predicado de disponibilidad: se consulta
 * {@code repository.codeExists}, que cuenta TODAS las filas —incluidas las de
 * empleados desactivados por soft-delete— porque el unique de
 * {@code employee_code} tampoco distingue.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SuggestEmployeeCodeService")
class SuggestEmployeeCodeServiceTest {

    private static final String SUGERENCIA = "VV-MARIANAROJ";

    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private EmployeeRepository repository;
    @InjectMocks
    private SuggestEmployeeCodeService service;

    @Nested
    @DisplayName("Sugerencia")
    class Sugerencia {

        @Test
        @DisplayName("compone el código con las iniciales de la empresa y el nombre del empleado")
        void compone_el_codigo_con_las_iniciales_de_la_empresa() {
            when(companyQueryPort.findById(EmployeeMother.COMPANY_ID))
                    .thenReturn(Optional.of(EmployeeMother.VETRINA));
            when(repository.codeExists(SUGERENCIA)).thenReturn(false);

            assertThat(service.suggest(EmployeeMother.COMPANY_ID, "Mariana Rojas"))
                    .isEqualTo(SUGERENCIA);
        }

        @Test
        @DisplayName("desempata con sufijo cuando el código propuesto ya está en uso")
        void desempata_con_sufijo_cuando_el_codigo_ya_existe() {
            when(companyQueryPort.findById(EmployeeMother.COMPANY_ID))
                    .thenReturn(Optional.of(EmployeeMother.VETRINA));
            when(repository.codeExists(SUGERENCIA)).thenReturn(true);
            when(repository.codeExists(SUGERENCIA + "-2")).thenReturn(false);

            assertThat(service.suggest(EmployeeMother.COMPANY_ID, "Mariana Rojas"))
                    .isEqualTo(SUGERENCIA + "-2");
        }
    }

    @Nested
    @DisplayName("Entradas degeneradas")
    class EntradasDegeneradas {

        @Test
        @DisplayName("una empresa inexistente sugiere igual, con el prefijo vacío")
        void una_empresa_inexistente_sugiere_con_prefijo_vacio() {
            // El .orElse("") no aborta: el chequeo de tenant vive en el @PreAuthorize del
            // puerto, aquí solo faltan las iniciales.
            when(companyQueryPort.findById(EmployeeMother.COMPANY_ID)).thenReturn(Optional.empty());
            when(repository.codeExists("-MARIANAROJ")).thenReturn(false);

            assertThat(service.suggest(EmployeeMother.COMPANY_ID, "Mariana Rojas"))
                    .isEqualTo("-MARIANAROJ");
        }

        @Test
        @DisplayName("un nombre nulo se trata como vacío y deja solo el prefijo")
        void un_nombre_nulo_se_trata_como_vacio() {
            when(companyQueryPort.findById(EmployeeMother.COMPANY_ID))
                    .thenReturn(Optional.of(EmployeeMother.VETRINA));
            when(repository.codeExists("VV-")).thenReturn(false);

            assertThat(service.suggest(EmployeeMother.COMPANY_ID, null)).isEqualTo("VV-");
        }

        @Test
        @DisplayName("sin empresa y sin nombre la sugerencia se reduce al separador")
        void sin_empresa_ni_nombre_queda_solo_el_separador() {
            when(companyQueryPort.findById(EmployeeMother.COMPANY_ID)).thenReturn(Optional.empty());
            when(repository.codeExists("-")).thenReturn(false);

            assertThat(service.suggest(EmployeeMother.COMPANY_ID, null)).isEqualTo("-");
        }
    }

    @Nested
    @DisplayName("Contrato del predicado de disponibilidad")
    class ContratoDelPredicado {

        @Test
        @DisplayName("un empleado desactivado sigue reservando su código")
        void un_empleado_desactivado_sigue_reservando_su_codigo() {
            // codeExists es una query nativa que salta el @SQLRestriction a propósito
            // (JpaEmployeeRepository:97-99): reciclar el código de alguien dado de baja
            // chocaría contra el unique de la BD en mitad del alta.
            when(companyQueryPort.findById(EmployeeMother.COMPANY_ID))
                    .thenReturn(Optional.of(EmployeeMother.VETRINA));
            when(repository.codeExists(SUGERENCIA)).thenReturn(true);
            when(repository.codeExists(SUGERENCIA + "-2")).thenReturn(false);

            String sugerido = service.suggest(EmployeeMother.COMPANY_ID, "Mariana Rojas");

            assertThat(sugerido).isNotEqualTo(SUGERENCIA).isEqualTo(SUGERENCIA + "-2");
        }

        @Test
        @DisplayName("el código consultado es exactamente el que se devuelve, ya recortado a 50")
        void el_codigo_consultado_es_exactamente_el_que_se_devuelve() {
            // La regresión del arreglo: con la base libre y razón social larga se
            // devolvía una cadena de 56 caracteres que jamás se había consultado, y el
            // constructor de Employee la rechazaba al aceptar la invitación.
            when(companyQueryPort.findById(EmployeeMother.COMPANY_ID))
                    .thenReturn(Optional.of(EmployeeMother.VETRINA_RAZON_SOCIAL_LARGA));
            when(repository.codeExists(anyString())).thenReturn(false);

            String sugerido = service.suggest(EmployeeMother.COMPANY_ID, "Maximiliano");

            ArgumentCaptor<String> consultado = ArgumentCaptor.forClass(String.class);
            verify(repository).codeExists(consultado.capture());
            assertThat(sugerido).hasSize(50).isEqualTo(consultado.getValue());
        }

        @Test
        @DisplayName("no toca el repositorio hasta tener resuelta la empresa")
        void resuelve_la_empresa_antes_de_consultar_el_repositorio() {
            when(companyQueryPort.findById(EmployeeMother.COMPANY_ID))
                    .thenThrow(new IllegalStateException("boom"));

            assertThatThrownBy(() -> service.suggest(EmployeeMother.COMPANY_ID, "Mariana Rojas"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("boom");

            verifyNoInteractions(repository);
        }
    }
}
