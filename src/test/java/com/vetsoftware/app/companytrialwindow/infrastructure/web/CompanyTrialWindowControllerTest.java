package com.vetsoftware.app.companytrialwindow.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companytrialwindow.application.dto.CompanyTrialWindowDto;
import com.vetsoftware.app.companytrialwindow.application.port.in.FindCurrentTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.domain.CompanyTrialWindowNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Rodaja HTTP de lo que la clínica ve de su propio reloj de prueba.
 *
 * <p>
 * <strong>La mitad de tenancy que sí se puede probar aquí es la de la
 * superficie</strong>, no la del gate —que en un {@code @WebMvcTest} no se
 * ejecuta—: la empresa no es un dato de entrada por ninguna vía, así que un
 * empleado de otra empresa no tiene <em>dónde</em> escribir un
 * {@code companyId} ajeno. Se comprueba mandando la petición y afirmando que al
 * caso de uso llega la empresa del principal y solo esa.
 */
@WebMvcTest(CompanyTrialWindowController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CompanyTrialWindowController — contrato HTTP")
class CompanyTrialWindowControllerTest {

    /** La empresa del principal. Distinta de la vecina de abajo, a propósito. */
    private static final Long MI_EMPRESA = 9L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindCurrentTrialWindowUseCase findUseCase;
    @MockitoBean
    private Authz authz;

    /** Del 1 al 30 de septiembre: treinta días con el último incluido. */
    private static CompanyTrialWindowDto ventanaDe(Long companyId) {
        return new CompanyTrialWindowDto(3L, companyId, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30), 30, 77L, null, true);
    }

    @Nested
    @DisplayName("Lectura del tenant")
    class LecturaDelTenant {

        @Test
        @DisplayName("GET /current devuelve la ventana viva de la clinica")
        void get_devuelve_la_ventana_viva() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(findUseCase.findOpenByCompanyId(MI_EMPRESA)).thenReturn(ventanaDe(MI_EMPRESA));

            mockMvc.perform(get("/company-trial-windows/current")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.companyId").value(9))
                    .andExpect(jsonPath("$.windowDays").value(30))
                    .andExpect(jsonPath("$.open").value(true))
                    .andExpect(jsonPath("$.sourceQuoteId").value(77));
        }

        /**
         * <strong>El fin viaja calculado y el test lo clava.</strong> Treinta días
         * desde el 1 de septiembre terminan el 30, no el 1 de octubre: es el desfase de
         * un día en el que ya cayó el documento de diseño, y con la clave foránea
         * triple que cuelga de las concesiones deja de ser un informe raro para
         * convertirse en un error del motor a mitad de un alta comercial.
         */
        @Test
        @DisplayName("el ultimo dia es inclusivo: 30 dias desde el 1 de septiembre acaban el 30")
        void el_ultimo_dia_es_inclusivo() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(findUseCase.findOpenByCompanyId(MI_EMPRESA)).thenReturn(ventanaDe(MI_EMPRESA));

            mockMvc.perform(get("/company-trial-windows/current")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.startDate").value("2026-09-01"))
                    .andExpect(jsonPath("$.endDate").value("2026-09-30"));
        }

        /**
         * Una clínica sin ventana abierta es el caso normal —la mayoría de los clientes
         * ya gastaron la suya— y no un fallo del servidor. El 404 lo produce el
         * {@code GlobalExceptionHandler} real que importa el andamiaje; sin él esto
         * sería un 500 y el test no comprobaría nada del contrato.
         */
        @Test
        @DisplayName("una clinica sin ventana abierta recibe 404, no 500")
        void sin_ventana_abierta_responde_404() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(findUseCase.findOpenByCompanyId(MI_EMPRESA))
                    .thenThrow(new CompanyTrialWindowNotFoundException(MI_EMPRESA));

            mockMvc.perform(get("/company-trial-windows/current")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la empresa la pone el servidor: un parametro companyId en la URL se ignora")
        void la_empresa_la_pone_el_servidor() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(findUseCase.findOpenByCompanyId(MI_EMPRESA)).thenReturn(ventanaDe(MI_EMPRESA));

            mockMvc.perform(get("/company-trial-windows/current").param("companyId", "77"))
                    .andExpect(status().isOk());

            verify(findUseCase).findOpenByCompanyId(MI_EMPRESA);
        }

        /**
         * El {@code verifyNoInteractions} es la mitad del valor: un 403 después de
         * haber leído los datos no es un gate, es un mensaje de error.
         */
        @Test
        @DisplayName("un principal sin empresa responde 403 y no llega al caso de uso")
        void principal_sin_empresa_responde_403() throws Exception {
            when(authz.currentCompanyId())
                    .thenThrow(new AccessDeniedException("No employee context"));

            mockMvc.perform(get("/company-trial-windows/current"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(findUseCase);
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        /**
         * El bloque <em>Prueba gratuita</em> es mixto —escribe plataforma, leen ambos—,
         * así que este puerto es el único de la feature que un tenant puede alcanzar y
         * su gate tiene que llevar la revalidación del tenant además de la puerta de
         * plataforma. Si alguien dejara solo {@code hasRole('SYSTEM')}, la clínica
         * perdería la visibilidad de su propia prueba.
         */
        @Test
        @DisplayName("el puerto admite SYSTEM o la propia empresa, y revalida el tenant")
        void el_puerto_admite_system_o_la_propia_empresa() throws Exception {
            String gate = FindCurrentTrialWindowUseCase.class
                    .getMethod("findOpenByCompanyId", Long.class).getAnnotation(PreAuthorize.class)
                    .value();

            assertThat(gate).contains("hasRole('SYSTEM')")
                    .contains("@authz.isMyCompany(#companyId)");
        }

        /**
         * El SpEL de arriba nombra {@code #companyId}, y un {@code #} que no case con
         * el nombre real del parámetro resuelve a {@code null} en silencio:
         * {@code isMyCompany(null)} da {@code false} y la regla falla siempre.
         */
        @Test
        @DisplayName("el parametro del puerto se llama companyId, que es lo que nombra el SpEL")
        void el_parametro_se_llama_company_id() throws Exception {
            assertThat(FindCurrentTrialWindowUseCase.class
                    .getMethod("findOpenByCompanyId", Long.class).getParameters()[0].getName())
                    .isEqualTo("companyId");
        }

        @Test
        @DisplayName("la ruta del controller del tenant no lleva companyId")
        void la_ruta_no_lleva_company_id() {
            assertThat(
                    CompanyTrialWindowController.class.getAnnotation(RequestMapping.class).value())
                    .containsExactly("/company-trial-windows");
        }
    }
}
