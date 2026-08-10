package com.vetsoftware.app.product.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
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

import com.vetsoftware.app.product.application.command.CreateProductCommand;
import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.command.UpdateProductCommand;
import com.vetsoftware.app.product.application.dto.CompanySummaryDto;
import com.vetsoftware.app.product.application.dto.PageResult;
import com.vetsoftware.app.product.application.dto.ProductCategorySummaryDto;
import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.dto.SupplierSummaryDto;
import com.vetsoftware.app.product.application.dto.TaxSummaryDto;
import com.vetsoftware.app.product.application.port.in.CreateProductUseCase;
import com.vetsoftware.app.product.application.port.in.DeleteProductUseCase;
import com.vetsoftware.app.product.application.port.in.FindProductUseCase;
import com.vetsoftware.app.product.application.port.in.ListProductsUseCase;
import com.vetsoftware.app.product.application.port.in.ReactivateProductUseCase;
import com.vetsoftware.app.product.application.port.in.SearchProductsUseCase;
import com.vetsoftware.app.product.application.port.in.UpdateProductUseCase;
import com.vetsoftware.app.product.domain.ProductCodeAlreadyExistsException;
import com.vetsoftware.app.product.domain.ProductNameAlreadyExistsException;
import com.vetsoftware.app.product.domain.ProductNotFoundException;
import com.vetsoftware.app.product.domain.TaxTreatment;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
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
 * Rodaja HTTP del controller de productos: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 */
@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ProductController — contrato HTTP")
class ProductControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;

    private static final String CUERPO_VALIDO = """
            {"name":"Concentrado adulto","code":"P-001","salePrice":12500.00,
             "baseUnitMeasureCode":"94","provider":"Proveedor texto","supplierId":6,
             "notes":"Bulto de 15 kg","taxTreatment":"GRAVADO","productCategoryId":3,"taxId":4}
            """;

    private static final String CUERPO_VALIDO_UPDATE = """
            {"name":"Concentrado adulto","code":"P-001","salePrice":12500.00,
             "baseUnitMeasureCode":"94","provider":"Proveedor texto","supplierId":6,
             "notes":"Bulto de 15 kg","taxTreatment":"GRAVADO","productCategoryId":3,"taxId":4,
             "version":2}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateProductUseCase createUseCase;
    @MockitoBean
    private UpdateProductUseCase updateUseCase;
    @MockitoBean
    private FindProductUseCase findUseCase;
    @MockitoBean
    private ListProductsUseCase listUseCase;
    @MockitoBean
    private SearchProductsUseCase searchUseCase;
    @MockitoBean
    private DeleteProductUseCase deleteUseCase;
    @MockitoBean
    private ReactivateProductUseCase reactivateUseCase;

    private static ProductDto concentrado() {
        return new ProductDto(55L, "Concentrado adulto", "P-001", new BigDecimal("12500.00"), "94",
                "Proveedor texto", new SupplierSummaryDto(6L, "Distribuidora Sur"),
                TaxTreatment.GRAVADO, "Bulto de 15 kg",
                new ProductCategorySummaryDto(3L, "Alimentos"),
                new TaxSummaryDto(4L, "IVA 19%", new BigDecimal("19.00")),
                new CompanySummaryDto(COMPANY_ID, "Clinica Norte", "NIT-900"),
                LocalDateTime.of(2026, 1, 15, 10, 30), null, null, 2L, true);
    }

    private static ProductDto sinImpuestoNiProveedor() {
        return new ProductDto(56L, "Vacuna social", "P-002", new BigDecimal("0.00"), "94", null,
                null, TaxTreatment.EXCLUIDO, null, new ProductCategorySummaryDto(3L, "Alimentos"),
                null, new CompanySummaryDto(COMPANY_ID, "Clinica Norte", "NIT-900"),
                LocalDateTime.of(2026, 1, 15, 10, 30), null, null, 0L, true);
    }

    @Nested
    @DisplayName("POST /products")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el recurso creado")
        void responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(concentrado());

            mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.name").value("Concentrado adulto"))
                    .andExpect(jsonPath("$.code").value("P-001"))
                    .andExpect(jsonPath("$.taxTreatment").value("GRAVADO"))
                    .andExpect(jsonPath("$.productCategory.name").value("Alimentos"))
                    .andExpect(jsonPath("$.tax.percentage").value(19.00))
                    .andExpect(jsonPath("$.supplier.name").value("Distribuidora Sur"))
                    .andExpect(jsonPath("$.company.identifier").value("NIT-900"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la empresa del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(concentrado());

            mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(createUseCase).execute(new CreateProductCommand("Concentrado adulto", "P-001",
                    new BigDecimal("12500.00"), "94", "Proveedor texto", 6L, "Bulto de 15 kg",
                    TaxTreatment.GRAVADO, 3L, 4L, COMPANY_ID));
        }

        @Test
        @DisplayName("sin unidad base el controller aplica el codigo 94 por defecto")
        void aplica_la_unidad_94_por_defecto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(concentrado());

            mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Concentrado adulto","code":"P-001","salePrice":12500.00,
                     "taxTreatment":"GRAVADO","productCategoryId":3}
                    """));

            verify(createUseCase).execute(new CreateProductCommand("Concentrado adulto", "P-001",
                    new BigDecimal("12500.00"), "94", null, null, null, TaxTreatment.GRAVADO, 3L,
                    null, COMPANY_ID));
        }

        @Test
        @DisplayName("con unidad base en blanco tambien aplica el 94")
        void unidad_en_blanco_aplica_el_94() throws Exception {
            when(createUseCase.execute(any())).thenReturn(concentrado());

            mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Concentrado adulto","code":"P-001","salePrice":12500.00,
                     "baseUnitMeasureCode":"   ","taxTreatment":"GRAVADO",
                     "productCategoryId":3}
                    """));

            verify(createUseCase).execute(new CreateProductCommand("Concentrado adulto", "P-001",
                    new BigDecimal("12500.00"), "94", null, null, null, TaxTreatment.GRAVADO, 3L,
                    null, COMPANY_ID));
        }

        @Test
        @DisplayName("nombre vacio responde 400 y no llega al caso de uso")
        void nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"","code":"P-001","salePrice":12500.00,
                     "taxTreatment":"GRAVADO","productCategoryId":3}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("precio negativo responde 400")
        void precio_negativo_responde_400() throws Exception {
            mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Concentrado","code":"P-001","salePrice":-1,
                     "taxTreatment":"GRAVADO","productCategoryId":3}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin tratamiento fiscal ni categoria responde 400")
        void sin_tratamiento_ni_categoria_responde_400() throws Exception {
            mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Concentrado","code":"P-001","salePrice":10}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("codigo repetido responde 409")
        void codigo_repetido_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new ProductCodeAlreadyExistsException("P-001"));

            mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("nombre repetido responde 409")
        void nombre_repetido_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new ProductNameAlreadyExistsException("Concentrado adulto"));

            mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("una referencia inexistente del dominio sale como 400, no 500")
        void referencia_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Tax not found: 4"));

            mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /products lista los productos de la empresa del contexto")
        void get_lista() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID)).thenReturn(List.of(concentrado()));

            mockMvc.perform(get("/products")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(55))
                    .andExpect(jsonPath("$[0].company.id").value(COMPANY_ID));
        }

        @Test
        @DisplayName("GET /products/disabled devuelve el listado de pausados")
        void get_pausados() throws Exception {
            when(listUseCase.listDisabledByCompany(COMPANY_ID))
                    .thenReturn(List.of(sinImpuestoNiProveedor()));

            mockMvc.perform(get("/products/disabled")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(56))
                    .andExpect(jsonPath("$[0].tax").doesNotExist())
                    .andExpect(jsonPath("$[0].supplier").doesNotExist());
        }

        @Test
        @DisplayName("GET /products/search devuelve la envoltura paginada")
        void get_search() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(concentrado()), 1, 5, 11L, 3));

            mockMvc.perform(get("/products/search").param("name", "concentrado").param("page", "1")
                    .param("pageSize", "5")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(55))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("GET /products/search sin parametros usa pagina 0 y tamano 20")
        void get_search_por_defecto() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/products/search")).andExpect(status().isOk());

            verify(searchUseCase)
                    .execute(new SearchProductsCommand(COMPANY_ID, null, null, null, null, 0, 20));
        }

        @Test
        @DisplayName("GET /products/search traslada todos los filtros al command")
        void get_search_traslada_los_filtros() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/products/search").param("name", "conc").param("code", "P-0")
                    .param("productCategoryId", "3").param("taxId", "4"))
                    .andExpect(status().isOk());

            verify(searchUseCase)
                    .execute(new SearchProductsCommand(COMPANY_ID, "conc", "P-0", 3L, 4L, 0, 20));
        }

        @Test
        @DisplayName("GET /products/{id} devuelve el recurso")
        void get_por_id() throws Exception {
            when(findUseCase.findById(55L, COMPANY_ID)).thenReturn(concentrado());

            mockMvc.perform(get("/products/55")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.baseUnitMeasureCode").value("94"))
                    .andExpect(jsonPath("$.version").value(2));
        }

        @Test
        @DisplayName("GET /products/{id} inexistente responde 404, no 500")
        void get_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new ProductNotFoundException(99L));

            mockMvc.perform(get("/products/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre un producto existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /products/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(concentrado());

            mockMvc.perform(put("/products/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55));
        }

        @Test
        @DisplayName("PUT /products/{id} traduce el request al command con el id de la ruta")
        void put_traduce_el_request() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(concentrado());

            mockMvc.perform(put("/products/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE));

            verify(updateUseCase).execute(new UpdateProductCommand(55L, "Concentrado adulto",
                    "P-001", new BigDecimal("12500.00"), "94", "Proveedor texto", 6L,
                    "Bulto de 15 kg", TaxTreatment.GRAVADO, 3L, 4L, COMPANY_ID, EMPLOYEE_ID, 2L));
        }

        @Test
        @DisplayName("PUT sin version responde 400")
        void put_sin_version_responde_400() throws Exception {
            mockMvc.perform(put("/products/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PUT sobre un producto de otra empresa responde 404")
        void put_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new ProductNotFoundException(55L));

            mockMvc.perform(put("/products/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /products/{id} responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/products/55")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(55L, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de un producto inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new ProductNotFoundException(99L)).when(deleteUseCase)
                    .execute(99L, COMPANY_ID);

            mockMvc.perform(delete("/products/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PATCH /products/{id}/enable reactiva y responde 200")
        void patch_enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(55L, COMPANY_ID)).thenReturn(concentrado());

            mockMvc.perform(patch("/products/55/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("PATCH enable de un producto inexistente responde 404")
        void patch_enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L, COMPANY_ID))
                    .thenThrow(new ProductNotFoundException(99L));

            mockMvc.perform(patch("/products/99/enable")).andExpect(status().isNotFound());
        }
    }
}
