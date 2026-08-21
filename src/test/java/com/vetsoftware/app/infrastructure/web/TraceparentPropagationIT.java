package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.vetsoftware.app.testsupport.AbstractFullApplicationIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * TR-05: el navegador manda {@code traceparent} y este backend tiene que
 * adoptar ese {@code trace-id} en vez de generar uno propio.
 *
 * <p>
 * Es el contrato entre los dos frontends y este servicio, y hasta ahora no lo
 * comprobaba nadie: se daba por hecho que Micrometer habla W3C porque es el
 * valor por defecto de Boot. Si un cambio de configuracion —o un upgrade—
 * cambiara el formato de propagacion, el sintoma seria silencioso: cada lado
 * seguiria teniendo su identificador y nadie notaria que dejaron de ser el
 * mismo, hasta que soporte buscara una traza y no la encontrara.
 *
 * <p>
 * <b>Comparte contexto —y por tanto base de datos y Redis— con las demas
 * {@link AbstractFullApplicationIT}.</b> Es seguro porque estos tres casos no
 * escriben nada observable: el login con un codigo inexistente muere en el
 * {@code findByCode} de {@code LoginEmployeeService} y su
 * {@code @Transactional} revierte una transaccion vacia, y los cubos del
 * {@code LoginRateLimitFilter} llevan el prefijo {@code login-rl:}, que ninguna
 * otra ruta bajo prueba toca.
 */
class TraceparentPropagationIT extends AbstractFullApplicationIT {

    /**
     * Un trace-id valido: 32 hex, como el que genera el interceptor de los fronts.
     */
    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String TRACEPARENT = "00-" + TRACE_ID + "-00f067aa0ba902b7-01";

    @Autowired
    private MockMvc mockMvc;

    /**
     * Se usa el login con credenciales falsas a proposito: la ruta es publica —no
     * hace falta montar una sesion— y responde 401, que es justo el caso que
     * importa. Un fallo es cuando soporte necesita la traza.
     */
    @Test
    @DisplayName("adopta el trace-id que manda el navegador y lo devuelve en X-Trace-Id")
    void adopta_el_trace_id_del_navegador() throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/v1/auth/login/employee").contextPath("/api/v1")
                        .servletPath("/auth/login/employee").header("traceparent", TRACEPARENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeCode\":\"nadie\",\"password\":\"tampoco\"}"))
                .andReturn();

        assertThat(result.getResponse().getHeader("X-Trace-Id"))
                .as("el backend genero una traza nueva en vez de continuar la del navegador")
                .isEqualTo(TRACE_ID);
    }

    /**
     * El cuerpo del error tambien lo lleva. Es el respaldo que usan los fronts
     * cuando la cabecera no llega —por un proxy que la filtre, por ejemplo— y tiene
     * que coincidir con ella, no ser otra traza.
     */
    @Test
    @DisplayName("el traceId del ProblemDetail coincide con la cabecera")
    void el_cuerpo_lleva_el_mismo_identificador() throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/v1/auth/login/employee").contextPath("/api/v1")
                        .servletPath("/auth/login/employee").header("traceparent", TRACEPARENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeCode\":\"nadie\",\"password\":\"tampoco\"}"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(TRACE_ID);
    }

    /**
     * Sin cabecera el backend sigue generando la suya: no se rompe nada al no
     * mandarla.
     */
    @Test
    @DisplayName("sin traceparent genera su propia traza")
    void sin_cabecera_genera_la_suya() throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/v1/auth/login/employee").contextPath("/api/v1")
                        .servletPath("/auth/login/employee").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeCode\":\"nadie\",\"password\":\"tampoco\"}"))
                .andReturn();

        assertThat(result.getResponse().getHeader("X-Trace-Id")).isNotBlank()
                .isNotEqualTo(TRACE_ID);
    }
}
