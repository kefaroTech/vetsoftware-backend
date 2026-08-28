package com.vetsoftware.app.companylimitevent.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.companylimitevent.application.command.AdjustCompanyUsageCommand;
import com.vetsoftware.app.companylimitevent.application.dto.CompanyLimitEventDto;
import com.vetsoftware.app.companylimitevent.application.dto.UsageReconciliationDto;
import com.vetsoftware.app.companylimitevent.application.port.in.AdjustCompanyUsageUseCase;
import com.vetsoftware.app.companylimitevent.application.port.in.ListCompanyLimitEventsUseCase;
import com.vetsoftware.app.companylimitevent.application.port.in.ReconcileCompanyUsageUseCase;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import com.vetsoftware.app.companylimitevent.infrastructure.web.request.AdjustCompanyUsageRequest;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.lang.reflect.RecordComponent;
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
 * Rodaja HTTP de la consola de plataforma sobre la bitácora de cupo.
 *
 * <p>
 * <strong>La invariante cara de esta rodaja es la firma.</strong> Quien corrige
 * un contador queda escrito con nombre, y ese nombre lo pone el servidor con
 * {@code authz.currentSystemUserId()}. El {@code Authz} del andamiaje devuelve
 * {@link WebMvcSliceConfig#SYSTEM_USER_ID}, distinto de {@code EMPLOYEE_ID}
 * justo para que la aserción pueda decir cuál de los dos actores firmó: con un
 * mock sin stub, Mockito devolvería {@code 0L} para un {@code Long} —no
 * {@code null}— y el test pasaría en verde con una corrección firmada por un
 * usuario de plataforma inexistente.
 */
@WebMvcTest(SystemCompanyLimitEventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemCompanyLimitEventController — contrato HTTP")
class SystemCompanyLimitEventControllerTest {

    private static final Long LA_CLINICA = 42L;
    private static final LocalDateTime DESDE = LocalDateTime.of(2026, 3, 1, 0, 0);
    private static final LocalDateTime HASTA = LocalDateTime.of(2026, 3, 31, 23, 59);

    /** Quinientas mascotas duplicadas por una migración: la corrección resta. */
    private static final String CUERPO_CORRECCION = """
            {"limitDimensionId":4,"capacityUnit":"ANIMALS","delta":-500,
             "reasonCode":"MIGRATION","reason":"Duplicados de la migracion del 12 de marzo"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListCompanyLimitEventsUseCase listUseCase;
    @MockitoBean
    private AdjustCompanyUsageUseCase adjustUseCase;
    @MockitoBean
    private ReconcileCompanyUsageUseCase reconcileUseCase;

    private static CompanyLimitEventDto correccion() {
        return new CompanyLimitEventDto(90L, LA_CLINICA, 4L, LimitEventType.USAGE_ADJUSTED, 100,
                541, -500, LimitSource.NONE, null, null, WebMvcSliceConfig.SYSTEM_USER_ID, false,
                "MIGRATION", "Duplicados de la migracion del 12 de marzo",
                LocalDateTime.of(2026, 3, 20, 9, 0));
    }

    @Nested
    @DisplayName("Correccion de consumo")
    class CorreccionDeConsumo {

        @Test
        @DisplayName("POST /usage-adjustments devuelve el hecho compensatorio, no un contador")
        void post_devuelve_el_hecho_compensatorio() throws Exception {
            when(adjustUseCase.execute(any())).thenReturn(correccion());

            mockMvc.perform(post("/system/company-limit-events/companies/42/usage-adjustments")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CORRECCION))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventType").value("USAGE_ADJUSTED"))
                    .andExpect(jsonPath("$.requestedDelta").value(-500))
                    .andExpect(jsonPath("$.reasonCode").value("MIGRATION"));
        }

        /**
         * <strong>La empresa sale de la ruta y la firma del principal.</strong> Ninguna
         * de las dos viaja en el cuerpo: un firmante que escribe el llamador no es una
         * firma, y el informe de correcciones seguiría enseñando un nombre —el que el
         * llamador quisiera—.
         */
        @Test
        @DisplayName("POST toma la empresa de la ruta y la firma del principal, no del cuerpo")
        void post_toma_la_empresa_y_la_firma_de_fuera_del_cuerpo() throws Exception {
            when(adjustUseCase.execute(any())).thenReturn(correccion());

            mockMvc.perform(post("/system/company-limit-events/companies/42/usage-adjustments")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CORRECCION))
                    .andExpect(status().isOk());

            ArgumentCaptor<AdjustCompanyUsageCommand> command = ArgumentCaptor
                    .forClass(AdjustCompanyUsageCommand.class);
            verify(adjustUseCase).execute(command.capture());
            assertThat(command.getValue().companyId()).isEqualTo(LA_CLINICA);
            assertThat(command.getValue().systemUserId())
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID);
            assertThat(command.getValue().delta()).isEqualTo(-500);
            assertThat(command.getValue().capacityUnit()).isEqualTo("ANIMALS");
        }

        /**
         * El caso principal de la válvula es <em>restar</em>. Si el binder acotara el
         * delta a positivos, la corrección de D-12 solo sabría inflar contadores.
         */
        @Test
        @DisplayName("un delta negativo es valido: corregir de mas es el caso principal")
        void delta_negativo_es_valido() throws Exception {
            when(adjustUseCase.execute(any())).thenReturn(correccion());

            mockMvc.perform(post("/system/company-limit-events/companies/42/usage-adjustments")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CORRECCION))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("POST sin motivo responde 400 y no corrige nada")
        void sin_motivo_responde_400() throws Exception {
            mockMvc.perform(post("/system/company-limit-events/companies/42/usage-adjustments")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"limitDimensionId":4,"capacityUnit":"ANIMALS","delta":-500}
                            """)).andExpect(status().isBadRequest());

            verify(adjustUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST con un motivo de mas de 255 caracteres responde 400")
        void motivo_demasiado_largo_responde_400() throws Exception {
            String largo = "x".repeat(256);
            mockMvc.perform(post("/system/company-limit-events/companies/42/usage-adjustments")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"limitDimensionId":4,"capacityUnit":"ANIMALS","delta":-500,
                             "reasonCode":"MIGRATION","reason":"%s"}
                            """.formatted(largo))).andExpect(status().isBadRequest());

            verify(adjustUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST sin unidad de capacidad responde 400")
        void sin_unidad_responde_400() throws Exception {
            mockMvc.perform(post("/system/company-limit-events/companies/42/usage-adjustments")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"limitDimensionId":4,"delta":-500,"reasonCode":"MIGRATION",
                             "reason":"Duplicados"}
                            """)).andExpect(status().isBadRequest());

            verify(adjustUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Recuento periodico")
    class RecuentoPeriodico {

        /**
         * Los cuatro números y el cursor son lo único que permite alertar sobre el
         * propio recuento: {@code drifted} creciendo es exactamente lo que R-LIMIT-30
         * existe para detectar.
         */
        @Test
        @DisplayName("POST /reconciliations devuelve los cuatro numeros y el cursor")
        void post_reconciliations_devuelve_el_cuadro() throws Exception {
            when(reconcileUseCase.execute(any(), anyLong(), anyInt()))
                    .thenReturn(new UsageReconciliationDto(200, 197, 2, 1, 4120L));

            mockMvc.perform(post("/system/company-limit-events/reconciliations")
                    .param("staleBefore", "2026-03-20T00:00:00")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.examined").value(200))
                    .andExpect(jsonPath("$.matched").value(197))
                    .andExpect(jsonPath("$.drifted").value(2))
                    .andExpect(jsonPath("$.skipped").value(1))
                    .andExpect(jsonPath("$.lastId").value(4120))
                    .andExpect(jsonPath("$.fullBatch").value(true));
        }

        /**
         * Un lote a medias dice «no hay más»: {@code fullBatch} es {@code false} y
         * quien barre puede parar. Sin ese dato, el recorrido no sabe cuándo termina.
         */
        @Test
        @DisplayName("un lote a medias marca fullBatch en false")
        void lote_a_medias_marca_full_batch_false() throws Exception {
            when(reconcileUseCase.execute(any(), anyLong(), anyInt()))
                    .thenReturn(new UsageReconciliationDto(13, 13, 0, 0, 4133L));

            mockMvc.perform(post("/system/company-limit-events/reconciliations")
                    .param("staleBefore", "2026-03-20T00:00:00")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullBatch").value(false));
        }

        @Test
        @DisplayName("el cursor y el tamano del lote llegan al caso de uso tal como se piden")
        void el_cursor_y_el_lote_llegan_al_caso_de_uso() throws Exception {
            when(reconcileUseCase.execute(any(), anyLong(), anyInt()))
                    .thenReturn(new UsageReconciliationDto(50, 50, 0, 0, 4200L));

            mockMvc.perform(post("/system/company-limit-events/reconciliations")
                    .param("staleBefore", "2026-03-20T00:00:00").param("afterId", "4100")
                    .param("batchSize", "50")).andExpect(status().isOk());

            verify(reconcileUseCase).execute(LocalDateTime.of(2026, 3, 20, 0, 0), 4100L, 50);
        }
    }

    @Nested
    @DisplayName("Lectura de plataforma")
    class LecturaDePlataforma {

        @Test
        @DisplayName("GET /companies/{id} lee la bitacora de la empresa de la ruta")
        void get_lee_la_empresa_de_la_ruta() throws Exception {
            when(listUseCase.listByCompanyId(LA_CLINICA, DESDE, HASTA))
                    .thenReturn(List.of(correccion()));

            mockMvc.perform(get("/system/company-limit-events/companies/42")
                    .param("from", "2026-03-01T00:00:00").param("to", "2026-03-31T23:59:00"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

            verify(listUseCase).listByCompanyId(LA_CLINICA, DESDE, HASTA);
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        /**
         * <strong>Es la advertencia literal de la ficha de construcción.</strong> Si la
         * corrección de consumo aterrizara en un puerto cuya autorización ya admite al
         * cliente, la administradora recuperaría su cupo cada vez que topa y el cupo
         * dejaría de existir sin que ninguna fila estuviera mal.
         */
        @Test
        @DisplayName("la correccion de consumo exige SYSTEM a secas, sin la mitad del tenant")
        void la_correccion_exige_system_a_secas() throws Exception {
            assertThat(AdjustCompanyUsageUseCase.class
                    .getMethod("execute", AdjustCompanyUsageCommand.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
        }

        @Test
        @DisplayName("el recuento exige SYSTEM a secas: recorre los contadores de todas")
        void el_recuento_exige_system_a_secas() throws Exception {
            assertThat(ReconcileCompanyUsageUseCase.class
                    .getMethod("execute", LocalDateTime.class, long.class, int.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
        }

        @Test
        @DisplayName("el request de correccion no declara companyId ni el usuario que firma")
        void el_request_no_declara_empresa_ni_firma() {
            assertThat(AdjustCompanyUsageRequest.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .doesNotContain("companyId", "systemUserId");
        }
    }
}
