package com.vetsoftware.app.cashterminal.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.cashterminal.application.dto.CashTerminalDto;
import com.vetsoftware.app.cashterminal.application.usecase.CashTerminalService;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de las terminales de caja: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON.
 *
 * <p>
 * La terminal es la unidad de unicidad de la caja —solo puede haber una sesion
 * OPEN por (empresa, sede, terminal)—, asi que lo que se protege aqui es que
 * <b>ni la empresa ni la sede lleguen del cliente</b>: la empresa sale de
 * {@code Authz.currentCompanyId()} y la sede pasa siempre por
 * {@code resolveAccessibleBranch}. El doble devuelve una sede distinta de la
 * pedida a proposito: si el controller pasara la del cuerpo tal cual, se podria
 * crear una terminal en la sede de otro.
 *
 * <p>
 * Se dobla {@code CashTerminalService} —una clase, no un {@code port/in}—
 * porque es lo que el controller inyecta hoy; la rodaja no puede inventarse un
 * puerto que no existe.
 */
@WebMvcTest(CashTerminalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CashTerminalController — contrato HTTP")
class CashTerminalControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    /** Sede que resuelve {@code Authz}; distinta de la que manda el request. */
    private static final Long SEDE_RESUELTA = 31L;
    private static final long SEDE_PEDIDA = 77L;
    private static final Long TERMINAL_ID = 100L;

    private static final String CUERPO_VALIDO = """
            {"branchId":77,"name":"Caja principal","code":"CAJA-1"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CashTerminalService service;

    /**
     * Sin este stub {@code resolveAccessibleBranch} devolveria 0L —el default de
     * Mockito para un {@code Long}, no null— y la terminal se crearia en una sede
     * inexistente sin que el test lo notara.
     */
    @BeforeEach
    void resolverLaSedeDesdeElContexto() {
        when(authz.resolveAccessibleBranch(any())).thenReturn(SEDE_RESUELTA);
    }

    private static CashTerminalDto principal() {
        return new CashTerminalDto(TERMINAL_ID, SEDE_RESUELTA, "Caja principal", "CAJA-1", true,
                LocalDateTime.of(2026, 1, 15, 10, 30));
    }

    private static CashTerminalDto inactiva() {
        return new CashTerminalDto(101L, SEDE_RESUELTA, "Caja 2", "CAJA-2", false,
                LocalDateTime.of(2026, 1, 15, 10, 30));
    }

    @Nested
    @DisplayName("GET /cash-terminals")
    class Listado {

        @Test
        @DisplayName("lista las terminales de la sede resuelta por el contexto")
        void lista_las_terminales_de_la_sede_resuelta() throws Exception {
            when(service.list(COMPANY_ID, SEDE_RESUELTA, false))
                    .thenReturn(List.of(principal(), inactiva()));

            // El stub esta atado a (COMPANY_ID, SEDE_RESUELTA): si el controller usara la
            // sede del query param, el doble no responderia y la lista saldria vacia.
            mockMvc.perform(get("/cash-terminals").param("branchId", String.valueOf(SEDE_PEDIDA)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(TERMINAL_ID))
                    .andExpect(jsonPath("$[0].branchId").value(SEDE_RESUELTA))
                    .andExpect(jsonPath("$[0].code").value("CAJA-1"))
                    .andExpect(jsonPath("$[0].active").value(true))
                    .andExpect(jsonPath("$[1].active").value(false));
        }

        @Test
        @DisplayName("activeOnly=true se traslada al servicio")
        void active_only_se_traslada() throws Exception {
            when(service.list(COMPANY_ID, SEDE_RESUELTA, true)).thenReturn(List.of(principal()));

            mockMvc.perform(get("/cash-terminals").param("branchId", String.valueOf(SEDE_PEDIDA))
                    .param("activeOnly", "true")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("sin branchId responde 400: el POS no puede pedir todas las sedes")
        void sin_branch_id_responde_400() throws Exception {
            mockMvc.perform(get("/cash-terminals")).andExpect(status().isBadRequest());

            verify(service, never()).list(anyLong(), anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("una sede inactiva o ajena responde 400, no 500")
        void sede_invalida_responde_400() throws Exception {
            when(service.list(COMPANY_ID, SEDE_RESUELTA, false))
                    .thenThrow(new IllegalArgumentException("Sede no válida o inactiva: 31"));

            mockMvc.perform(get("/cash-terminals").param("branchId", String.valueOf(SEDE_PEDIDA)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /cash-terminals")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la terminal creada")
        void responde_201() throws Exception {
            when(service.create(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(principal());

            mockMvc.perform(post("/cash-terminals").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(TERMINAL_ID))
                    .andExpect(jsonPath("$.name").value("Caja principal"))
                    .andExpect(jsonPath("$.code").value("CAJA-1"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("la empresa y la sede las pone el backend, no el request")
        void empresa_y_sede_las_pone_el_backend() throws Exception {
            when(service.create(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(principal());

            mockMvc.perform(post("/cash-terminals").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(service).create(COMPANY_ID, SEDE_RESUELTA, "Caja principal", "CAJA-1");
        }

        @Test
        @DisplayName("sin sede responde 400 y no crea nada")
        void sin_sede_responde_400() throws Exception {
            mockMvc.perform(
                    post("/cash-terminals").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Caja principal","code":"CAJA-1"}
                            """)).andExpect(status().isBadRequest());

            verify(service, never()).create(anyLong(), anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("con el codigo en blanco responde 400")
        void codigo_en_blanco_responde_400() throws Exception {
            mockMvc.perform(
                    post("/cash-terminals").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":77,"name":"Caja principal","code":"   "}
                            """)).andExpect(status().isBadRequest());

            verify(service, never()).create(anyLong(), anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("con un nombre de mas de 120 caracteres responde 400")
        void nombre_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/cash-terminals").contentType(MediaType.APPLICATION_JSON).content(
                    "{\"branchId\":77,\"name\":\"" + "C".repeat(121) + "\",\"code\":\"CAJA-1\"}"))
                    .andExpect(status().isBadRequest());

            verify(service, never()).create(anyLong(), anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("un codigo repetido en la sede responde 400, no 500")
        void codigo_repetido_responde_400() throws Exception {
            when(service.create(anyLong(), anyLong(), anyString(), anyString()))
                    .thenThrow(new IllegalArgumentException(
                            "Ya existe un terminal con ese código en la sede"));

            mockMvc.perform(post("/cash-terminals").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /cash-terminals/{id}")
    class Actualizacion {

        @Test
        @DisplayName("responde 200 y arma la llamada con el id de la ruta y la empresa del contexto")
        void responde_200() throws Exception {
            when(service.update(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(principal());

            mockMvc.perform(
                    put("/cash-terminals/100").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Caja principal","code":"CAJA-1"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TERMINAL_ID));

            verify(service).update(COMPANY_ID, TERMINAL_ID, "Caja principal", "CAJA-1");
        }

        @Test
        @DisplayName("con el nombre vacio responde 400 y no actualiza")
        void nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(
                    put("/cash-terminals/100").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"","code":"CAJA-1"}
                            """)).andExpect(status().isBadRequest());

            verify(service, never()).update(anyLong(), anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("cambiar el codigo con una caja abierta responde 409, no 500")
        void cambiar_codigo_con_caja_abierta_responde_409() throws Exception {
            when(service.update(anyLong(), anyLong(), anyString(), anyString()))
                    .thenThrow(new IllegalStateException(
                            "No se puede cambiar el código de un terminal con una caja abierta"));

            mockMvc.perform(
                    put("/cash-terminals/100").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Caja principal","code":"CAJA-9"}
                            """)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("una terminal de otra empresa responde 400")
        void terminal_ajena_responde_400() throws Exception {
            when(service.update(anyLong(), anyLong(), anyString(), anyString()))
                    .thenThrow(new IllegalArgumentException("Terminal de caja no encontrado: 100"));

            mockMvc.perform(
                    put("/cash-terminals/100").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Caja principal","code":"CAJA-1"}
                            """)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("activar y desactivar")
    class Estado {

        @Test
        @DisplayName("DELETE /cash-terminals/{id} desactiva y devuelve 200 con la terminal")
        void delete_desactiva_y_responde_200() throws Exception {
            when(service.setActive(COMPANY_ID, TERMINAL_ID, false)).thenReturn(inactiva());

            // No es un 204: el endpoint es un soft delete que devuelve el recurso ya
            // desactivado para que el front no tenga que releerlo.
            mockMvc.perform(delete("/cash-terminals/100")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        @DisplayName("desactivar una terminal con la caja abierta responde 409")
        void desactivar_con_caja_abierta_responde_409() throws Exception {
            when(service.setActive(COMPANY_ID, TERMINAL_ID, false))
                    .thenThrow(new IllegalStateException(
                            "No se puede desactivar un terminal con una caja abierta"));

            mockMvc.perform(delete("/cash-terminals/100")).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("PATCH /cash-terminals/{id}/activate reactiva y responde 200")
        void patch_activate_responde_200() throws Exception {
            when(service.setActive(COMPANY_ID, TERMINAL_ID, true)).thenReturn(principal());

            mockMvc.perform(patch("/cash-terminals/100/activate")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true));
        }
    }
}
