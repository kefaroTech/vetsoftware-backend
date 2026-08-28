package com.vetsoftware.app.companyusageevent.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.vetsoftware.app.companyusageevent.application.command.AttachUsageEventToChargeCommand;
import com.vetsoftware.app.companyusageevent.application.command.RecordCompanyUsageEventCommand;
import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import com.vetsoftware.app.companyusageevent.application.port.in.AttachUsageEventToChargeUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.FindCompanyUsageEventUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.ListCompanyUsageEventsUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.ListUsageEventsByChargeUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.RecordCompanyUsageEventUseCase;
import com.vetsoftware.app.companyusageevent.domain.UsageBranch;
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
 * Rodaja web de la consola de plataforma sobre los hechos de uso.
 *
 * <p>
 * Lo que congela esta clase y no ve ninguna prueba de servicio:
 *
 * <ul>
 * <li><b>La empresa viaja por la query string y NUNCA en el cuerpo.</b> Es la
 * regla dura {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}: un {@code companyId} que el
 * cliente escribe en el JSON convierte cualquier comprobacion de tenant en una
 * comparacion del numero consigo mismo. Los dos casos de escritura capturan el
 * command y comprueban que el {@code companyId} salio del parametro.
 * <li><b>Los cinco campos del cuerpo llegan al command en su posicion.</b> Dos
 * son identificadores {@code Long} y dos son texto; cruzar
 * {@code limitDimensionCode} con {@code periodKey}, o {@code usageReferenceId}
 * con el cargo, compila sin una queja. El caso feliz los compara uno a uno con
 * valores todos distintos.
 * <li><b>El {@code @Valid} del {@code @RequestBody}.</b> Sin el, los
 * {@code @NotNull} y el {@code @Pattern} del record estan escritos y no se
 * evaluan nunca (#135). Los casos de 400 se ponen rojos el dia que alguien lo
 * quite.
 * <li><b>{@code occurredAt} es un campo del cuerpo y no lo pone el
 * servidor.</b> De eso depende {@code uq_cue_fact}: si el instante lo pusiera
 * el reloj del proceso, el reintento de la medicion dejaria de chocar y el
 * excedente se facturaria dos veces. Que el contrato HTTP lo exija es la mitad
 * visible de esa garantia.
 * </ul>
 *
 * <p>
 * <b>Lo que esta clase NO puede cubrir, y no se finge que si:</b> el
 * {@code hasRole('SYSTEM')} de los cinco puertos. El gate vive en el
 * {@code @PreAuthorize} de la interfaz de {@code port/in}, y aqui los cinco
 * puertos son {@code @MockitoBean} —sin proxy de seguridad—, ademas de que
 * {@code addFilters = false} desactiva la cadena de filtros. Un caso que
 * afirmara «403 con principal de empleado» pasaria por el motivo equivocado y
 * seguiria verde el dia que alguien borrara la anotacion. Quien comprueba de
 * verdad que la feature entera esta cerrada a plataforma es la regla de
 * ArchUnit {@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}, mas la auditoria de
 * autorizacion.
 */
@WebMvcTest(SystemCompanyUsageEventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemCompanyUsageEventController — contrato HTTP de plataforma")
class SystemCompanyUsageEventControllerTest {

    private static final Long COMPANY_ID = 900L;
    private static final Long EVENTO_ID = 8650L;
    private static final Long DUENO_ID = 8600L;
    private static final Long CARGO_ID = 8610L;
    private static final Long EJE_ID = 41L;

    private static final LocalDateTime OCURRIO_EL = LocalDateTime.of(2026, 3, 14, 9, 30, 15);
    private static final LocalDateTime ANOTADO_EL = LocalDateTime.of(2026, 3, 14, 9, 30, 20);

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RecordCompanyUsageEventUseCase recordUseCase;
    @MockitoBean
    private AttachUsageEventToChargeUseCase attachUseCase;
    @MockitoBean
    private FindCompanyUsageEventUseCase findUseCase;
    @MockitoBean
    private ListCompanyUsageEventsUseCase listUseCase;
    @MockitoBean
    private ListUsageEventsByChargeUseCase listByChargeUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Registro del hecho")
    class Registro {

        @Test
        @DisplayName("responde 201, toma la empresa del parametro y traslada los cinco campos"
                + " del cuerpo sin cruzarlos")
        void responde_201_y_traslada_los_cinco_campos_sin_cruzarlos() throws Exception {
            when(recordUseCase.execute(any())).thenReturn(dto(null));

            mockMvc.perform(post("/system/company-usage-events").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "limitDimensionCode": "OWNER",
                              "usageReferenceId": 8600,
                              "occurredAt": "2026-03-14T09:30:15",
                              "periodKey": "2026-03",
                              "billable": true
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(EVENTO_ID))
                    .andExpect(jsonPath("$.branch").value("OWNER"))
                    .andExpect(jsonPath("$.chargeId").doesNotExist());

            ArgumentCaptor<RecordCompanyUsageEventCommand> command = ArgumentCaptor
                    .forClass(RecordCompanyUsageEventCommand.class);
            verify(recordUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                // La empresa sale del parametro, no del JSON: si un dia alguien la
                // moviera al cuerpo, este assert seguiria pasando pero la regla dura
                // EMPRESA_NO_VIAJA_EN_EL_CUERPO rompe el build antes.
                assertThat(cmd.companyId()).isEqualTo(COMPANY_ID);
                assertThat(cmd.limitDimensionCode()).isEqualTo("OWNER");
                assertThat(cmd.usageReferenceId()).isEqualTo(DUENO_ID);
                assertThat(cmd.occurredAt()).isEqualTo(OCURRIO_EL);
                assertThat(cmd.periodKey()).isEqualTo("2026-03");
                assertThat(cmd.billable()).isTrue();
            });
        }

        @Test
        @DisplayName("un cuerpo sin eje, sin referencia, sin instante ni periodo sale 400"
                + " nombrandolos")
        void un_cuerpo_sin_los_obligatorios_sale_400_nombrandolos() throws Exception {
            mockMvc.perform(post("/system/company-usage-events").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "billable": true
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[*].field", Matchers.containsInAnyOrder(
                            "limitDimensionCode", "usageReferenceId", "occurredAt", "periodKey")));

            verifyNoInteractions(recordUseCase);
        }

        /**
         * {@code billable} es {@code Boolean} y no {@code boolean} en el request, y eso
         * es deliberado: con el primitivo, un cuerpo que omite el campo entraria como
         * {@code false} —un hecho que nadie va a cobrar— sin que nada avisara. El
         * {@code @NotNull} obliga a decirlo.
         */
        @Test
        @DisplayName("omitir billable sale 400: un hecho que no dice si se cobra no es un hecho")
        void omitir_billable_sale_400() throws Exception {
            mockMvc.perform(post("/system/company-usage-events").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "limitDimensionCode": "OWNER",
                              "usageReferenceId": 8600,
                              "occurredAt": "2026-03-14T09:30:15",
                              "periodKey": "2026-03"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("billable"));

            verifyNoInteractions(recordUseCase);
        }

        @Test
        @DisplayName("un periodo con mes trece sale 400 en el binder, antes de llegar al motor")
        void un_periodo_invalido_sale_400() throws Exception {
            mockMvc.perform(post("/system/company-usage-events").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "limitDimensionCode": "OWNER",
                              "usageReferenceId": 8600,
                              "occurredAt": "2026-03-14T09:30:15",
                              "periodKey": "2026-13",
                              "billable": true
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("periodKey"));

            verifyNoInteractions(recordUseCase);
        }

        @Test
        @DisplayName("una referencia consumida negativa sale 400")
        void una_referencia_negativa_sale_400() throws Exception {
            mockMvc.perform(post("/system/company-usage-events").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "limitDimensionCode": "OWNER",
                              "usageReferenceId": -1,
                              "occurredAt": "2026-03-14T09:30:15",
                              "periodKey": "2026-03",
                              "billable": true
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("usageReferenceId"));

            verifyNoInteractions(recordUseCase);
        }

        @Test
        @DisplayName("sin el parametro companyId no hay peticion: sale 400 y el caso de uso"
                + " ni se entera")
        void sin_el_parametro_de_empresa_sale_400() throws Exception {
            mockMvc.perform(post("/system/company-usage-events")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "limitDimensionCode": "OWNER",
                              "usageReferenceId": 8600,
                              "occurredAt": "2026-03-14T09:30:15",
                              "periodKey": "2026-03",
                              "billable": true
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(recordUseCase);
        }
    }

    @Nested
    @DisplayName("Enganche con el cargo")
    class Enganche {

        @Test
        @DisplayName("toma el id de la ruta, la empresa del parametro y el cargo del cuerpo")
        void toma_cada_dato_de_su_sitio() throws Exception {
            when(attachUseCase.execute(any())).thenReturn(dto(CARGO_ID));

            mockMvc.perform(patch("/system/company-usage-events/{id}/charge", EVENTO_ID)
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "chargeId": 8610
                            }
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.chargeId").value(CARGO_ID));

            ArgumentCaptor<AttachUsageEventToChargeCommand> command = ArgumentCaptor
                    .forClass(AttachUsageEventToChargeCommand.class);
            verify(attachUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.id()).isEqualTo(EVENTO_ID);
                assertThat(cmd.companyId()).isEqualTo(COMPANY_ID);
                assertThat(cmd.chargeId()).isEqualTo(CARGO_ID);
            });
        }

        @Test
        @DisplayName("un cuerpo sin cargo sale 400 y no llega al caso de uso")
        void un_cuerpo_sin_cargo_sale_400() throws Exception {
            mockMvc.perform(patch("/system/company-usage-events/{id}/charge", EVENTO_ID)
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON)
                    .content("{}")).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("chargeId"));

            verifyNoInteractions(attachUseCase);
        }
    }

    @Nested
    @DisplayName("Lecturas")
    class Lecturas {

        @Test
        @DisplayName("el detalle publica la rama, la referencia y los dos instantes, y no"
                + " publica la version")
        void el_detalle_publica_lo_que_toca_y_no_la_version() throws Exception {
            when(findUseCase.findById(EVENTO_ID)).thenReturn(dto(CARGO_ID));

            mockMvc.perform(get("/system/company-usage-events/{id}", EVENTO_ID))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(EVENTO_ID))
                    .andExpect(jsonPath("$.companyId").value(COMPANY_ID))
                    .andExpect(jsonPath("$.limitDimensionId").value(EJE_ID))
                    .andExpect(jsonPath("$.branch").value("OWNER"))
                    .andExpect(jsonPath("$.usageReferenceId").value(DUENO_ID))
                    .andExpect(jsonPath("$.occurredAt").value("2026-03-14T09:30:15"))
                    .andExpect(jsonPath("$.createdDate").value("2026-03-14T09:30:20"))
                    .andExpect(jsonPath("$.periodKey").value("2026-03"))
                    .andExpect(jsonPath("$.billable").value(true))
                    // La version es una barandilla del que escribe, no un dato del
                    // hecho: publicarla invitaria a construir un protocolo de
                    // concurrencia sobre una consola que solo lee plataforma.
                    .andExpect(jsonPath("$.version").doesNotExist());
        }

        /**
         * El barrido de plataforma. <b>Los cinco campos de la pagina son los de la
         * consulta, no los del contenido mapeado</b>: con doce millones de filas
         * proyectadas, un {@code totalElements} recalculado sobre la pagina diria
         * siempre el tamano de la pagina.
         */
        @Test
        @DisplayName("el listado de plataforma pagina y arrastra los totales de la consulta")
        void el_listado_pagina_y_arrastra_los_totales() throws Exception {
            when(listUseCase.listAll(0, 20))
                    .thenReturn(new PageResult<>(List.of(dto(null)), 0, 20, 12_000_000L, 600_000));

            mockMvc.perform(get("/system/company-usage-events")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20))
                    .andExpect(jsonPath("$.totalElements").value(12_000_000L))
                    .andExpect(jsonPath("$.totalPages").value(600_000));
        }

        @Test
        @DisplayName("el listado por empresa traslada la empresa y la pagina pedidas")
        void el_listado_por_empresa_traslada_sus_parametros() throws Exception {
            when(listUseCase.listByCompany(eq(COMPANY_ID), anyInt(), anyInt()))
                    .thenReturn(new PageResult<>(List.of(dto(null)), 2, 50, 51L, 3));

            mockMvc.perform(get("/system/company-usage-events/by-company").param("companyId", "900")
                    .param("page", "2").param("pageSize", "50")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(2));

            verify(listUseCase).listByCompany(COMPANY_ID, 2, 50);
        }

        /**
         * El desglose de un cargo por excedente: es la consulta con la que se responde
         * a «por que me cobraron esto», asi que el cargo y la empresa tienen que llegar
         * los dos al puerto.
         */
        @Test
        @DisplayName("el desglose por cargo traslada la empresa y el cargo")
        void el_desglose_por_cargo_traslada_sus_parametros() throws Exception {
            when(listByChargeUseCase.listByCharge(eq(COMPANY_ID), eq(CARGO_ID), anyInt(), anyInt()))
                    .thenReturn(new PageResult<>(List.of(dto(CARGO_ID)), 0, 20, 1L, 1));

            mockMvc.perform(get("/system/company-usage-events/by-charge").param("companyId", "900")
                    .param("chargeId", "8610")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].chargeId").value(CARGO_ID));

            verify(listByChargeUseCase).listByCharge(COMPANY_ID, CARGO_ID, 0, 20);
        }
    }

    private static CompanyUsageEventDto dto(Long chargeId) {
        return new CompanyUsageEventDto(EVENTO_ID, COMPANY_ID, EJE_ID, UsageBranch.OWNER, DUENO_ID,
                OCURRIO_EL, "2026-03", true, chargeId, ANOTADO_EL);
    }
}
