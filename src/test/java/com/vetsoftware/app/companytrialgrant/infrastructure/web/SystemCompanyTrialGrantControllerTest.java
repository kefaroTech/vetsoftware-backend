package com.vetsoftware.app.companytrialgrant.infrastructure.web;

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

import com.vetsoftware.app.companytrialgrant.application.command.ConsumeTrialGrantCommand;
import com.vetsoftware.app.companytrialgrant.application.command.GrantTrialCommand;
import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import com.vetsoftware.app.companytrialgrant.application.port.in.ConsumeTrialGrantUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.in.GrantTrialUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.in.ListCompanyTrialGrantsUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.in.ListExpiredTrialGrantsUseCase;
import com.vetsoftware.app.companytrialgrant.domain.TrialAlreadyGrantedException;
import com.vetsoftware.app.companytrialgrant.domain.TrialOutcome;
import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import com.vetsoftware.app.companytrialgrant.domain.TrialWindowNotOpenException;
import com.vetsoftware.app.companytrialgrant.infrastructure.web.request.GrantTrialRequest;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
 * Rodaja HTTP de la consola de plataforma sobre las concesiones de prueba.
 *
 * <p>
 * Cuatro invariantes de frontera: la empresa entra por la ruta, el fin de la
 * prueba y la ventana no se aceptan de fuera, resolver una prueba escribe un
 * hecho en vez de borrarlo, y el barrido de vencimientos cuelga de una ruta
 * plana porque lista filas de todas las empresas.
 */
@WebMvcTest(SystemCompanyTrialGrantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemCompanyTrialGrantController — contrato HTTP")
class SystemCompanyTrialGrantControllerTest {

    private static final Long LA_CLINICA = 42L;

    private static final String CUERPO_CONCESION = """
            {"catalogItemId":5,"grantedOn":"2026-09-16","daysGranted":15,
             "policyTrialDays":30,"policyTrialOutcome":"LIMITED","sourceQuoteId":77}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GrantTrialUseCase grantUseCase;
    @MockitoBean
    private ConsumeTrialGrantUseCase consumeUseCase;
    @MockitoBean
    private ListCompanyTrialGrantsUseCase listUseCase;
    @MockitoBean
    private ListExpiredTrialGrantsUseCase listExpiredUseCase;

    private static CompanyTrialGrantDto viva() {
        return new CompanyTrialGrantDto(11L, LA_CLINICA, 5L, 3L, LocalDate.of(2026, 9, 16), 15, 15,
                LocalDate.of(2026, 9, 30), 30, TrialPolicyOutcome.LIMITED, 77L, null, null, null,
                true);
    }

    private static CompanyTrialGrantDto resuelta(TrialOutcome outcome) {
        return new CompanyTrialGrantDto(11L, LA_CLINICA, 5L, 3L, LocalDate.of(2026, 9, 16), 15, 15,
                LocalDate.of(2026, 9, 30), 30, TrialPolicyOutcome.LIMITED, 77L, null,
                LocalDateTime.of(2026, 9, 21, 8, 0), outcome, false);
    }

    @Nested
    @DisplayName("Concesion")
    class Concesion {

        @Test
        @DisplayName("POST /companies/{id} responde 201 con la concesion")
        void post_responde_201() throws Exception {
            when(grantUseCase.execute(any())).thenReturn(viva());

            mockMvc.perform(post("/system/company-trial-grants/companies/42")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CONCESION))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.companyId").value(42))
                    .andExpect(jsonPath("$.catalogItemId").value(5))
                    .andExpect(jsonPath("$.effectiveDays").value(15));
        }

        @Test
        @DisplayName("POST toma la empresa de la ruta, no del cuerpo ni del principal")
        void post_toma_la_empresa_de_la_ruta() throws Exception {
            when(grantUseCase.execute(any())).thenReturn(viva());

            mockMvc.perform(post("/system/company-trial-grants/companies/42")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CONCESION))
                    .andExpect(status().isCreated());

            ArgumentCaptor<GrantTrialCommand> command = ArgumentCaptor
                    .forClass(GrantTrialCommand.class);
            verify(grantUseCase).execute(command.capture());
            assertThat(command.getValue().companyId()).isEqualTo(LA_CLINICA);
            assertThat(command.getValue().catalogItemId()).isEqualTo(5L);
            assertThat(command.getValue().daysGranted()).isEqualTo(15);
            assertThat(command.getValue().policyTrialDays()).isEqualTo(30);
        }

        @Test
        @DisplayName("regalar dos veces el mismo articulo a la misma empresa responde 409")
        void segunda_concesion_responde_409() throws Exception {
            when(grantUseCase.execute(any()))
                    .thenThrow(new TrialAlreadyGrantedException(LA_CLINICA, 5L));

            mockMvc.perform(post("/system/company-trial-grants/companies/42")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CONCESION))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("conceder sin ventana viva responde 409")
        void sin_ventana_viva_responde_409() throws Exception {
            when(grantUseCase.execute(any())).thenThrow(
                    new TrialWindowNotOpenException(LA_CLINICA, LocalDate.of(2026, 9, 16)));

            mockMvc.perform(post("/system/company-trial-grants/companies/42")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CONCESION))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("POST sin desenlace de politica responde 400 y no concede nada")
        void sin_politica_responde_400() throws Exception {
            mockMvc.perform(post("/system/company-trial-grants/companies/42")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"catalogItemId":5,"grantedOn":"2026-09-16","daysGranted":15,
                             "policyTrialDays":30}
                            """)).andExpect(status().isBadRequest());

            verify(grantUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST con cero dias concedidos responde 400")
        void cero_dias_responde_400() throws Exception {
            mockMvc.perform(post("/system/company-trial-grants/companies/42")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"catalogItemId":5,"grantedOn":"2026-09-16","daysGranted":0,
                             "policyTrialDays":30,"policyTrialOutcome":"LIMITED"}
                            """)).andExpect(status().isBadRequest());

            verify(grantUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Resolucion")
    class Resolucion {

        @Test
        @DisplayName("POST /consumptions escribe el desenlace y devuelve la concesion resuelta")
        void post_consumptions_resuelve() throws Exception {
            when(consumeUseCase.execute(any())).thenReturn(resuelta(TrialOutcome.ABANDONED));

            mockMvc.perform(
                    post("/system/company-trial-grants/companies/42/catalog-items/5/consumptions")
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"outcome":"ABANDONED"}
                                    """))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("ABANDONED"))
                    .andExpect(jsonPath("$.live").value(false))
                    .andExpect(jsonPath("$.consumedAt").exists());
        }

        @Test
        @DisplayName("la resolucion se dirige por empresa y articulo, ambos de la ruta")
        void la_resolucion_se_dirige_por_empresa_y_articulo() throws Exception {
            when(consumeUseCase.execute(any())).thenReturn(resuelta(TrialOutcome.ABANDONED));

            mockMvc.perform(
                    post("/system/company-trial-grants/companies/42/catalog-items/5/consumptions")
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"outcome":"ABANDONED"}
                                    """))
                    .andExpect(status().isOk());

            ArgumentCaptor<ConsumeTrialGrantCommand> command = ArgumentCaptor
                    .forClass(ConsumeTrialGrantCommand.class);
            verify(consumeUseCase).execute(command.capture());
            assertThat(command.getValue().companyId()).isEqualTo(LA_CLINICA);
            assertThat(command.getValue().catalogItemId()).isEqualTo(5L);
            assertThat(command.getValue().outcome()).isEqualTo(TrialOutcome.ABANDONED);
        }

        /**
         * <strong>Un desenlace vacío es un valor con significado</strong>: «el que diga
         * su política congelada», que es el caso normal cuando la prueba vence en su
         * fecha. Por eso el cuerpo no lleva {@code @NotNull} y esto responde 200.
         */
        @Test
        @DisplayName("un cuerpo sin desenlace es valido: significa el que diga la politica")
        void cuerpo_sin_desenlace_es_valido() throws Exception {
            when(consumeUseCase.execute(any())).thenReturn(resuelta(TrialOutcome.LIMITED));

            mockMvc.perform(
                    post("/system/company-trial-grants/companies/42/catalog-items/5/consumptions")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("LIMITED"));

            ArgumentCaptor<ConsumeTrialGrantCommand> command = ArgumentCaptor
                    .forClass(ConsumeTrialGrantCommand.class);
            verify(consumeUseCase).execute(command.capture());
            assertThat(command.getValue().outcome()).isNull();
        }
    }

    @Nested
    @DisplayName("Barrido de vencimientos")
    class BarridoDeVencimientos {

        /**
         * Cuelga de una ruta plana porque lista filas de todas las empresas. Su gate
         * tiene que ser {@code hasRole('SYSTEM')} a secas: es exactamente la familia
         * que cierra {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
         */
        @Test
        @DisplayName("GET /expirations barre por el dia que llega en el parametro")
        void get_expirations_barre_por_el_dia() throws Exception {
            when(listExpiredUseCase.listLiveExpiredOn(LocalDate.of(2026, 10, 1)))
                    .thenReturn(List.of(viva()));

            mockMvc.perform(
                    get("/system/company-trial-grants/expirations").param("day", "2026-10-01"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

            verify(listExpiredUseCase).listLiveExpiredOn(LocalDate.of(2026, 10, 1));
        }

        /**
         * El día no lo deriva el servidor de su propio reloj: lo pasa quien llama, en
         * la zona horaria del negocio. Sin el parámetro, la petición no es válida.
         */
        @Test
        @DisplayName("GET /expirations sin dia responde 400: el reloj no lo pone el servidor")
        void get_expirations_sin_dia_responde_400() throws Exception {
            mockMvc.perform(get("/system/company-trial-grants/expirations"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Lectura de plataforma")
    class LecturaDePlataforma {

        @Test
        @DisplayName("GET /companies/{id} lista las concesiones de la empresa de la ruta")
        void get_lista_la_empresa_de_la_ruta() throws Exception {
            when(listUseCase.listByCompanyId(LA_CLINICA)).thenReturn(List.of(viva()));

            mockMvc.perform(get("/system/company-trial-grants/companies/42"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

            verify(listUseCase).listByCompanyId(LA_CLINICA);
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        @Test
        @DisplayName("conceder, resolver y barrer exigen SYSTEM a secas")
        void las_tres_operaciones_exigen_system() throws Exception {
            assertThat(GrantTrialUseCase.class.getMethod("execute", GrantTrialCommand.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
            assertThat(ConsumeTrialGrantUseCase.class
                    .getMethod("execute", ConsumeTrialGrantCommand.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
            assertThat(ListExpiredTrialGrantsUseCase.class
                    .getMethod("listLiveExpiredOn", LocalDate.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
        }

        @Test
        @DisplayName("el request de concesion no declara companyId, trialEndDate ni trialWindowId")
        void el_request_no_declara_lo_que_no_puede_elegir() {
            assertThat(GrantTrialRequest.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .doesNotContain("companyId", "trialEndDate", "trialWindowId");
        }
    }

    @Nested
    @DisplayName("Una concesion no se desconcede")
    class UnaConcesionNoSeDesconcede {

        @Test
        @DisplayName("el controller de plataforma no publica ningun metodo que borre")
        void el_controller_no_publica_ningun_borrado() {
            assertThat(Arrays.stream(SystemCompanyTrialGrantController.class.getDeclaredMethods())
                    .map(Method::getName))
                    .noneMatch(nombre -> nombre.startsWith("delete") || nombre.startsWith("remove")
                            || nombre.startsWith("revoke") || nombre.startsWith("disable"));
        }

        @Test
        @DisplayName("no existe ningun DELETE sobre una concesion")
        void no_existe_delete() throws Exception {
            mockMvc.perform(delete("/system/company-trial-grants/companies/42/catalog-items/5"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
