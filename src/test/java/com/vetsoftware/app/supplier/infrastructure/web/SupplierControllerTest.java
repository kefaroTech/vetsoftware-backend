package com.vetsoftware.app.supplier.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplier.application.command.CreateSupplierCommand;
import com.vetsoftware.app.supplier.application.command.SearchSuppliersCommand;
import com.vetsoftware.app.supplier.application.command.UpdateSupplierCommand;
import com.vetsoftware.app.supplier.application.dto.CompanySummaryDto;
import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.in.CreateSupplierUseCase;
import com.vetsoftware.app.supplier.application.port.in.DeleteSupplierUseCase;
import com.vetsoftware.app.supplier.application.port.in.FindSupplierUseCase;
import com.vetsoftware.app.supplier.application.port.in.ListSuppliersUseCase;
import com.vetsoftware.app.supplier.application.port.in.SearchSuppliersUseCase;
import com.vetsoftware.app.supplier.application.port.in.UpdateSupplierUseCase;
import com.vetsoftware.app.supplier.domain.SupplierNameAlreadyExistsException;
import com.vetsoftware.app.supplier.domain.SupplierNotFoundException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del controller de proveedores: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * La comprobacion que mas vale de todas es que el {@code companyId} del command
 * sale del contexto de autorizacion y no del cuerpo: la peticion no tiene donde
 * declararlo, y si algun dia lo tuviera este test lo delataria.
 */
@WebMvcTest(SupplierController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SupplierController — contrato HTTP")
class SupplierControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;

    private static final String CUERPO_VALIDO = """
            {"name":"Distribuidora Sur","taxId":"901555444-1","contactName":"Marta Gil",
             "phone":"3001234567","email":"compras@sur.test","address":"Calle 10 # 5-20",
             "paymentTermsDays":30,"notes":"Entrega los martes"}
            """;

    private static final String CUERPO_VALIDO_UPDATE = """
            {"name":"Distribuidora Sur","taxId":"901555444-1","contactName":"Marta Gil",
             "phone":"3001234567","email":"compras@sur.test","address":"Calle 10 # 5-20",
             "paymentTermsDays":30,"notes":"Entrega los martes","version":3}
            """;

    private static final String NOMBRE_DE_151_CARACTERES = "x".repeat(151);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSupplierUseCase createUseCase;
    @MockitoBean
    private UpdateSupplierUseCase updateUseCase;
    @MockitoBean
    private FindSupplierUseCase findUseCase;
    @MockitoBean
    private ListSuppliersUseCase listUseCase;
    @MockitoBean
    private SearchSuppliersUseCase searchUseCase;
    @MockitoBean
    private DeleteSupplierUseCase deleteUseCase;

    private static SupplierDto distribuidoraSur() {
        return new SupplierDto(55L, "Distribuidora Sur", "901555444-1", "Marta Gil", "3001234567",
                "compras@sur.test", "Calle 10 # 5-20", 30, "Entrega los martes",
                new CompanySummaryDto(COMPANY_ID, "Clinica Norte", "NIT-900"),
                LocalDateTime.of(2026, 1, 15, 10, 30), null, null, 3L, true);
    }

    private static CreateSupplierCommand comandoDeCreacionEsperado() {
        return new CreateSupplierCommand("Distribuidora Sur", "901555444-1", "Marta Gil",
                "3001234567", "compras@sur.test", "Calle 10 # 5-20", 30, "Entrega los martes",
                COMPANY_ID);
    }

    @Nested
    @DisplayName("POST /suppliers")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el recurso creado")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(distribuidoraSur());

            mockMvc.perform(post("/suppliers").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.name").value("Distribuidora Sur"))
                    .andExpect(jsonPath("$.taxId").value("901555444-1"))
                    .andExpect(jsonPath("$.contactName").value("Marta Gil"))
                    .andExpect(jsonPath("$.paymentTermsDays").value(30))
                    .andExpect(jsonPath("$.company.identifier").value("NIT-900"))
                    .andExpect(jsonPath("$.version").value(3))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la empresa del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(distribuidoraSur());

            mockMvc.perform(post("/suppliers").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(createUseCase).execute(comandoDeCreacionEsperado());
        }

        @Test
        @DisplayName("un companyId colado en el cuerpo no suplanta a la empresa del contexto")
        void un_company_id_colado_en_el_cuerpo_no_suplanta_al_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(distribuidoraSur());

            mockMvc.perform(post("/suppliers").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Distribuidora Sur","taxId":"901555444-1","contactName":"Marta Gil",
                     "phone":"3001234567","email":"compras@sur.test","address":"Calle 10 # 5-20",
                     "paymentTermsDays":30,"notes":"Entrega los martes","companyId":777}
                    """));

            // El request no declara companyId: lo pone Authz. Si alguien lo anadiera al
            // record, este command llevaria 777 y el proveedor caeria en otra veterinaria.
            verify(createUseCase).execute(comandoDeCreacionEsperado());
        }

        @Test
        @DisplayName("nombre vacio responde 400 y no llega al caso de uso")
        void nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(post("/suppliers").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"","taxId":"901555444-1"}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("nombre de mas de 150 caracteres responde 400")
        void nombre_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/suppliers").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + NOMBRE_DE_151_CARACTERES + "\"}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("plazo de pago negativo responde 400")
        void plazo_de_pago_negativo_responde_400() throws Exception {
            mockMvc.perform(post("/suppliers").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Distribuidora Sur","paymentTermsDays":-1}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("nombre repetido en la empresa responde 409")
        void nombre_repetido_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new SupplierNameAlreadyExistsException("Distribuidora Sur"));

            mockMvc.perform(post("/suppliers").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("una empresa inexistente sale como 400, no como 500")
        void empresa_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Company not found: 9"));

            mockMvc.perform(post("/suppliers").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /suppliers lista los proveedores de la empresa del contexto")
        void get_lista_los_de_la_empresa_del_contexto() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID)).thenReturn(List.of(distribuidoraSur()));

            mockMvc.perform(get("/suppliers")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(55))
                    .andExpect(jsonPath("$[0].company.id").value(COMPANY_ID));
        }

        @Test
        @DisplayName("GET /suppliers/search devuelve la envoltura paginada")
        void get_search_devuelve_la_envoltura_paginada() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(distribuidoraSur()), 1, 5, 11L, 3));

            mockMvc.perform(get("/suppliers/search").param("name", "sur").param("page", "1")
                    .param("pageSize", "5")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(55))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("GET /suppliers/search sin parametros usa pagina 0 y tamano 20")
        void get_search_sin_parametros_usa_los_valores_por_defecto() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/suppliers/search")).andExpect(status().isOk());

            verify(searchUseCase)
                    .execute(new SearchSuppliersCommand(COMPANY_ID, null, null, 0, 20));
        }

        @Test
        @DisplayName("GET /suppliers/search traslada los filtros y la empresa al command")
        void get_search_traslada_los_filtros() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/suppliers/search").param("name", "sur").param("taxId", "901")
                    .param("page", "2").param("pageSize", "50")).andExpect(status().isOk());

            verify(searchUseCase)
                    .execute(new SearchSuppliersCommand(COMPANY_ID, "sur", "901", 2, 50));
        }

        @Test
        @DisplayName("GET /suppliers/{id} devuelve el recurso")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(55L, COMPANY_ID)).thenReturn(distribuidoraSur());

            mockMvc.perform(get("/suppliers/55")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.email").value("compras@sur.test"))
                    .andExpect(jsonPath("$.version").value(3));
        }

        @Test
        @DisplayName("GET /suppliers/{id} de otra empresa responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new SupplierNotFoundException(99L));

            mockMvc.perform(get("/suppliers/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre un proveedor existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /suppliers/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(distribuidoraSur());

            mockMvc.perform(put("/suppliers/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta y el empleado")
        void put_traduce_el_request_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(distribuidoraSur());

            mockMvc.perform(put("/suppliers/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE));

            verify(updateUseCase).execute(new UpdateSupplierCommand(55L, "Distribuidora Sur",
                    "901555444-1", "Marta Gil", "3001234567", "compras@sur.test", "Calle 10 # 5-20",
                    30, "Entrega los martes", COMPANY_ID, EMPLOYEE_ID, 3L));
        }

        @Test
        @DisplayName("PUT sin version responde 400 y no llega al caso de uso")
        void put_sin_version_responde_400() throws Exception {
            mockMvc.perform(put("/suppliers/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PUT sobre un proveedor de otra empresa responde 404")
        void put_de_otra_empresa_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new SupplierNotFoundException(55L));

            mockMvc.perform(put("/suppliers/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT con un nombre ya usado en la empresa responde 409")
        void put_con_nombre_repetido_responde_409() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new SupplierNameAlreadyExistsException("Distribuidora Sur"));

            mockMvc.perform(put("/suppliers/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("DELETE /suppliers/{id} responde 204 sin cuerpo y pasa la empresa")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/suppliers/55")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(55L, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de un proveedor inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            doThrow(new SupplierNotFoundException(99L)).when(deleteUseCase).execute(99L,
                    COMPANY_ID);

            mockMvc.perform(delete("/suppliers/99")).andExpect(status().isNotFound());
        }
    }
}
