package com.vetsoftware.app.companytrialwindow.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.companytrialwindow.application.command.OpenTrialWindowCommand;
import com.vetsoftware.app.companytrialwindow.application.dto.CompanyTrialWindowDto;
import com.vetsoftware.app.companytrialwindow.application.port.in.CloseTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.application.port.in.FindCurrentTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.application.port.in.OpenTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.domain.CompanyAlreadyHasOpenTrialWindowException;
import com.vetsoftware.app.companytrialwindow.domain.TrialWindowAlreadyClosedException;
import com.vetsoftware.app.companytrialwindow.infrastructure.web.request.OpenTrialWindowRequest;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de la consola de plataforma sobre el reloj de prueba.
 *
 * <p>
 * Tres invariantes de frontera se clavan aquí: la empresa entra por la ruta y
 * nunca por el cuerpo, el fin de la ventana no se acepta de fuera, y cerrar es
 * un {@code POST} sobre un sub-recurso porque escribe un hecho nuevo en vez de
 * borrar uno viejo.
 */
@WebMvcTest(SystemCompanyTrialWindowController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemCompanyTrialWindowController — contrato HTTP")
class SystemCompanyTrialWindowControllerTest {

    private static final Long LA_CLINICA = 42L;

    private static final String CUERPO_APERTURA = """
            {"startDate":"2026-09-01","windowDays":30,"sourceQuoteId":77}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenTrialWindowUseCase openUseCase;
    @MockitoBean
    private CloseTrialWindowUseCase closeUseCase;
    @MockitoBean
    private FindCurrentTrialWindowUseCase findUseCase;

    private static CompanyTrialWindowDto abierta() {
        return new CompanyTrialWindowDto(3L, LA_CLINICA, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30), 30, 77L, null, true);
    }

    private static CompanyTrialWindowDto cerrada() {
        return new CompanyTrialWindowDto(3L, LA_CLINICA, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30), 30, 77L, LocalDateTime.of(2026, 9, 12, 10, 30), false);
    }

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("POST /companies/{id} responde 201 con la ventana abierta")
        void post_responde_201() throws Exception {
            when(openUseCase.execute(any())).thenReturn(abierta());

            mockMvc.perform(post("/system/company-trial-windows/companies/42")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_APERTURA))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.companyId").value(42))
                    .andExpect(jsonPath("$.endDate").value("2026-09-30"))
                    .andExpect(jsonPath("$.open").value(true));
        }

        /**
         * La empresa sale de la ruta y de ningún otro sitio. Un usuario de plataforma
         * no tiene empresa, así que {@code authz.currentCompanyId()} no es una
         * alternativa: lanzaría.
         */
        @Test
        @DisplayName("POST toma la empresa de la ruta, no del cuerpo ni del principal")
        void post_toma_la_empresa_de_la_ruta() throws Exception {
            when(openUseCase.execute(any())).thenReturn(abierta());

            mockMvc.perform(post("/system/company-trial-windows/companies/42")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_APERTURA))
                    .andExpect(status().isCreated());

            ArgumentCaptor<OpenTrialWindowCommand> command = ArgumentCaptor
                    .forClass(OpenTrialWindowCommand.class);
            verify(openUseCase).execute(command.capture());
            assertThat(command.getValue().companyId()).isEqualTo(LA_CLINICA);
            assertThat(command.getValue().windowDays()).isEqualTo(30);
            assertThat(command.getValue().sourceQuoteId()).isEqualTo(77L);
        }

        @Test
        @DisplayName("una segunda ventana con la primera viva responde 409")
        void segunda_ventana_responde_409() throws Exception {
            when(openUseCase.execute(any()))
                    .thenThrow(new CompanyAlreadyHasOpenTrialWindowException(LA_CLINICA));

            mockMvc.perform(post("/system/company-trial-windows/companies/42")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_APERTURA))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("POST con cero dias responde 400 y no abre nada")
        void cero_dias_responde_400() throws Exception {
            mockMvc.perform(post("/system/company-trial-windows/companies/42")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"startDate":"2026-09-01","windowDays":0,"sourceQuoteId":77}
                            """)).andExpect(status().isBadRequest());

            verify(openUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST sin cotizacion de origen responde 400: la prueba nace de una venta")
        void sin_cotizacion_responde_400() throws Exception {
            mockMvc.perform(post("/system/company-trial-windows/companies/42")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"startDate":"2026-09-01","windowDays":30}
                            """)).andExpect(status().isBadRequest());

            verify(openUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Cierre")
    class Cierre {

        @Test
        @DisplayName("POST /closures devuelve la ventana con su fecha de cierre escrita")
        void post_closures_cierra() throws Exception {
            when(closeUseCase.execute(LA_CLINICA)).thenReturn(cerrada());

            mockMvc.perform(post("/system/company-trial-windows/companies/42/closures"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.open").value(false))
                    .andExpect(jsonPath("$.closedAt").exists());
        }

        /**
         * <strong>Cerrar no acorta.</strong> Ni {@code startDate}, ni
         * {@code windowDays}, ni {@code endDate} se mueven: eso es lo que hace que
         * reponer un módulo después no invente fecha nueva, y lo que la clave foránea
         * de las concesiones impone además desde el motor.
         */
        @Test
        @DisplayName("cerrar no toca el principio, la duracion ni el fin de la ventana")
        void cerrar_no_acorta() throws Exception {
            when(closeUseCase.execute(LA_CLINICA)).thenReturn(cerrada());

            mockMvc.perform(post("/system/company-trial-windows/companies/42/closures"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.startDate").value("2026-09-01"))
                    .andExpect(jsonPath("$.endDate").value("2026-09-30"))
                    .andExpect(jsonPath("$.windowDays").value(30));
        }

        @Test
        @DisplayName("cerrar dos veces responde 409")
        void cerrar_dos_veces_responde_409() throws Exception {
            when(closeUseCase.execute(LA_CLINICA)).thenThrow(new TrialWindowAlreadyClosedException(
                    LA_CLINICA, LocalDateTime.of(2026, 9, 12, 10, 30)));

            mockMvc.perform(post("/system/company-trial-windows/companies/42/closures"))
                    .andExpect(status().isConflict());
        }

        /**
         * <strong>No hay {@code DELETE} en esta rodaja y no puede haberlo.</strong> El
         * fin de la ventana está copiado dentro de cada concesión y atado por clave
         * foránea: borrarla no es una operación que el modelo admita, y el verbo que la
         * insinúa no debe existir en la superficie.
         */
        @Test
        @DisplayName("no existe ningun DELETE sobre la ventana")
        void no_existe_delete() throws Exception {
            mockMvc.perform(delete("/system/company-trial-windows/companies/42"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("Lectura de plataforma")
    class LecturaDePlataforma {

        @Test
        @DisplayName("GET /companies/{id}/current lee la ventana de la empresa de la ruta")
        void get_lee_la_empresa_de_la_ruta() throws Exception {
            when(findUseCase.findOpenByCompanyId(LA_CLINICA)).thenReturn(abierta());

            mockMvc.perform(get("/system/company-trial-windows/companies/42/current"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.companyId").value(42));

            verify(findUseCase).findOpenByCompanyId(LA_CLINICA);
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        /**
         * Conceder días de prueba es una decisión comercial de plataforma. Si el gate
         * admitiera al empleado de la clínica, la administradora se abriría ventanas y
         * el abuso que toda la capa existe para cerrar entraría por la puerta
         * principal.
         */
        @Test
        @DisplayName("abrir y cerrar exigen SYSTEM a secas")
        void abrir_y_cerrar_exigen_system() throws Exception {
            assertThat(
                    OpenTrialWindowUseCase.class.getMethod("execute", OpenTrialWindowCommand.class)
                            .getAnnotation(PreAuthorize.class).value())
                    .isEqualTo("hasRole('SYSTEM')");
            assertThat(CloseTrialWindowUseCase.class.getMethod("execute", Long.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
        }

        @Test
        @DisplayName("el request de apertura no declara companyId ni endDate")
        void el_request_no_declara_company_id_ni_end_date() {
            assertThat(OpenTrialWindowRequest.class.getRecordComponents())
                    .extracting(RecordComponent::getName).doesNotContain("companyId", "endDate");
        }

        /**
         * <strong>Ninguna operación de la superficie estira la ventana.</strong>
         * R-TRIAL-10 lo dice del slice entero y {@code TrialWindowWriteSurfaceTest} lo
         * comprueba en los puertos; esto lo cierra en el controlador, que es lo único
         * que un cliente puede alcanzar.
         */
        @Test
        @DisplayName("el controller no publica ningun metodo que amplie o reabra la ventana")
        void el_controller_no_publica_ninguna_ampliacion() {
            assertThat(Arrays.stream(SystemCompanyTrialWindowController.class.getDeclaredMethods())
                    .map(Method::getName))
                    .noneMatch(nombre -> nombre.startsWith("extend") || nombre.startsWith("prolong")
                            || nombre.startsWith("reopen") || nombre.startsWith("delete"));
        }
    }
}
