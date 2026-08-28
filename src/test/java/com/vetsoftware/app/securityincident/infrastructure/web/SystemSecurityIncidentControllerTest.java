package com.vetsoftware.app.securityincident.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.securityincident.application.command.CloseSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.command.RegisterAffectedCompanyCommand;
import com.vetsoftware.app.securityincident.application.command.RegisterSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.command.ReportSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.dto.AffectedCompanyDto;
import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import com.vetsoftware.app.securityincident.application.port.in.CloseSecurityIncidentUseCase;
import com.vetsoftware.app.securityincident.application.port.in.FindSecurityIncidentUseCase;
import com.vetsoftware.app.securityincident.application.port.in.ListAffectedCompaniesUseCase;
import com.vetsoftware.app.securityincident.application.port.in.ListSecurityIncidentsUseCase;
import com.vetsoftware.app.securityincident.application.port.in.RegisterAffectedCompanyUseCase;
import com.vetsoftware.app.securityincident.application.port.in.RegisterSecurityIncidentUseCase;
import com.vetsoftware.app.securityincident.application.port.in.ReportSecurityIncidentUseCase;
import com.vetsoftware.app.securityincident.domain.AffectedScope;
import com.vetsoftware.app.securityincident.domain.IncidentSeverity;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentKind;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja web del expediente de incidentes, que es entero de plataforma.
 *
 * <p>
 * Lo que congela esta clase y no ve ningun test de servicio:
 *
 * <ul>
 * <li><b>Las tres fechas del alta llegan al command en su posicion.</b>
 * {@code detectedAt}, {@code occurredAt} y {@code escalatedAt} son del mismo
 * tipo y cruzarlas compila sin una queja — y cruzar las dos ultimas mueve el
 * vencimiento del reporte a la SIC, que es el unico campo del que depende
 * cumplir o no cumplir. Por eso el caso feliz captura el command y compara los
 * siete campos con valores todos distintos.</li>
 * <li><b>La clinica alcanzada sale de la RUTA, no del cuerpo.</b>
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} —regla dura— baja recursivamente por
 * los campos del {@code @RequestBody}, asi que un {@code companyId} ahi dentro
 * rompe el build; la salida que la propia regla documenta es el
 * {@code @PathVariable}. El caso lo comprueba enviando un cuerpo que
 * <em>no</em> lo lleva y afirmando que el command igualmente lo recibe.</li>
 * </ul>
 *
 * <p>
 * <b>Lo que esta clase NO cubre, y no por olvido: la autorizacion.</b> El gate
 * de esta rodaja es {@code @PreAuthorize("hasRole('SYSTEM')")} y vive en los
 * <em>puertos de entrada</em>, que aqui son {@code @MockitoBean}; ademas
 * {@code @AutoConfigureMockMvc(addFilters = false)} quita la cadena de
 * seguridad. Un caso que pidiera un 401 o un 403 en este contexto no probaria
 * el gate: probaria el andamio, y pasaria en verde el dia que alguien borrara
 * la anotacion. Quien vigila que ningun puerto se quede sin gate es ArchUnit
 * —{@code PUERTO_SIN_PREAUTHORIZE} y
 * {@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}—, y ahi si es una regla dura que
 * rompe el build.
 */
@WebMvcTest(SystemSecurityIncidentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemSecurityIncidentController — contrato HTTP de plataforma")
class SystemSecurityIncidentControllerTest {

    private static final Long INCIDENTE_ID = 8601L;
    private static final Long COMPANY_ID = 4207L;

    private static final LocalDateTime OCURRIO = LocalDateTime.of(2026, 3, 2, 22, 15, 0);
    private static final LocalDateTime DETECTADO = LocalDateTime.of(2026, 3, 3, 8, 30, 0);
    private static final LocalDateTime ESCALADO = LocalDateTime.of(2026, 3, 5, 9, 0, 0);
    private static final LocalDateTime VENCE = LocalDateTime.of(2026, 3, 26, 23, 59, 59);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 5, 9, 5, 0);

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RegisterSecurityIncidentUseCase registerUseCase;
    @MockitoBean
    private ReportSecurityIncidentUseCase reportUseCase;
    @MockitoBean
    private CloseSecurityIncidentUseCase closeUseCase;
    @MockitoBean
    private FindSecurityIncidentUseCase findUseCase;
    @MockitoBean
    private ListSecurityIncidentsUseCase listUseCase;
    @MockitoBean
    private RegisterAffectedCompanyUseCase registerAffectedUseCase;
    @MockitoBean
    private ListAffectedCompaniesUseCase listAffectedUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Alta")
    class Alta {

        @Test
        @DisplayName("responde 201 y traslada los siete campos del cuerpo al command sin cruzarlos")
        void responde_201_y_traslada_los_siete_campos_sin_cruzarlos() throws Exception {
            when(registerUseCase.execute(any())).thenReturn(dtoAbierto());

            mockMvc.perform(post("/system/security-incidents")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "detectedAt": "2026-03-03T08:30:00",
                              "occurredAt": "2026-03-02T22:15:00",
                              "escalatedAt": "2026-03-05T09:00:00",
                              "kind": "DATA_LEAK",
                              "severity": "HIGH",
                              "summary": "Llave de API expuesta en un repositorio publico",
                              "affectedSubjectCount": 1200
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(8601))
                    .andExpect(jsonPath("$.deadlineAt").value("2026-03-26T23:59:59"));

            ArgumentCaptor<RegisterSecurityIncidentCommand> command = ArgumentCaptor
                    .forClass(RegisterSecurityIncidentCommand.class);
            verify(registerUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                // Las tres fechas son del mismo tipo y estan a horas distintas a
                // proposito: cruzar escalatedAt con detectedAt mueve el vencimiento
                // del reporte a la SIC y compila sin una queja.
                assertThat(cmd.detectedAt()).isEqualTo(DETECTADO);
                assertThat(cmd.occurredAt()).isEqualTo(OCURRIO);
                assertThat(cmd.escalatedAt()).isEqualTo(ESCALADO);
                assertThat(cmd.kind()).isEqualTo(SecurityIncidentKind.DATA_LEAK);
                assertThat(cmd.severity()).isEqualTo(IncidentSeverity.HIGH);
                assertThat(cmd.summary())
                        .isEqualTo("Llave de API expuesta en un repositorio publico");
                assertThat(cmd.affectedSubjectCount()).isEqualTo(1200);
            });
        }

        @Test
        @DisplayName("un incidente del que no se sabe cuando ocurrio entra sin esa fecha")
        void un_incidente_sin_fecha_de_ocurrencia_entra() throws Exception {
            when(registerUseCase.execute(any())).thenReturn(dtoAbierto());

            mockMvc.perform(post("/system/security-incidents")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "detectedAt": "2026-03-03T08:30:00",
                              "escalatedAt": "2026-03-05T09:00:00",
                              "kind": "RANSOMWARE",
                              "severity": "CRITICAL",
                              "summary": "Cifrado hostil en un servidor de respaldo",
                              "affectedSubjectCount": 0
                            }
                            """)).andExpect(status().isCreated());

            ArgumentCaptor<RegisterSecurityIncidentCommand> command = ArgumentCaptor
                    .forClass(RegisterSecurityIncidentCommand.class);
            verify(registerUseCase).execute(command.capture());
            assertThat(command.getValue().occurredAt()).isNull();
        }

        @Test
        @DisplayName("un cuerpo sin deteccion, sin escalamiento, sin clase ni resumen sale 400 "
                + "nombrandolos")
        void un_cuerpo_sin_los_obligatorios_sale_400_nombrandolos() throws Exception {
            // El @Valid del @RequestBody es lo unico que dispara el validador; sin
            // el, los @NotNull del record estan escritos y no se evaluan nunca
            // (#135). Este caso se pone rojo el dia que alguien lo quite.
            mockMvc.perform(post("/system/security-incidents")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "affectedSubjectCount": 3
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED")).andExpect(
                            jsonPath("$.errors[*].field", Matchers.containsInAnyOrder("detectedAt",
                                    "escalatedAt", "kind", "severity", "summary")));

            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("el escalamiento es obligatorio: sin el no hay de donde colgar el plazo")
        void el_escalamiento_es_obligatorio() throws Exception {
            // Es la unica fecha del alta que la norma convierte en dies a quo. Si
            // dejara de ser obligatoria, el vencimiento tendria que salir de la
            // deteccion y el plazo saldria mas largo que el real, siempre en la
            // direccion de incumplir.
            mockMvc.perform(post("/system/security-incidents")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "detectedAt": "2026-03-03T08:30:00",
                              "kind": "DATA_LEAK",
                              "severity": "HIGH",
                              "summary": "Llave de API expuesta",
                              "affectedSubjectCount": 10
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("escalatedAt"));

            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("un contador de afectados negativo sale 400")
        void un_contador_negativo_sale_400() throws Exception {
            mockMvc.perform(post("/system/security-incidents")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "detectedAt": "2026-03-03T08:30:00",
                              "escalatedAt": "2026-03-05T09:00:00",
                              "kind": "DATA_LEAK",
                              "severity": "HIGH",
                              "summary": "Llave de API expuesta",
                              "affectedSubjectCount": -1
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("affectedSubjectCount"));

            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("una clase de incidente que no existe se rechaza en el binder")
        void una_clase_de_incidente_que_no_existe_se_rechaza() throws Exception {
            // La lista es cerrada tambien en el motor (chk_security_incidents_kind):
            // un PHISHING entra aqui o no entra en ningun lado.
            mockMvc.perform(post("/system/security-incidents")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "detectedAt": "2026-03-03T08:30:00",
                              "escalatedAt": "2026-03-05T09:00:00",
                              "kind": "PHISHING",
                              "severity": "HIGH",
                              "summary": "Correo fraudulento",
                              "affectedSubjectCount": 4
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(registerUseCase);
        }
    }

    @Nested
    @DisplayName("Reporte a la autoridad")
    class Reporte {

        @Test
        @DisplayName("PATCH y no POST: el id sale de la ruta y el radicado del cuerpo")
        void patch_con_el_id_en_la_ruta_y_el_radicado_en_el_cuerpo() throws Exception {
            when(reportUseCase.execute(any())).thenReturn(dtoAbierto());

            mockMvc.perform(patch("/system/security-incidents/{id}/report", INCIDENTE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "reportedAt": "2026-03-20T16:40:00",
                              "reportReference": "SIC-2026-004512"
                            }
                            """)).andExpect(status().isOk());

            ArgumentCaptor<ReportSecurityIncidentCommand> command = ArgumentCaptor
                    .forClass(ReportSecurityIncidentCommand.class);
            verify(reportUseCase).execute(command.capture());
            assertThat(command.getValue().id()).isEqualTo(INCIDENTE_ID);
            assertThat(command.getValue().reportedAt())
                    .isEqualTo(LocalDateTime.of(2026, 3, 20, 16, 40, 0));
            assertThat(command.getValue().reportReference()).isEqualTo("SIC-2026-004512");
        }

        @Test
        @DisplayName("reportar sin radicado sale 400: un reporte que no se puede rastrear no "
                + "consta")
        void reportar_sin_radicado_sale_400() throws Exception {
            mockMvc.perform(patch("/system/security-incidents/{id}/report", INCIDENTE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "reportedAt": "2026-03-20T16:40:00",
                              "reportReference": "  "
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("reportReference"));

            verifyNoInteractions(reportUseCase);
        }
    }

    @Nested
    @DisplayName("Cierre")
    class Cierre {

        @Test
        @DisplayName("PATCH y no DELETE: cerrar es escribir como acabo, no retirarlo")
        void patch_y_no_delete() throws Exception {
            when(closeUseCase.execute(any())).thenReturn(dtoAbierto());

            mockMvc.perform(patch("/system/security-incidents/{id}/close", INCIDENTE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "closedAt": "2026-04-10T11:00:00",
                              "containment": "Se revocaron las credenciales expuestas",
                              "rootCause": "Una llave quedo en un repositorio publico",
                              "notifiedSubjectsAt": "2026-04-09T10:00:00"
                            }
                            """)).andExpect(status().isOk());

            ArgumentCaptor<CloseSecurityIncidentCommand> command = ArgumentCaptor
                    .forClass(CloseSecurityIncidentCommand.class);
            verify(closeUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.id()).isEqualTo(INCIDENTE_ID);
                assertThat(cmd.closedAt()).isEqualTo(LocalDateTime.of(2026, 4, 10, 11, 0, 0));
                // Contencion y causa raiz son dos String seguidos: cruzarlos compila,
                // y el expediente quedaria contando la historia al reves.
                assertThat(cmd.containment()).isEqualTo("Se revocaron las credenciales expuestas");
                assertThat(cmd.rootCause()).isEqualTo("Una llave quedo en un repositorio publico");
                assertThat(cmd.notifiedSubjectsAt())
                        .isEqualTo(LocalDateTime.of(2026, 4, 9, 10, 0, 0));
            });
        }

        @Test
        @DisplayName("cerrar sin contencion ni causa raiz sale 400 nombrando las dos")
        void cerrar_sin_narrativa_sale_400() throws Exception {
            // Es la mitad Java de chk_security_incidents_close: un incidente cerrado
            // y sin documentar es indistinguible de uno ocultado.
            mockMvc.perform(patch("/system/security-incidents/{id}/close", INCIDENTE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "closedAt": "2026-04-10T11:00:00",
                              "containment": "",
                              "rootCause": null
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field",
                            Matchers.containsInAnyOrder("containment", "rootCause")));

            verifyNoInteractions(closeUseCase);
        }

        @Test
        @DisplayName("el cierre sin notificacion a titulares entra: la obligacion es con la "
                + "autoridad")
        void el_cierre_sin_notificacion_a_titulares_entra() throws Exception {
            // En Colombia la obligacion legal es informar a la autoridad, no a los
            // titulares. Si este campo pasara a obligatorio, se estaria inventando un
            // deber que la norma no impone.
            when(closeUseCase.execute(any())).thenReturn(dtoAbierto());

            mockMvc.perform(patch("/system/security-incidents/{id}/close", INCIDENTE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "closedAt": "2026-04-10T11:00:00",
                              "containment": "Se revocaron las credenciales",
                              "rootCause": "Llave en repositorio publico"
                            }
                            """)).andExpect(status().isOk());

            ArgumentCaptor<CloseSecurityIncidentCommand> command = ArgumentCaptor
                    .forClass(CloseSecurityIncidentCommand.class);
            verify(closeUseCase).execute(command.capture());
            assertThat(command.getValue().notifiedSubjectsAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Afectados")
    class Afectados {

        @Test
        @DisplayName("la clinica alcanzada sale de la RUTA y no del cuerpo")
        void la_clinica_alcanzada_sale_de_la_ruta() throws Exception {
            // EMPRESA_NO_VIAJA_EN_EL_CUERPO —regla dura— baja por los campos del
            // @RequestBody, asi que un companyId ahi dentro rompe el build. El cuerpo
            // que se envia aqui NO lo lleva, y el command igualmente lo recibe: eso
            // solo puede venir del @PathVariable.
            when(registerAffectedUseCase.execute(any())).thenReturn(dtoAfectada());

            mockMvc.perform(post("/system/security-incidents/{id}/affected-companies/{companyId}",
                    INCIDENTE_ID, COMPANY_ID).contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "affectedScope": "CLINICAL_DATA",
                              "affectedSubjectCount": 480
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.companyId").value(4207));

            ArgumentCaptor<RegisterAffectedCompanyCommand> command = ArgumentCaptor
                    .forClass(RegisterAffectedCompanyCommand.class);
            verify(registerAffectedUseCase).execute(command.capture());
            assertThat(command.getValue().securityIncidentId()).isEqualTo(INCIDENTE_ID);
            assertThat(command.getValue().companyId()).isEqualTo(COMPANY_ID);
            assertThat(command.getValue().affectedScope()).isEqualTo(AffectedScope.CLINICAL_DATA);
            assertThat(command.getValue().affectedSubjectCount()).isEqualTo(480);
        }

        @Test
        @DisplayName("registrar un afectado sin ambito sale 400: el ambito entra en la unicidad")
        void registrar_sin_ambito_sale_400() throws Exception {
            // Sin ambito no se puede escribir la fila: es parte de uq_sic_pair, y sin
            // el la misma clinica no podria constar dos veces por dos alcances.
            mockMvc.perform(post("/system/security-incidents/{id}/affected-companies/{companyId}",
                    INCIDENTE_ID, COMPANY_ID).contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "affectedSubjectCount": 480
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("affectedScope"));

            verifyNoInteractions(registerAffectedUseCase);
        }

        @Test
        @DisplayName("el listado de afectados de un incidente sale paginado")
        void el_listado_de_afectados_sale_paginado() throws Exception {
            when(listAffectedUseCase.listByIncident(eq(INCIDENTE_ID), eq(0), eq(20)))
                    .thenReturn(PageResult.of(List.of(dtoAfectada()), 0, 20, 1));

            mockMvc.perform(get("/system/security-incidents/{id}/affected-companies", INCIDENTE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].affectedScope").value("CLINICAL_DATA"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("Consulta")
    class Consulta {

        @Test
        @DisplayName("el barrido de incidentes sale paginado con los valores por defecto")
        void el_barrido_sale_paginado_con_los_valores_por_defecto() throws Exception {
            when(listUseCase.listAll(eq(0), eq(20)))
                    .thenReturn(PageResult.of(List.of(dtoAbierto()), 0, 20, 1));

            mockMvc.perform(get("/system/security-incidents")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(8601))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20));
        }

        @Test
        @DisplayName("el detalle devuelve el incidente con su vencimiento")
        void el_detalle_devuelve_el_incidente_con_su_vencimiento() throws Exception {
            when(findUseCase.findById(INCIDENTE_ID)).thenReturn(dtoAbierto());

            mockMvc.perform(get("/system/security-incidents/{id}", INCIDENTE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.escalatedAt").value("2026-03-05T09:00:00"))
                    .andExpect(jsonPath("$.deadlineAt").value("2026-03-26T23:59:59"))
                    .andExpect(jsonPath("$.reportedToAuthorityAt").doesNotExist());
        }
    }

    private static SecurityIncidentDto dtoAbierto() {
        return new SecurityIncidentDto(INCIDENTE_ID, DETECTADO, OCURRIO, ESCALADO,
                SecurityIncidentKind.DATA_LEAK, IncidentSeverity.HIGH,
                "Llave de API expuesta en un repositorio publico", 1200, VENCE, null, null, null,
                null, null, null, CREADO_EL);
    }

    private static AffectedCompanyDto dtoAfectada() {
        return new AffectedCompanyDto(9001L, INCIDENTE_ID, COMPANY_ID, AffectedScope.CLINICAL_DATA,
                480);
    }
}
