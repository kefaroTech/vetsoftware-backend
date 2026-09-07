package com.vetsoftware.app.paymentgateway.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("WompiWebhookBodySizeFilter — limite de tamano del webhook de Wompi")
class WompiWebhookBodySizeFilterTest {

    private static final String RUTA_WEBHOOK = "/payment-gateway/wompi/events";
    private static final long LIMITE = 100L;

    @Mock
    private FilterChain chain;

    private final WompiWebhookBodySizeFilter filter = new WompiWebhookBodySizeFilter(LIMITE);

    @Nested
    @DisplayName("shouldNotFilter")
    class Seleccion {

        @Test
        @DisplayName("no exime la ruta del webhook de Wompi")
        void no_exime_la_ruta_del_webhook() {
            assertThat(filter.shouldNotFilter(peticion(RUTA_WEBHOOK, null, null))).isFalse();
        }

        @Test
        @DisplayName("exime cualquier otra ruta: el filtro es exclusivo de ese endpoint")
        void exime_otras_rutas() throws Exception {
            MockHttpServletRequest request = peticion("/payment-gateway/wompi/sources", LIMITE + 1,
                    null);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("doFilterInternal")
    class Filtrado {

        @Test
        @DisplayName("un cuerpo por debajo del limite pasa a la cadena")
        void un_cuerpo_por_debajo_del_limite_pasa() throws Exception {
            MockHttpServletRequest request = peticion(RUTA_WEBHOOK, LIMITE - 1, null);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            verify(chain).doFilter(any(HttpServletRequest.class), eq(response));
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("un cuerpo igual al limite pasa a la cadena")
        void un_cuerpo_igual_al_limite_pasa() throws Exception {
            MockHttpServletRequest request = peticion(RUTA_WEBHOOK, LIMITE, null);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            verify(chain).doFilter(any(HttpServletRequest.class), eq(response));
        }

        @Test
        @DisplayName("un cuerpo que supera el limite se rechaza con 413 y no llega a la cadena")
        void un_cuerpo_que_supera_el_limite_se_rechaza() throws Exception {
            MockHttpServletRequest request = peticion(RUTA_WEBHOOK, LIMITE + 1, null);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(413);
            verifyNoInteractions(chain);
        }

        @Test
        @DisplayName("sin Content-Length declarado y cuerpo pequeno, pasa a la cadena")
        void sin_content_length_pasa_a_la_cadena() throws Exception {
            MockHttpServletRequest request = peticion(RUTA_WEBHOOK, null, null);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, chain);

            verify(chain).doFilter(any(HttpServletRequest.class), eq(response));
        }

        @Test
        @DisplayName("cuerpo chunked (sin Content-Length) que supera el limite se rechaza con 413 al leerlo")
        void cuerpo_chunked_que_supera_el_limite_se_rechaza() throws Exception {
            MockHttpServletRequest request = peticion(RUTA_WEBHOOK, null,
                    new byte[(int) (LIMITE + 1)]);
            MockHttpServletResponse response = new MockHttpServletResponse();
            doAnswer(inv -> {
                HttpServletRequest wrapped = inv.getArgument(0);
                wrapped.getInputStream().readAllBytes();
                return null;
            }).when(chain).doFilter(any(), any());

            filter.doFilterInternal(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(413);
        }

        @Test
        @DisplayName("cuerpo chunked (sin Content-Length) por debajo del limite se lee entero sin rechazarse")
        void cuerpo_chunked_por_debajo_del_limite_pasa() throws Exception {
            MockHttpServletRequest request = peticion(RUTA_WEBHOOK, null,
                    new byte[(int) (LIMITE - 1)]);
            MockHttpServletResponse response = new MockHttpServletResponse();
            doAnswer(inv -> {
                HttpServletRequest wrapped = inv.getArgument(0);
                wrapped.getInputStream().readAllBytes();
                return null;
            }).when(chain).doFilter(any(), any());

            filter.doFilterInternal(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    /**
     * {@code contentLength} nulo simula una peticion sin esa cabecera. Si ademas se
     * pasa {@code content}, la peticion lleva bytes reales -transferencia
     * {@code chunked}- pero {@code MockHttpServletRequest} deriva
     * {@code getContentLengthLong()} del propio array, asi que hay que
     * sobreescribirlo para que la longitud declarada siga siendo -1.
     */
    private static MockHttpServletRequest peticion(String servletPath, Long contentLength,
            byte[] content) {
        MockHttpServletRequest request = content == null
                ? new MockHttpServletRequest("POST", servletPath)
                : new MockHttpServletRequest("POST", servletPath) {

                    @Override
                    public long getContentLengthLong() {
                        return -1L;
                    }

                    @Override
                    public int getContentLength() {
                        return -1;
                    }
                };
        request.setServletPath(servletPath);
        if (contentLength != null) {
            request.setContent(new byte[contentLength.intValue()]);
        } else if (content != null) {
            request.setContent(content);
        }
        return request;
    }
}
