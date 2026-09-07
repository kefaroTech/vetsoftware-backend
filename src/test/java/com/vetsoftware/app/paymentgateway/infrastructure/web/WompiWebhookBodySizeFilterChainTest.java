package com.vetsoftware.app.paymentgateway.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.filter.AuthFilter;
import com.vetsoftware.app.auth.infrastructure.filter.LoginRateLimitFilter;
import com.vetsoftware.app.paymentgateway.application.port.in.ProcessWompiEventUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Rodaja que encadena {@link WompiWebhookBodySizeFilter} de verdad delante de
 * {@link WompiWebhookController}, a diferencia de
 * {@link WompiWebhookControllerTest} —que desactiva los filtros con
 * {@code addFilters = false} para aislar el contrato HTTP—. Aquí lo que se
 * comprueba es justo lo que ese aislamiento no puede ver: que un cuerpo por
 * encima del límite nunca llega al caso de uso, ni declarado por
 * {@code Content-Length} ni por transferencia sin ella.
 *
 * <p>
 * Sin la autoconfiguracion de seguridad de Boot: la ruta del webhook es publica
 * y lo que se prueba es el filtro de tamano encadenado con el controller, no la
 * cadena de seguridad ni el limite de tasa: con ellos montados, la rodaja
 * deniega o revienta contra un Valkey mockeado antes de llegar al filtro.
 */
@WebMvcTest(controllers = WompiWebhookController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                AuthFilter.class, LoginRateLimitFilter.class}))
@Import({WebMvcSliceConfig.class, WompiWebhookBodySizeFilter.class})
@TestPropertySource(properties = "vetsoftware.payments.wompi.max-event-body-bytes=20")
@DisplayName("WompiWebhookBodySizeFilter + WompiWebhookController — cadena real")
class WompiWebhookBodySizeFilterChainTest {

    private static final String RUTA = "/payment-gateway/wompi/events";
    private static final String CUERPO_GRANDE = "{\"event\":\"transaction.updated.demasiado.largo\"}";
    private static final String CUERPO_PEQUENO = "{\"e\":1}";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ProcessWompiEventUseCase processEventUseCase;

    @Test
    @DisplayName("Content-Length declarado por encima del limite: 413 y el caso de uso nunca se llama")
    void content_length_sobre_el_limite_responde_413_sin_llegar_al_caso_de_uso() throws Exception {
        mockMvc.perform(post(RUTA).servletPath(RUTA).contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_GRANDE)).andExpect(status().isPayloadTooLarge());

        verifyNoInteractions(processEventUseCase);
    }

    @Test
    @DisplayName("Content-Length declarado dentro del limite: llega al caso de uso y responde 200")
    void content_length_dentro_del_limite_llega_al_caso_de_uso() throws Exception {
        mockMvc.perform(post(RUTA).servletPath(RUTA).contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_PEQUENO)).andExpect(status().isOk());

        verify(processEventUseCase).execute(any());
    }

    @Test
    @DisplayName("cuerpo sin Content-Length (chunked) por encima del limite: 413 al leerlo, sin llegar al caso de uso")
    void cuerpo_chunked_sobre_el_limite_responde_413_sin_llegar_al_caso_de_uso() throws Exception {
        mockMvc.perform(post(RUTA).servletPath(RUTA).contentType(MediaType.APPLICATION_JSON)
                .with(sinContentLengthDeclarado(CUERPO_GRANDE)))
                .andExpect(status().isPayloadTooLarge());

        verifyNoInteractions(processEventUseCase);
    }

    @Test
    @DisplayName("cuerpo sin Content-Length (chunked) dentro del limite: llega al caso de uso y responde 200")
    void cuerpo_chunked_dentro_del_limite_llega_al_caso_de_uso() throws Exception {
        mockMvc.perform(post(RUTA).servletPath(RUTA).contentType(MediaType.APPLICATION_JSON)
                .with(sinContentLengthDeclarado(CUERPO_PEQUENO))).andExpect(status().isOk());

        verify(processEventUseCase).execute(any());
    }

    /**
     * {@code MockHttpServletRequest} deriva {@code getContentLengthLong()} del
     * array que recibe {@code setContent}, así que simular una transferencia sin
     * esa cabecera —el caso que fuerza al filtro a contar bytes mientras lee, en
     * vez de cortar de un vistazo— exige sobreescribir el getter en una subclase,
     * igual que en {@code WompiWebhookBodySizeFilterTest}.
     */
    private static RequestPostProcessor sinContentLengthDeclarado(String cuerpo) {
        return request -> {
            MockHttpServletRequest sinLongitud = new MockHttpServletRequest(
                    request.getServletContext()) {

                @Override
                public long getContentLengthLong() {
                    return -1L;
                }

                @Override
                public int getContentLength() {
                    return -1;
                }
            };
            sinLongitud.setMethod(request.getMethod());
            sinLongitud.setServletPath(request.getServletPath());
            sinLongitud.setRequestURI(request.getRequestURI());
            sinLongitud.setContentType(request.getContentType());
            for (String nombre : Collections.list(request.getHeaderNames())) {
                for (String valor : Collections.list(request.getHeaders(nombre))) {
                    sinLongitud.addHeader(nombre, valor);
                }
            }
            sinLongitud.setContent(cuerpo.getBytes(StandardCharsets.UTF_8));
            return sinLongitud;
        };
    }
}
