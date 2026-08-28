package com.vetsoftware.app.companylimitoverride.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.companylimitoverride.application.command.GrantCompanyLimitOverrideCommand;
import com.vetsoftware.app.companylimitoverride.application.command.RevokeCompanyLimitOverrideCommand;
import com.vetsoftware.app.companylimitoverride.application.dto.CompanyLimitOverrideDto;
import com.vetsoftware.app.companylimitoverride.application.dto.EffectiveLimitDto;
import com.vetsoftware.app.companylimitoverride.application.port.in.GrantCompanyLimitOverrideUseCase;
import com.vetsoftware.app.companylimitoverride.application.port.in.ListCompanyLimitOverridesUseCase;
import com.vetsoftware.app.companylimitoverride.application.port.in.ResolveEffectiveLimitUseCase;
import com.vetsoftware.app.companylimitoverride.application.port.in.RevokeCompanyLimitOverrideUseCase;
import com.vetsoftware.app.companylimitoverride.domain.CompanyAlreadyHasLimitOverrideException;
import com.vetsoftware.app.companylimitoverride.domain.CompanyLimitOverrideNotFoundException;
import com.vetsoftware.app.companylimitoverride.domain.LimitSource;
import com.vetsoftware.app.companylimitoverride.domain.OverrideAlreadyRevokedException;
import com.vetsoftware.app.companylimitoverride.domain.OverrideReasonCode;
import com.vetsoftware.app.companylimitoverride.infrastructure.web.request.GrantCompanyLimitOverrideRequest;
import com.vetsoftware.app.companylimitoverride.infrastructure.web.request.RevokeCompanyLimitOverrideRequest;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * Rodaja HTTP de la consola de plataforma sobre las excepciones de techo.
 *
 * <p>
 * Tres invariantes de frontera se clavan aquí: la empresa entra por la ruta, la
 * <strong>firma la pone el servidor</strong> con
 * {@code authz.currentSystemUserId()}, y revocar es un {@code POST} sobre un
 * sub-recurso porque escribe un hecho nuevo en vez de borrar uno viejo.
 *
 * <p>
 * El {@code Authz} del andamiaje se reutiliza tal cual: devuelve
 * {@link WebMvcSliceConfig#SYSTEM_USER_ID}, que es distinto de
 * {@code EMPLOYEE_ID} justo para que la aserción de la firma pueda decir cuál
 * de los dos actores firmó. Con un mock sin stub, Mockito devolvería {@code 0L}
 * para un {@code Long} —no {@code null}— y el test pasaría en verde con una
 * excepción concedida por un usuario de sistema inexistente.
 */
@WebMvcTest(SystemCompanyLimitOverrideController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemCompanyLimitOverrideController — contrato HTTP")
class SystemCompanyLimitOverrideControllerTest {

    private static final String CUERPO_CONCESION = """
            {"limitDimensionId":4,"limitQuantity":250,"validFrom":"2026-03-01",
             "reasonCode":"RETENTION","reason":"Retencion pactada con la cuenta"}
            """;

    private static final String CUERPO_REVOCACION = """
            {"revokedReasonCode":"COMMERCIAL_AGREEMENT",
             "revokedReason":"Sustituida por el acuerdo nuevo"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResolveEffectiveLimitUseCase resolveUseCase;
    @MockitoBean
    private GrantCompanyLimitOverrideUseCase grantUseCase;
    @MockitoBean
    private RevokeCompanyLimitOverrideUseCase revokeUseCase;
    @MockitoBean
    private ListCompanyLimitOverridesUseCase listUseCase;

    private static CompanyLimitOverrideDto concedida() {
        return new CompanyLimitOverrideDto(31L, 77L, 4L, 250, LocalDate.of(2026, 3, 1), null,
                OverrideReasonCode.RETENTION, "Retencion pactada con la cuenta",
                WebMvcSliceConfig.SYSTEM_USER_ID, null, null, null, null, true);
    }

    private static CompanyLimitOverrideDto revocada() {
        return new CompanyLimitOverrideDto(31L, 77L, 4L, 250, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 8, 27), OverrideReasonCode.RETENTION,
                "Retencion pactada con la cuenta", WebMvcSliceConfig.SYSTEM_USER_ID,
                WebMvcSliceConfig.SYSTEM_USER_ID, LocalDateTime.of(2026, 8, 27, 12, 0),
                OverrideReasonCode.COMMERCIAL_AGREEMENT, "Sustituida por el acuerdo nuevo", false);
    }

    @Nested
    @DisplayName("Concesion")
    class Concesion {

        @Test
        @DisplayName("POST /companies/{id} responde 201 con la excepcion concedida")
        void post_responde_201() throws Exception {
            when(grantUseCase.execute(any())).thenReturn(concedida());

            mockMvc.perform(post("/system/company-limit-overrides/companies/77")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CONCESION))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(31))
                    .andExpect(jsonPath("$.companyId").value(77))
                    .andExpect(jsonPath("$.limitQuantity").value(250))
                    .andExpect(jsonPath("$.validFrom").value("2026-03-01"))
                    .andExpect(jsonPath("$.reasonCode").value("RETENTION"))
                    .andExpect(jsonPath("$.alive").value(true));
        }

        /**
         * La empresa por la ruta y la firma desde el principal. Que el cuerpo pudiera
         * declarar al firmante sería peor que no firmar: el informe de excepciones
         * seguiría enseñando un nombre, y sería el que escribió el llamador.
         */
        @Test
        @DisplayName("POST toma la empresa de la ruta y la firma del principal, no del cuerpo")
        void post_toma_la_empresa_de_la_ruta_y_la_firma_del_principal() throws Exception {
            when(grantUseCase.execute(any())).thenReturn(concedida());

            mockMvc.perform(post("/system/company-limit-overrides/companies/77")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CONCESION));

            ArgumentCaptor<GrantCompanyLimitOverrideCommand> command = ArgumentCaptor
                    .forClass(GrantCompanyLimitOverrideCommand.class);
            verify(grantUseCase).execute(command.capture());
            assertThat(command.getValue().companyId()).isEqualTo(77L);
            assertThat(command.getValue().limitDimensionId()).isEqualTo(4L);
            assertThat(command.getValue().limitQuantity()).isEqualTo(250);
            assertThat(command.getValue().reasonCode()).isEqualTo(OverrideReasonCode.RETENTION);
            assertThat(command.getValue().grantedBySystemUserId())
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID);
        }

        @Test
        @DisplayName("una segunda excepcion viva sobre el mismo eje responde 409")
        void segunda_excepcion_viva_responde_409() throws Exception {
            when(grantUseCase.execute(any()))
                    .thenThrow(new CompanyAlreadyHasLimitOverrideException(77L, 4L));

            mockMvc.perform(post("/system/company-limit-overrides/companies/77")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CONCESION))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        /**
         * El motivo es obligatorio y no tiene valor por defecto. Sin {@code @Valid} en
         * el {@code @RequestBody} (#135) esta restricción se leería perfecta en el diff
         * y no se evaluaría nunca: la excepción entraría sin explicación y el fallo
         * saldría del dominio con otra forma y otro código.
         */
        @Test
        @DisplayName("POST sin motivo responde 400 y no concede nada")
        void post_sin_motivo_responde_400() throws Exception {
            mockMvc.perform(post("/system/company-limit-overrides/companies/77")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"limitDimensionId":4,"limitQuantity":250,"validFrom":"2026-03-01",
                             "reasonCode":"RETENTION"}
                            """)).andExpect(status().isBadRequest());

            verify(grantUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST con un motivo de mas de 255 caracteres responde 400")
        void post_con_motivo_largo_responde_400() throws Exception {
            mockMvc.perform(post("/system/company-limit-overrides/companies/77")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"limitDimensionId\":4,\"limitQuantity\":250,"
                            + "\"validFrom\":\"2026-03-01\",\"reasonCode\":\"RETENTION\","
                            + "\"reason\":\"" + "X".repeat(256) + "\"}"))
                    .andExpect(status().isBadRequest());

            verify(grantUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST con un techo negativo responde 400")
        void post_con_techo_negativo_responde_400() throws Exception {
            mockMvc.perform(post("/system/company-limit-overrides/companies/77")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"limitDimensionId":4,"limitQuantity":-5,"validFrom":"2026-03-01",
                             "reasonCode":"RETENTION","reason":"Lo que sea"}
                            """)).andExpect(status().isBadRequest());

            verify(grantUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("la revocacion sin motivo responde 400 y no revoca nada")
        void revocacion_sin_motivo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/system/company-limit-overrides/companies/77/dimensions/4/revocations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"revokedReasonCode\":\"OTHER\"}"))
                    .andExpect(status().isBadRequest());

            verify(revokeUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Revocacion")
    class Revocacion {

        /**
         * Revocar no borra: escribe quién la quitó, cuándo y por qué, y le pone fecha
         * de fin. Por eso la respuesta trae la fila entera con su desenlace y no un 204
         * vacío — y por eso «¿qué techo tenía el 14 de marzo?» sigue teniendo
         * respuesta.
         */
        @Test
        @DisplayName("POST /revocations devuelve la excepcion cerrada, con su desenlace escrito")
        void post_revocations_devuelve_la_excepcion_cerrada() throws Exception {
            when(revokeUseCase.execute(any())).thenReturn(revocada());

            mockMvc.perform(
                    post("/system/company-limit-overrides/companies/77/dimensions/4/revocations")
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_REVOCACION))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.alive").value(false))
                    .andExpect(jsonPath("$.validTo").value("2026-08-27"))
                    .andExpect(jsonPath("$.revokedAt").value("2026-08-27T12:00:00"))
                    .andExpect(jsonPath("$.revokedReasonCode").value("COMMERCIAL_AGREEMENT"))
                    .andExpect(jsonPath("$.revokedBySystemUserId").value(6));
        }

        /**
         * La revocación se dirige por empresa y eje, no por el id de la fila, porque el
         * puerto de salida no ofrece ninguna carga «por id» suelta a propósito: una
         * excepción es de alguien, y cargarla por un id que escribe el cliente es la
         * familia de fugas que cerró BE-COV.
         */
        @Test
        @DisplayName("la revocacion se dirige por empresa y eje, ambos de la ruta")
        void la_revocacion_se_dirige_por_empresa_y_eje() throws Exception {
            when(revokeUseCase.execute(any())).thenReturn(revocada());

            mockMvc.perform(
                    post("/system/company-limit-overrides/companies/77/dimensions/4/revocations")
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_REVOCACION));

            ArgumentCaptor<RevokeCompanyLimitOverrideCommand> command = ArgumentCaptor
                    .forClass(RevokeCompanyLimitOverrideCommand.class);
            verify(revokeUseCase).execute(command.capture());
            assertThat(command.getValue().companyId()).isEqualTo(77L);
            assertThat(command.getValue().limitDimensionId()).isEqualTo(4L);
            assertThat(command.getValue().revokedBySystemUserId())
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID);
            assertThat(command.getValue().revokedReasonCode())
                    .isEqualTo(OverrideReasonCode.COMMERCIAL_AGREEMENT);
        }

        @Test
        @DisplayName("revocar una excepcion que no existe responde 404")
        void revocar_inexistente_responde_404() throws Exception {
            when(revokeUseCase.execute(any()))
                    .thenThrow(new CompanyLimitOverrideNotFoundException(77L, 4L));

            mockMvc.perform(
                    post("/system/company-limit-overrides/companies/77/dimensions/4/revocations")
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_REVOCACION))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("revocar dos veces responde 409")
        void revocar_dos_veces_responde_409() throws Exception {
            when(revokeUseCase.execute(any())).thenThrow(new OverrideAlreadyRevokedException(77L,
                    4L, LocalDateTime.of(2026, 8, 27, 12, 0)));

            mockMvc.perform(
                    post("/system/company-limit-overrides/companies/77/dimensions/4/revocations")
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_REVOCACION))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Lectura de plataforma")
    class LecturaDePlataforma {

        @Test
        @DisplayName("GET /companies/{id} lista la historia de la empresa que pide la ruta")
        void get_por_empresa() throws Exception {
            when(listUseCase.listByCompanyId(77L)).thenReturn(List.of(concedida()));

            mockMvc.perform(get("/system/company-limit-overrides/companies/77"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].companyId").value(77));

            verify(listUseCase).listByCompanyId(77L);
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        /**
         * <strong>La aserción más importante de esta feature.</strong> Conceder y
         * revocar tienen que quedar en {@code hasRole('SYSTEM')} a secas: si el gate
         * admitiera al empleado de la clínica, la administradora se subiría el techo
         * cada vez que topa y el cupo dejaría de ser un cupo. Y ArchUnit no lo vería —
         * un {@code @authz.isMyCompany(#command.companyId)} pasa sus reglas duras
         * enteras, porque solo prueba que el llamador declara <em>su propia</em>
         * empresa, que es justo lo que un atacante interno declararía.
         */
        @Test
        @DisplayName("conceder y revocar exigen SYSTEM a secas: subir un techo lleva firma")
        void conceder_y_revocar_exigen_system() throws Exception {
            assertThat(GrantCompanyLimitOverrideUseCase.class
                    .getMethod("execute", GrantCompanyLimitOverrideCommand.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
            assertThat(RevokeCompanyLimitOverrideUseCase.class
                    .getMethod("execute", RevokeCompanyLimitOverrideCommand.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
        }

        @Test
        @DisplayName("ningun request declara companyId ni el usuario que firma")
        void ningun_request_declara_company_id_ni_firma() {
            assertThat(GrantCompanyLimitOverrideRequest.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .doesNotContain("companyId", "grantedBySystemUserId");
            assertThat(RevokeCompanyLimitOverrideRequest.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .doesNotContain("companyId", "revokedBySystemUserId");
        }
    }

    @Nested
    @DisplayName("Techo efectivo")
    class TechoEfectivo {

        /**
         * Es lo que soporte necesita <em>antes</em> de negociar: sin el origen, subir
         * un techo que ya venía de una excepción viva abre una segunda sobre el mismo
         * eje y el índice único la rechaza a mitad de la llamada comercial.
         */
        @Test
        @DisplayName("GET /companies/{id}/effective-limits/{eje} resuelve la empresa de la ruta")
        void get_resuelve_la_empresa_de_la_ruta() throws Exception {
            when(resolveUseCase.resolve(42L, 4L)).thenReturn(
                    new EffectiveLimitDto(42L, 4L, 250, LimitSource.COMPANY_OVERRIDE, 31L, false));

            mockMvc.perform(get("/system/company-limit-overrides/companies/42/effective-limits/4"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.companyId").value(42))
                    .andExpect(jsonPath("$.source").value("COMPANY_OVERRIDE"))
                    .andExpect(jsonPath("$.overrideId").value(31));

            verify(resolveUseCase).resolve(42L, 4L);
        }
    }
}
