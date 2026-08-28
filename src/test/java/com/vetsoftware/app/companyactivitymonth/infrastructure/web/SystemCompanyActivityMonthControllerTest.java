package com.vetsoftware.app.companyactivitymonth.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.companyactivitymonth.application.command.RecordCompanyActivityMonthCommand;
import com.vetsoftware.app.companyactivitymonth.application.command.UpdateCompanyActivityMonthCommand;
import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import com.vetsoftware.app.companyactivitymonth.application.port.in.FindCompanyActivityMonthUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.in.ListCompanyActivityMonthsUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.in.ListDormantCompaniesUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.in.RecordCompanyActivityMonthUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.in.UpdateCompanyActivityMonthUseCase;
import com.vetsoftware.app.companyactivitymonth.domain.CommercialState;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonthNotFoundException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja web de la serie de actividad, que es solo de plataforma.
 *
 * <p>
 * Lo que congela esta clase y no ve ningun test de servicio:
 *
 * <ul>
 * <li><b>Que la empresa viaje por la query string y los seis campos del cuerpo
 * lleguen al command en su posicion.</b> Tres de ellos —{@code activeDays},
 * {@code activeUsers} y {@code recordsCreated}— son {@code int} consecutivos:
 * cruzarlos compila sin una queja y produce un informe de actividad que miente
 * sin dar un solo error. Por eso el caso feliz captura el command y compara
 * componente a componente con tres valores distintos.</li>
 * <li><b>Que el {@code @Valid} este puesto.</b> Sin el, los {@code @NotNull},
 * el {@code @Pattern} del mes y el {@code @Digits} del MRR estan escritos y no
 * se evaluan nunca (#135) — y springdoc seguiria anunciandolos al front, que es
 * lo que hace al defecto invisible.</li>
 * <li><b>Que la respuesta NO publique {@code version}.</b> Es una barandilla
 * del que escribe, no un dato de la medicion; el dia que alguien la anada al
 * record, este caso se pone rojo.</li>
 * </ul>
 *
 * <p>
 * <b>Lo que esta clase NO prueba, y no por olvido: la autorizacion.</b>
 * {@code WebMvcSliceConfig} sustituye la cadena de seguridad por una permisiva
 * —la real necesita Redis y base de datos— y, sobre todo, el
 * {@code @PreAuthorize} de esta feature vive en los <em>puertos de
 * entrada</em>, que aqui son {@code @MockitoBean}: nunca llega a evaluarse. Que
 * los cinco puertos esten cerrados a {@code hasRole('SYSTEM')} a secas lo
 * verifica ArchUnit, que es donde esa comprobacion tiene sentido. Lo unico que
 * si es de esta rodaja es <b>que forma tiene la negativa cuando ocurre</b>, y
 * eso lo cubre {@code Autorizacion}.
 */
@WebMvcTest(SystemCompanyActivityMonthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemCompanyActivityMonthController — contrato HTTP de plataforma")
class SystemCompanyActivityMonthControllerTest {

    private static final Long MES_ID = 8801L;
    private static final Long EMPRESA = 900L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RecordCompanyActivityMonthUseCase recordUseCase;
    @MockitoBean
    private UpdateCompanyActivityMonthUseCase updateUseCase;
    @MockitoBean
    private FindCompanyActivityMonthUseCase findUseCase;
    @MockitoBean
    private ListCompanyActivityMonthsUseCase listUseCase;
    @MockitoBean
    private ListDormantCompaniesUseCase listDormantUseCase;

    @Nested
    @DisplayName("Alta")
    class Alta {

        @Test
        @DisplayName("responde 201 y traslada la empresa y los seis campos sin cruzarlos")
        void responde_201_y_traslada_los_campos_sin_cruzarlos() throws Exception {
            when(recordUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/system/company-activity-months").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "periodKey": "2029-01",
                              "commercialState": "PAID",
                              "activeDays": 21,
                              "activeUsers": 7,
                              "recordsCreated": 143,
                              "mrrSnapshot": 189000.00
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(MES_ID))
                    .andExpect(jsonPath("$.companyId").value(EMPRESA))
                    .andExpect(jsonPath("$.periodKey").value("2029-01"));

            ArgumentCaptor<RecordCompanyActivityMonthCommand> command = ArgumentCaptor
                    .forClass(RecordCompanyActivityMonthCommand.class);
            verify(recordUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                // La empresa llega del @RequestParam, nunca del cuerpo: el request no
                // tiene ese campo y EMPRESA_NO_VIAJA_EN_EL_CUERPO lo prohibe.
                assertThat(cmd.companyId()).isEqualTo(EMPRESA);
                assertThat(cmd.periodKey()).isEqualTo("2029-01");
                assertThat(cmd.commercialState()).isEqualTo(CommercialState.PAID);
                // Los tres int, cada uno en su sitio.
                assertThat(cmd.activeDays()).isEqualTo(21);
                assertThat(cmd.activeUsers()).isEqualTo(7);
                assertThat(cmd.recordsCreated()).isEqualTo(143);
                assertThat(cmd.mrrSnapshot()).isEqualByComparingTo("189000.00");
            });
        }

        @Test
        @DisplayName("la respuesta no publica la version, que es del que escribe")
        void la_respuesta_no_publica_la_version() throws Exception {
            when(recordUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/system/company-activity-months").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpoValido()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.version").doesNotExist());
        }

        @Test
        @DisplayName("un cuerpo sin mes, sin estado y sin MRR sale 400 nombrando los tres")
        void un_cuerpo_sin_los_obligatorios_sale_400_nombrandolos() throws Exception {
            // El @Valid del @RequestBody es lo unico que dispara el validador; sin el,
            // los @NotNull del record estan escritos y no se evaluan nunca (#135). Este
            // caso se pone rojo el dia que alguien lo quite.
            mockMvc.perform(post("/system/company-activity-months").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "activeDays": 21
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    // hasItems y NO containsInAnyOrder, y el motivo es un hallazgo:
                    // cuando el cuerpo omite un componente PRIMITIVO del record, el 400
                    // lo nombra tambien -activeUsers aqui-, aunque no lleve
                    // @NotNull (un primitivo no puede llevarlo). Exigir la lista exacta
                    // ataria este caso a ese detalle del binder en vez de a lo que de
                    // verdad afirma: que los obligatorios salen nombrados.
                    .andExpect(jsonPath("$.errors[*].field",
                            Matchers.hasItems("periodKey", "commercialState", "mrrSnapshot")));

            verifyNoInteractions(recordUseCase);
        }

        @Test
        @DisplayName("un mes 13 sale 400 en el campo y no baja hasta el motor")
        void un_mes_13_sale_400_en_el_campo() throws Exception {
            // El @Pattern es el mismo REGEXP de chk_cam_period_key. Sin el, un 2029-13
            // llegaria a la base para volver como error de integridad, que el front no
            // sabe pintar bajo el input.
            mockMvc.perform(post("/system/company-activity-months").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "periodKey": "2029-13",
                              "commercialState": "PAID",
                              "activeDays": 21,
                              "activeUsers": 7,
                              "recordsCreated": 143,
                              "mrrSnapshot": 189000.00
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("periodKey"));

            verifyNoInteractions(recordUseCase);
        }

        @Test
        @DisplayName("treinta y dos dias activos salen 400: ningun mes los tiene")
        void treinta_y_dos_dias_activos_salen_400() throws Exception {
            mockMvc.perform(post("/system/company-activity-months").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "periodKey": "2029-01",
                              "commercialState": "PAID",
                              "activeDays": 32,
                              "activeUsers": 7,
                              "recordsCreated": 143,
                              "mrrSnapshot": 189000.00
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("activeDays"));

            verifyNoInteractions(recordUseCase);
        }

        @Test
        @DisplayName("un MRR negativo sale 400: un ingreso recurrente no resta")
        void un_mrr_negativo_sale_400() throws Exception {
            mockMvc.perform(post("/system/company-activity-months").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "periodKey": "2029-01",
                              "commercialState": "PAID",
                              "activeDays": 21,
                              "activeUsers": 7,
                              "recordsCreated": 143,
                              "mrrSnapshot": -1.00
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("mrrSnapshot"));

            verifyNoInteractions(recordUseCase);
        }

        @Test
        @DisplayName("un tercer decimal en el MRR sale 400 y no se redondea callando")
        void un_tercer_decimal_en_el_mrr_sale_400() throws Exception {
            // La columna es DECIMAL(19,2): un tercer decimal no lo rechaza el motor, lo
            // REDONDEA, y el MRR guardado deja de ser el que alguien calculo.
            mockMvc.perform(post("/system/company-activity-months").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "periodKey": "2029-01",
                              "commercialState": "PAID",
                              "activeDays": 21,
                              "activeUsers": 7,
                              "recordsCreated": 143,
                              "mrrSnapshot": 189000.001
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("mrrSnapshot"));

            verifyNoInteractions(recordUseCase);
        }

        @Test
        @DisplayName("un estado comercial que no existe se rechaza en el binder")
        void un_estado_comercial_que_no_existe_se_rechaza() throws Exception {
            // La lista es cerrada en los dos lados: un ACTIVE por PAID entra aqui o no
            // entra en ningun sitio.
            mockMvc.perform(post("/system/company-activity-months").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "periodKey": "2029-01",
                              "commercialState": "ACTIVE",
                              "activeDays": 21,
                              "activeUsers": 7,
                              "recordsCreated": 143,
                              "mrrSnapshot": 189000.00
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(recordUseCase);
        }

        @Test
        @DisplayName("sin la empresa en la query string sale 400 y no se inventa ninguna")
        void sin_la_empresa_en_la_query_string_sale_400() throws Exception {
            // Un principal SYSTEM no tiene empresa propia de la que derivarla, asi que
            // omitirla no puede resolverse con un defecto: seria escribir la actividad
            // de una clinica cualquiera.
            mockMvc.perform(post("/system/company-activity-months")
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpoValido()))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(recordUseCase);
        }
    }

    @Nested
    @DisplayName("Recalculo")
    class Recalculo {

        @Test
        @DisplayName("PATCH responde 200 y lleva el id de la ruta y los cinco numeros al command")
        void patch_responde_200_y_traslada_el_id_y_los_numeros() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(patch("/system/company-activity-months/{id}", MES_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "commercialState": "CHURNED",
                              "activeDays": 4,
                              "activeUsers": 2,
                              "recordsCreated": 11,
                              "mrrSnapshot": 0.00
                            }
                            """)).andExpect(status().isOk());

            ArgumentCaptor<UpdateCompanyActivityMonthCommand> command = ArgumentCaptor
                    .forClass(UpdateCompanyActivityMonthCommand.class);
            verify(updateUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.id()).isEqualTo(MES_ID);
                assertThat(cmd.commercialState()).isEqualTo(CommercialState.CHURNED);
                assertThat(cmd.activeDays()).isEqualTo(4);
                assertThat(cmd.activeUsers()).isEqualTo(2);
                assertThat(cmd.recordsCreated()).isEqualTo(11);
                assertThat(cmd.mrrSnapshot()).isEqualByComparingTo("0.00");
            });
        }

        @Test
        @DisplayName("el cuerpo del recalculo tambien se valida: sin estado sale 400")
        void el_cuerpo_del_recalculo_tambien_se_valida() throws Exception {
            mockMvc.perform(patch("/system/company-activity-months/{id}", MES_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "activeDays": 4,
                              "activeUsers": 2,
                              "recordsCreated": 11,
                              "mrrSnapshot": 0.00
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("commercialState"));

            verifyNoInteractions(updateUseCase);
        }
    }

    @Nested
    @DisplayName("Consulta")
    class Consulta {

        @Test
        @DisplayName("GET por id devuelve los nueve campos de la respuesta")
        void get_por_id_devuelve_la_respuesta_entera() throws Exception {
            when(findUseCase.findById(MES_ID)).thenReturn(dto());

            mockMvc.perform(get("/system/company-activity-months/{id}", MES_ID))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(MES_ID))
                    .andExpect(jsonPath("$.companyId").value(EMPRESA))
                    .andExpect(jsonPath("$.periodKey").value("2029-01"))
                    .andExpect(jsonPath("$.commercialState").value("PAID"))
                    .andExpect(jsonPath("$.activeDays").value(21))
                    .andExpect(jsonPath("$.activeUsers").value(7))
                    .andExpect(jsonPath("$.recordsCreated").value(143))
                    .andExpect(jsonPath("$.mrrSnapshot").value(189000.00))
                    .andExpect(jsonPath("$.createdDate").exists());
        }

        @Test
        @DisplayName("la busqueda por clinica y mes lleva los dos parametros al caso de uso")
        void la_busqueda_por_clinica_y_mes_lleva_los_dos_parametros() throws Exception {
            // Ruta propia y no un filtro sobre el listado porque devuelve UN recurso:
            // uq_cam_month garantiza que hay como mucho uno, y un 404 dice mas que una
            // lista vacia.
            when(findUseCase.findByCompanyIdAndPeriodKey(EMPRESA, "2029-01")).thenReturn(dto());

            mockMvc.perform(get("/system/company-activity-months/lookup").param("companyId", "900")
                    .param("periodKey", "2029-01")).andExpect(status().isOk());

            verify(findUseCase).findByCompanyIdAndPeriodKey(EMPRESA, "2029-01");
        }

        @Test
        @DisplayName("el listado global sale con la forma de pagina y sus cinco campos")
        void el_listado_global_sale_con_la_forma_de_pagina() throws Exception {
            when(listUseCase.listAll(0, 20)).thenReturn(pagina());

            mockMvc.perform(get("/system/company-activity-months")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(MES_ID))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(listUseCase).listAll(0, 20);
        }

        @Test
        @DisplayName("el listado por clinica acota con la empresa de la query string")
        void el_listado_por_clinica_acota_con_la_empresa() throws Exception {
            when(listUseCase.listByCompany(EMPRESA, 2, 50)).thenReturn(pagina());

            mockMvc.perform(get("/system/company-activity-months/by-company")
                    .param("companyId", "900").param("page", "2").param("pageSize", "50"))
                    .andExpect(status().isOk());

            verify(listUseCase).listByCompany(EMPRESA, 2, 50);
        }

        @Test
        @DisplayName("el listado por mes acota con el periodo y no filtra por empresa")
        void el_listado_por_mes_acota_con_el_periodo() throws Exception {
            // No filtra por empresa a proposito: comparar clinicas entre si ES el
            // producto. Por eso el puerto va cerrado a hasRole('SYSTEM') a secas.
            when(listUseCase.listByPeriod("2029-01", 0, 20)).thenReturn(pagina());

            mockMvc.perform(
                    get("/system/company-activity-months/by-period").param("periodKey", "2029-01"))
                    .andExpect(status().isOk());

            verify(listUseCase).listByPeriod("2029-01", 0, 20);
        }
    }

    @Nested
    @DisplayName("Dormidos")
    class Dormidos {

        @Test
        @DisplayName("el barrido lleva el mes y el umbral al caso de uso, en ese orden")
        void el_barrido_lleva_el_mes_y_el_umbral() throws Exception {
            when(listDormantUseCase.listDormant("2029-01", 3, 0, 20)).thenReturn(pagina());

            mockMvc.perform(get("/system/company-activity-months/dormant")
                    .param("periodKey", "2029-01").param("activeDaysThreshold", "3"))
                    .andExpect(status().isOk());

            verify(listDormantUseCase).listDormant("2029-01", 3, 0, 20);
        }

        @Test
        @DisplayName("sin umbral sale 400: no hay un numero por defecto que sea correcto")
        void sin_umbral_sale_400() throws Exception {
            // «Dormido» son tres dias para quien mira retencion y cero para quien mira
            // bajas. Poner un defecto aqui seria decidir por quien pregunta, y devolver
            // una lista que no es la que pidio.
            mockMvc.perform(
                    get("/system/company-activity-months/dormant").param("periodKey", "2029-01"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(listDormantUseCase);
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        @Test
        @DisplayName("la negativa del gate sale 403 y no 500")
        void la_negativa_del_gate_sale_403() throws Exception {
            // ESTO NO PRUEBA EL GATE, prueba la FORMA DE LA NEGATIVA. El
            // @PreAuthorize("hasRole('SYSTEM')") vive en el puerto de entrada, que aqui
            // es un @MockitoBean y nunca lo evalua nadie; que los cinco puertos lo
            // lleven lo verifica ArchUnit.
            //
            // Lo que si es de esta rodaja: cuando en produccion ese gate rechace a un
            // principal que no es SYSTEM, al cliente le tiene que llegar un 403 y no un
            // 500. Sin este caso, un GlobalExceptionHandler que dejara de mapear
            // AccessDeniedException convertiria toda la consola en errores de servidor
            // sin que nada avisara.
            when(listUseCase.listAll(anyInt(), anyInt()))
                    .thenThrow(new AccessDeniedException("Access Denied"));

            mockMvc.perform(get("/system/company-activity-months"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("un mes que no existe sale 404 y no una pagina vacia")
        void un_mes_que_no_existe_sale_404() throws Exception {
            when(findUseCase.findByCompanyIdAndPeriodKey(any(), anyString()))
                    .thenThrow(new CompanyActivityMonthNotFoundException(EMPRESA, "2029-09"));

            mockMvc.perform(get("/system/company-activity-months/lookup").param("companyId", "900")
                    .param("periodKey", "2029-09")).andExpect(status().isNotFound());
        }
    }

    private static String cuerpoValido() {
        return """
                {
                  "periodKey": "2029-01",
                  "commercialState": "PAID",
                  "activeDays": 21,
                  "activeUsers": 7,
                  "recordsCreated": 143,
                  "mrrSnapshot": 189000.00
                }
                """;
    }

    private static CompanyActivityMonthDto dto() {
        return new CompanyActivityMonthDto(MES_ID, EMPRESA, "2029-01", CommercialState.PAID, 21, 7,
                143, new BigDecimal("189000.00"), LocalDateTime.of(2029, 2, 1, 3, 15, 0));
    }

    private static PageResult<CompanyActivityMonthDto> pagina() {
        return PageResult.of(List.of(dto()), 0, 20, 1L);
    }
}
