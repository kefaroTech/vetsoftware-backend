package com.vetsoftware.app.subscriptionitemlimit.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.subscriptionitemlimit.application.command.FreezeSubscriptionItemLimitCommand;
import com.vetsoftware.app.subscriptionitemlimit.application.command.PropagateCatalogLimitImprovementCommand;
import com.vetsoftware.app.subscriptionitemlimit.application.dto.SubscriptionItemLimitDto;
import com.vetsoftware.app.subscriptionitemlimit.application.port.in.FreezeSubscriptionItemLimitUseCase;
import com.vetsoftware.app.subscriptionitemlimit.application.port.in.ListSubscriptionItemLimitsUseCase;
import com.vetsoftware.app.subscriptionitemlimit.application.port.in.PropagateCatalogLimitImprovementUseCase;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitMode;
import com.vetsoftware.app.subscriptionitemlimit.domain.MeasureKind;
import com.vetsoftware.app.subscriptionitemlimit.infrastructure.web.request.FreezeSubscriptionItemLimitRequest;
import com.vetsoftware.app.subscriptionitemlimit.infrastructure.web.request.PropagateCatalogLimitImprovementRequest;
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
 * Rodaja HTTP de la consola de plataforma sobre los techos congelados.
 *
 * <p>
 * Lo que esta rodaja tiene que dejar clavado es <strong>por dónde entra la
 * empresa</strong>: por la ruta y solo por la ruta. Es la salida que
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} deja escrita para un gate de
 * plataforma, y la diferencia con un {@code companyId} en el JSON no es de
 * estilo — con el campo en el cuerpo, cualquier validación de propiedad se
 * convierte en una comparación del número consigo mismo.
 */
@WebMvcTest(SystemSubscriptionItemLimitController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemSubscriptionItemLimitController — contrato HTTP")
class SystemSubscriptionItemLimitControllerTest {

    private static final String CUERPO_VALIDO = """
            {"subscriptionItemId":55,"limitDimensionId":4,"measureKind":"CUMULATIVE",
             "mode":"LIMITED","limitQuantity":100,"enforcement":"BLOCK","warnThreshold":80}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FreezeSubscriptionItemLimitUseCase freezeUseCase;
    @MockitoBean
    private PropagateCatalogLimitImprovementUseCase propagateUseCase;
    @MockitoBean
    private ListSubscriptionItemLimitsUseCase listUseCase;

    private static SubscriptionItemLimitDto techoDe(Long companyId) {
        return new SubscriptionItemLimitDto(21L, companyId, 55L, 4L, MeasureKind.CUMULATIVE,
                LimitMode.LIMITED, 100, null, LimitEnforcement.BLOCK, null, 80,
                LocalDateTime.of(2026, 8, 27, 9, 0));
    }

    @Nested
    @DisplayName("Congelacion")
    class Congelacion {

        @Test
        @DisplayName("POST /companies/{id} responde 201 con el techo congelado")
        void post_responde_201() throws Exception {
            when(freezeUseCase.execute(any())).thenReturn(techoDe(77L));

            mockMvc.perform(post("/system/subscription-item-limits/companies/77")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(21))
                    .andExpect(jsonPath("$.companyId").value(77))
                    .andExpect(jsonPath("$.limitQuantity").value(100))
                    .andExpect(jsonPath("$.warnThreshold").value(80));
        }

        @Test
        @DisplayName("POST toma la empresa de la ruta, nunca del cuerpo")
        void post_toma_la_empresa_de_la_ruta() throws Exception {
            when(freezeUseCase.execute(any())).thenReturn(techoDe(77L));

            mockMvc.perform(post("/system/subscription-item-limits/companies/77")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO));

            ArgumentCaptor<FreezeSubscriptionItemLimitCommand> command = ArgumentCaptor
                    .forClass(FreezeSubscriptionItemLimitCommand.class);
            verify(freezeUseCase).execute(command.capture());
            assertThat(command.getValue().companyId()).isEqualTo(77L);
            assertThat(command.getValue().subscriptionItemId()).isEqualTo(55L);
            assertThat(command.getValue().measureKind()).isEqualTo(MeasureKind.CUMULATIVE);
            assertThat(command.getValue().enforcement()).isEqualTo(LimitEnforcement.BLOCK);
        }

        @Test
        @DisplayName("POST sin linea de contrato responde 400 y no congela nada")
        void post_sin_linea_responde_400() throws Exception {
            mockMvc.perform(post("/system/subscription-item-limits/companies/77")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"limitDimensionId":4,"measureKind":"STOCK","mode":"FULL",
                             "enforcement":"BLOCK","warnThreshold":80}
                            """)).andExpect(status().isBadRequest());

            verify(freezeUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST con porcentaje de aviso cero responde 400")
        void post_con_warn_threshold_cero_responde_400() throws Exception {
            mockMvc.perform(post("/system/subscription-item-limits/companies/77")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"subscriptionItemId":55,"limitDimensionId":4,"measureKind":"STOCK",
                             "mode":"FULL","enforcement":"BLOCK","warnThreshold":0}
                            """)).andExpect(status().isBadRequest());

            verify(freezeUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Propagacion de mejoras")
    class Propagacion {

        @Test
        @DisplayName("POST /propagations devuelve cuantos contratos mejoraron, envuelto en objeto")
        void post_propagations_devuelve_el_recuento() throws Exception {
            when(propagateUseCase.execute(any())).thenReturn(40);

            mockMvc.perform(post("/system/subscription-item-limits/propagations")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"catalogItemId":7,"limitDimensionId":4,"factoryMode":"LIMITED",
                             "factoryLimitQuantity":200}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.improvedContracts").value(40));

            ArgumentCaptor<PropagateCatalogLimitImprovementCommand> command = ArgumentCaptor
                    .forClass(PropagateCatalogLimitImprovementCommand.class);
            verify(propagateUseCase).execute(command.capture());
            assertThat(command.getValue().catalogItemId()).isEqualTo(7L);
            assertThat(command.getValue().factoryMode()).isEqualTo(LimitMode.LIMITED);
            assertThat(command.getValue().factoryLimitQuantity()).isEqualTo(200);
        }

        /**
         * Cero no es un error: bajar el cupo de fábrica no toca a ningún contrato vivo
         * (D-75). La respuesta tiene que ser un 200 con el recuento, no un 404 ni un
         * 409, porque la operación se ejecutó y su desenlace fue «ninguno mejoraba».
         */
        @Test
        @DisplayName("cero contratos mejorados responde 200, no un error")
        void cero_mejorados_responde_200() throws Exception {
            when(propagateUseCase.execute(any())).thenReturn(0);

            mockMvc.perform(post("/system/subscription-item-limits/propagations")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"catalogItemId":7,"limitDimensionId":4,"factoryMode":"LIMITED",
                             "factoryLimitQuantity":80}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.improvedContracts").value(0));
        }

        @Test
        @DisplayName("POST /propagations sin articulo responde 400")
        void post_propagations_sin_articulo_responde_400() throws Exception {
            mockMvc.perform(post("/system/subscription-item-limits/propagations")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"limitDimensionId":4,"factoryMode":"FULL"}
                            """)).andExpect(status().isBadRequest());

            verify(propagateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Lectura de plataforma")
    class LecturaDePlataforma {

        @Test
        @DisplayName("GET /companies/{id} lista los techos de la empresa que pide la ruta")
        void get_por_empresa() throws Exception {
            when(listUseCase.listByCompanyId(77L)).thenReturn(List.of(techoDe(77L)));

            mockMvc.perform(get("/system/subscription-item-limits/companies/77"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].companyId").value(77));

            verify(listUseCase).listByCompanyId(77L);
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        /**
         * Congelar un techo y propagar una mejora son decisiones comerciales: el
         * cliente no se congela sus propios cupos. Si alguno de los dos gates admitiera
         * al empleado del tenant, la administradora podría congelarse el techo que
         * quisiera, y ArchUnit no lo vería —solo exige que la anotación exista—.
         */
        @Test
        @DisplayName("congelar y propagar exigen SYSTEM a secas")
        void congelar_y_propagar_exigen_system() throws Exception {
            assertThat(FreezeSubscriptionItemLimitUseCase.class
                    .getMethod("execute", FreezeSubscriptionItemLimitCommand.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
            assertThat(PropagateCatalogLimitImprovementUseCase.class
                    .getMethod("execute", PropagateCatalogLimitImprovementCommand.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
        }

        @Test
        @DisplayName("ningun request de esta rodaja declara companyId: la empresa va en la ruta")
        void ningun_request_declara_company_id() {
            assertThat(FreezeSubscriptionItemLimitRequest.class.getRecordComponents())
                    .extracting(RecordComponent::getName).doesNotContain("companyId");
            assertThat(PropagateCatalogLimitImprovementRequest.class.getRecordComponents())
                    .extracting(RecordComponent::getName).doesNotContain("companyId");
        }
    }
}
