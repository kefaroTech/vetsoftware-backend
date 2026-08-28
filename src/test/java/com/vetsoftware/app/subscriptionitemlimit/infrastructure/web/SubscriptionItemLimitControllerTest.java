package com.vetsoftware.app.subscriptionitemlimit.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.subscriptionitemlimit.application.dto.SubscriptionItemLimitDto;
import com.vetsoftware.app.subscriptionitemlimit.application.port.in.ListSubscriptionItemLimitsUseCase;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitMode;
import com.vetsoftware.app.subscriptionitemlimit.domain.MeasureKind;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Rodaja HTTP de lo que la clínica ve de sus propios techos congelados.
 *
 * <p>
 * <strong>Aquí sí se puede probar la mitad de tenancy que importa</strong>, y
 * no por el gate —que en esta rodaja no se ejecuta— sino por la superficie: la
 * empresa no es un dato de entrada por ninguna vía, así que un empleado de otra
 * empresa no tiene <em>dónde</em> escribir un {@code companyId} ajeno. Eso se
 * comprueba mandando la petición y afirmando que al caso de uso llega la
 * empresa del principal y solo esa.
 *
 * <p>
 * {@code WebMvcSliceConfig} entra por el andamiaje —los seis filtros de la
 * aplicación y el {@code GlobalExceptionHandler} real, sin el cual el 403 que
 * se afirma abajo sería un 500—, pero su {@code Authz} se
 * <strong>sustituye</strong> con un {@code @MockitoBean} propio: el del
 * andamiaje devuelve siempre la misma empresa y aquí hace falta decidir caso a
 * caso qué contesta {@code currentCompanyId()}, incluida la excepción del
 * principal que no es empleado de ninguna clínica.
 */
@WebMvcTest(SubscriptionItemLimitController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SubscriptionItemLimitController — contrato HTTP")
class SubscriptionItemLimitControllerTest {

    /** La empresa del principal. Distinta de la vecina de abajo, a propósito. */
    private static final Long MI_EMPRESA = 9L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListSubscriptionItemLimitsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    private static SubscriptionItemLimitDto techoDe(Long companyId) {
        return new SubscriptionItemLimitDto(21L, companyId, 55L, 4L, MeasureKind.CUMULATIVE,
                LimitMode.LIMITED, 100, null, LimitEnforcement.BLOCK, null, 80,
                LocalDateTime.of(2026, 8, 27, 9, 0));
    }

    @Nested
    @DisplayName("Lectura del tenant")
    class LecturaDelTenant {

        @Test
        @DisplayName("GET /subscription-item-limits devuelve los techos congelados de la clinica")
        void get_devuelve_los_techos() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA)).thenReturn(List.of(techoDe(MI_EMPRESA)));

            mockMvc.perform(get("/subscription-item-limits")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].companyId").value(9))
                    .andExpect(jsonPath("$[0].subscriptionItemId").value(55))
                    .andExpect(jsonPath("$[0].measureKind").value("CUMULATIVE"))
                    .andExpect(jsonPath("$[0].limitQuantity").value(100))
                    .andExpect(jsonPath("$[0].enforcement").value("BLOCK"));
        }

        @Test
        @DisplayName("una clinica sin techos congelados recibe una lista vacia, no un 404")
        void sin_techos_lista_vacia() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA)).thenReturn(List.of());

            mockMvc.perform(get("/subscription-item-limits")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * El corazón del aislamiento en esta rodaja: la empresa la pone el servidor. El
         * endpoint no acepta parámetro, ni ruta, ni cuerpo, así que un empleado de la
         * empresa 77 que llame aquí recibe los techos de la 77 —los suyos— y no tiene
         * ninguna vía para pedir los de la 9.
         */
        @Test
        @DisplayName("la empresa la pone el servidor: un parametro companyId en la URL se ignora")
        void la_empresa_la_pone_el_servidor() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA)).thenReturn(List.of());

            mockMvc.perform(get("/subscription-item-limits").param("companyId", "77")
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

            verify(listUseCase).listByCompanyId(MI_EMPRESA);
        }

        /**
         * Un principal que no es empleado de ninguna clínica —una cuenta de plataforma,
         * o un token sin contexto— no llega a este endpoint: {@code currentCompanyId()}
         * lanza y el manejador lo convierte en 403. <strong>El
         * {@code verifyNoInteractions} es la mitad del valor</strong>: un 403 después
         * de haber leído los datos no es un gate, es un mensaje de error.
         */
        @Test
        @DisplayName("un principal sin empresa responde 403 y no llega al caso de uso")
        void principal_sin_empresa_responde_403() throws Exception {
            when(authz.currentCompanyId())
                    .thenThrow(new AccessDeniedException("No employee context"));

            mockMvc.perform(get("/subscription-item-limits")).andExpect(status().isForbidden());

            verifyNoInteractions(listUseCase);
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        /**
         * El bloque es mixto —escribe plataforma, leen ambos—, así que este puerto es
         * el único de la feature que un tenant puede alcanzar, y su gate tiene que
         * llevar la revalidación del tenant además de la puerta de plataforma. Si
         * alguien dejara solo {@code hasRole('SYSTEM')}, la clínica perdería la
         * visibilidad de su propio tope, que es media razón de ser del plan con cupo.
         */
        @Test
        @DisplayName("el puerto admite SYSTEM o la propia empresa, y revalida el tenant")
        void el_puerto_admite_system_o_la_propia_empresa() throws Exception {
            String gate = ListSubscriptionItemLimitsUseCase.class
                    .getMethod("listByCompanyId", Long.class).getAnnotation(PreAuthorize.class)
                    .value();

            assertThat(gate).contains("hasRole('SYSTEM')")
                    .contains("@authz.isMyCompany(#companyId)");
        }

        /**
         * El SpEL de arriba nombra {@code #companyId}, y un {@code #} que no case con
         * el nombre real del parámetro resuelve a {@code null} en silencio:
         * {@code isMyCompany(null)} da {@code false} y la regla falla siempre. Esto ata
         * las dos mitades.
         */
        @Test
        @DisplayName("el parametro del puerto se llama companyId, que es lo que nombra el SpEL")
        void el_parametro_se_llama_company_id() throws Exception {
            assertThat(ListSubscriptionItemLimitsUseCase.class
                    .getMethod("listByCompanyId", Long.class).getParameters()[0].getName())
                    .isEqualTo("companyId");
        }

        /**
         * Ni ruta ni cuerpo: el controller del tenant no ofrece ninguna forma de
         * nombrar una empresa. Si mañana alguien colgara este controller de
         * {@code /companies/{companyId}}, el gate seguiría verde —admite la propia
         * empresa— pero la URL invitaría a probar la del vecino.
         */
        @Test
        @DisplayName("la ruta del controller del tenant no lleva companyId")
        void la_ruta_no_lleva_company_id() {
            assertThat(SubscriptionItemLimitController.class.getAnnotation(RequestMapping.class)
                    .value()).containsExactly("/subscription-item-limits");
        }
    }
}
