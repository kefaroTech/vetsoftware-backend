package com.vetsoftware.app.petshopcatalog.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.vetsoftware.app.catalog.domain.SellableItemType;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BarcodeLookupDto;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleDto;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleItemDto;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleItemWrite;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleWrite;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.PresentationDto;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.PresentationWrite;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.UnitMeasureDto;
import com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogConflictException;
import com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
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
 * Rodaja HTTP del catalogo que consume el POS: presentaciones, combos y la
 * busqueda por codigo de barras.
 *
 * <p>
 * Dos cosas que solo se pueden romper aqui:
 *
 * <ul>
 * <li><b>El tenant y el sello de autoria.</b> Ninguna de las dos viaja en el
 * cuerpo; el controller las saca de {@code Authz}. Un combo creado con el
 * {@code companyId} equivocado se vende en la caja de otra clinica.</li>
 * <li><b>El {@code expectedVersion} del borrado.</b> Va como query param, no en
 * el cuerpo: si el controller dejara de trasladarlo, el borrado optimista se
 * volveria un borrado a ciegas.</li>
 * </ul>
 */
@WebMvcTest(PetshopCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PetshopCatalogController — contrato HTTP")
class PetshopCatalogControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;

    private static final Long PRODUCT_ID = 55L;
    private static final Long PRESENTATION_ID = 700L;
    private static final Long BUNDLE_ID = 800L;

    private static final String PRESENTACION_VALIDA = """
            {"productId":55,"name":"Bulto 15 kg","unitMeasureCode":"KGM","conversionFactor":15,
             "salePrice":120000.00,"defaultPresentation":true,"barcodes":["7701234567890"],
             "expectedVersion":2}
            """;

    private static final String COMBO_VALIDO = """
            {"name":"Combo cachorro","code":"CMB-1","unitMeasureCode":"NIU","salePrice":95000.00,
             "items":[{"presentationId":700,"quantity":2,"displayOrder":0}],
             "barcodes":["7709876543210"],"expectedVersion":3}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PetshopCatalogUseCase useCase;

    private static PresentationDto presentacion() {
        return new PresentationDto(PRESENTATION_ID, PRODUCT_ID, "Concentrado adulto", "Bulto 15 kg",
                "KGM", 15, new BigDecimal("120000.00"), true, List.of("7701234567890"), 2L);
    }

    private static BundleDto combo() {
        return new BundleDto(BUNDLE_ID, "Combo cachorro", "CMB-1", "NIU",
                new BigDecimal("95000.00"), List.of(new BundleItemDto(PRESENTATION_ID,
                        "Bulto 15 kg", "Concentrado adulto", 2, 0)),
                List.of("7709876543210"), 3L);
    }

    @Nested
    @DisplayName("presentaciones")
    class Presentaciones {

        @Test
        @DisplayName("GET lista las presentaciones del producto para la empresa del contexto")
        void get_lista() throws Exception {
            when(useCase.listPresentations(PRODUCT_ID, COMPANY_ID))
                    .thenReturn(List.of(presentacion()));

            mockMvc.perform(get("/petshop-catalog/presentations").param("productId", "55"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(PRESENTATION_ID))
                    .andExpect(jsonPath("$[0].conversionFactor").value(15))
                    .andExpect(jsonPath("$[0].defaultPresentation").value(true))
                    .andExpect(jsonPath("$[0].barcodes[0]").value("7701234567890"));
        }

        @Test
        @DisplayName("GET sin productId responde 400")
        void get_sin_producto_responde_400() throws Exception {
            mockMvc.perform(get("/petshop-catalog/presentations"))
                    .andExpect(status().isBadRequest());

            verify(useCase, never()).listPresentations(anyLong(), anyLong());
        }

        @Test
        @DisplayName("POST responde 201 con la presentacion creada")
        void post_responde_201() throws Exception {
            when(useCase.createPresentation(any(), anyLong(), any())).thenReturn(presentacion());

            mockMvc.perform(post("/petshop-catalog/presentations")
                    .contentType(MediaType.APPLICATION_JSON).content(PRESENTACION_VALIDA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(PRESENTATION_ID))
                    .andExpect(jsonPath("$.productName").value("Concentrado adulto"))
                    .andExpect(jsonPath("$.salePrice").value(120000.00))
                    .andExpect(jsonPath("$.version").value(2));
        }

        @Test
        @DisplayName("POST traduce el request al write y sella empresa y empleado del contexto")
        void post_traduce_el_request() throws Exception {
            when(useCase.createPresentation(any(), anyLong(), any())).thenReturn(presentacion());

            mockMvc.perform(post("/petshop-catalog/presentations")
                    .contentType(MediaType.APPLICATION_JSON).content(PRESENTACION_VALIDA));

            verify(useCase).createPresentation(
                    new PresentationWrite(PRODUCT_ID, "Bulto 15 kg", "KGM", 15,
                            new BigDecimal("120000.00"), true, List.of("7701234567890"), 2L),
                    COMPANY_ID, EMPLOYEE_ID);
        }

        @Test
        @DisplayName("POST con factor de conversion cero responde 400 y no llega al caso de uso")
        void post_con_factor_cero_responde_400() throws Exception {
            // Un factor 0 dividiria por cero al descontar inventario desde el POS.
            mockMvc.perform(post("/petshop-catalog/presentations")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"productId":55,"name":"Bulto","unitMeasureCode":"KGM",
                             "conversionFactor":0,"salePrice":1000.00,"defaultPresentation":false}
                            """)).andExpect(status().isBadRequest());

            verify(useCase, never()).createPresentation(any(), anyLong(), any());
        }

        @Test
        @DisplayName("POST con precio negativo responde 400")
        void post_con_precio_negativo_responde_400() throws Exception {
            mockMvc.perform(post("/petshop-catalog/presentations")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"productId":55,"name":"Bulto","unitMeasureCode":"KGM",
                             "conversionFactor":15,"salePrice":-1,"defaultPresentation":false}
                            """)).andExpect(status().isBadRequest());

            verify(useCase, never()).createPresentation(any(), anyLong(), any());
        }

        @Test
        @DisplayName("POST sin producto responde 400")
        void post_sin_producto_responde_400() throws Exception {
            mockMvc.perform(post("/petshop-catalog/presentations")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Bulto","unitMeasureCode":"KGM","conversionFactor":15,
                             "salePrice":1000.00,"defaultPresentation":false}
                            """)).andExpect(status().isBadRequest());

            verify(useCase, never()).createPresentation(any(), anyLong(), any());
        }

        @Test
        @DisplayName("POST con un codigo de barras en blanco responde 400")
        void post_con_barcode_en_blanco_responde_400() throws Exception {
            // La validacion va en el elemento de la lista: sin ella entraria un barcode
            // vacio que el lector del POS jamas resolveria.
            mockMvc.perform(post("/petshop-catalog/presentations")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"productId":55,"name":"Bulto","unitMeasureCode":"KGM",
                             "conversionFactor":15,"salePrice":1000.00,
                             "defaultPresentation":false,"barcodes":["  "]}
                            """)).andExpect(status().isBadRequest());

            verify(useCase, never()).createPresentation(any(), anyLong(), any());
        }

        @Test
        @DisplayName("POST con un codigo de barras ya usado responde 409, no 500")
        void post_con_barcode_repetido_responde_409() throws Exception {
            when(useCase.createPresentation(any(), anyLong(), any()))
                    .thenThrow(new PetshopCatalogConflictException("BARCODE_ALREADY_USED",
                            "El código de barras ya está asignado"));

            mockMvc.perform(post("/petshop-catalog/presentations")
                    .contentType(MediaType.APPLICATION_JSON).content(PRESENTACION_VALIDA))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("PUT responde 200 y arma el write con el id de la ruta")
        void put_responde_200() throws Exception {
            when(useCase.updatePresentation(anyLong(), any(), anyLong(), any()))
                    .thenReturn(presentacion());

            mockMvc.perform(put("/petshop-catalog/presentations/700")
                    .contentType(MediaType.APPLICATION_JSON).content(PRESENTACION_VALIDA))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(PRESENTATION_ID));

            verify(useCase).updatePresentation(PRESENTATION_ID,
                    new PresentationWrite(PRODUCT_ID, "Bulto 15 kg", "KGM", 15,
                            new BigDecimal("120000.00"), true, List.of("7701234567890"), 2L),
                    COMPANY_ID, EMPLOYEE_ID);
        }

        @Test
        @DisplayName("PUT de una presentacion inexistente responde 404")
        void put_inexistente_responde_404() throws Exception {
            when(useCase.updatePresentation(anyLong(), any(), anyLong(), any()))
                    .thenThrow(new PetshopCatalogNotFoundException("presentation", 700L));

            mockMvc.perform(put("/petshop-catalog/presentations/700")
                    .contentType(MediaType.APPLICATION_JSON).content(PRESENTACION_VALIDA))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE responde 204 y traslada el expectedVersion del query")
        void delete_responde_204() throws Exception {
            mockMvc.perform(
                    delete("/petshop-catalog/presentations/700").param("expectedVersion", "2"))
                    .andExpect(status().isNoContent());

            verify(useCase).deletePresentation(PRESENTATION_ID, 2L, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE sin expectedVersion responde 400 y no borra")
        void delete_sin_version_responde_400() throws Exception {
            mockMvc.perform(delete("/petshop-catalog/presentations/700"))
                    .andExpect(status().isBadRequest());

            verify(useCase, never()).deletePresentation(anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("DELETE con una version obsoleta responde 409")
        void delete_con_version_obsoleta_responde_409() throws Exception {
            doThrow(new PetshopCatalogConflictException("CONCURRENT_MODIFICATION",
                    "La presentación fue modificada por otra operación")).when(useCase)
                    .deletePresentation(PRESENTATION_ID, 1L, COMPANY_ID);

            mockMvc.perform(
                    delete("/petshop-catalog/presentations/700").param("expectedVersion", "1"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("combos")
    class Combos {

        @Test
        @DisplayName("GET lista los combos de la empresa del contexto con sus items")
        void get_lista() throws Exception {
            when(useCase.listBundles(COMPANY_ID)).thenReturn(List.of(combo()));

            mockMvc.perform(get("/petshop-catalog/bundles")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(BUNDLE_ID))
                    .andExpect(jsonPath("$[0].code").value("CMB-1"))
                    .andExpect(jsonPath("$[0].items[0].presentationId").value(PRESENTATION_ID))
                    .andExpect(jsonPath("$[0].items[0].quantity").value(2));
        }

        @Test
        @DisplayName("POST responde 201 y traduce items y autoria")
        void post_responde_201() throws Exception {
            when(useCase.createBundle(any(), anyLong(), any())).thenReturn(combo());

            mockMvc.perform(post("/petshop-catalog/bundles").contentType(MediaType.APPLICATION_JSON)
                    .content(COMBO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(BUNDLE_ID))
                    .andExpect(jsonPath("$.items[0].productName").value("Concentrado adulto"));

            verify(useCase).createBundle(new BundleWrite("Combo cachorro", "CMB-1", "NIU",
                    new BigDecimal("95000.00"), List.of(new BundleItemWrite(PRESENTATION_ID, 2, 0)),
                    List.of("7709876543210"), 3L), COMPANY_ID, EMPLOYEE_ID);
        }

        @Test
        @DisplayName("POST de un combo sin items responde 400: un combo vacio no se vende")
        void post_sin_items_responde_400() throws Exception {
            mockMvc.perform(post("/petshop-catalog/bundles").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name":"Combo","code":"CMB-1","unitMeasureCode":"NIU",
                             "salePrice":1000.00,"items":[]}
                            """)).andExpect(status().isBadRequest());

            verify(useCase, never()).createBundle(any(), anyLong(), any());
        }

        @Test
        @DisplayName("POST con un item de cantidad cero responde 400")
        void post_con_item_de_cantidad_cero_responde_400() throws Exception {
            // La cascada @Valid sobre los items es lo unico que impide un combo con
            // cantidad 0, que descontaria inventario en falso.
            mockMvc.perform(post("/petshop-catalog/bundles").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name":"Combo","code":"CMB-1","unitMeasureCode":"NIU",
                             "salePrice":1000.00,
                             "items":[{"presentationId":700,"quantity":0,"displayOrder":0}]}
                            """)).andExpect(status().isBadRequest());

            verify(useCase, never()).createBundle(any(), anyLong(), any());
        }

        @Test
        @DisplayName("POST con un codigo de combo repetido responde 409")
        void post_con_codigo_repetido_responde_409() throws Exception {
            when(useCase.createBundle(any(), anyLong(), any()))
                    .thenThrow(new PetshopCatalogConflictException("BUNDLE_CODE_ALREADY_USED",
                            "Ya existe un combo con ese código"));

            mockMvc.perform(post("/petshop-catalog/bundles").contentType(MediaType.APPLICATION_JSON)
                    .content(COMBO_VALIDO)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("PUT responde 200 con el id de la ruta")
        void put_responde_200() throws Exception {
            when(useCase.updateBundle(anyLong(), any(), anyLong(), any())).thenReturn(combo());

            mockMvc.perform(put("/petshop-catalog/bundles/800")
                    .contentType(MediaType.APPLICATION_JSON).content(COMBO_VALIDO))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(BUNDLE_ID));

            verify(useCase).updateBundle(org.mockito.ArgumentMatchers.eq(BUNDLE_ID), any(),
                    org.mockito.ArgumentMatchers.eq(COMPANY_ID),
                    org.mockito.ArgumentMatchers.eq(EMPLOYEE_ID));
        }

        @Test
        @DisplayName("DELETE responde 204 y traslada el expectedVersion")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/petshop-catalog/bundles/800").param("expectedVersion", "3"))
                    .andExpect(status().isNoContent());

            verify(useCase).deleteBundle(BUNDLE_ID, 3L, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de un combo inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            doThrow(new PetshopCatalogNotFoundException("bundle", 800L)).when(useCase)
                    .deleteBundle(BUNDLE_ID, 3L, COMPANY_ID);

            mockMvc.perform(delete("/petshop-catalog/bundles/800").param("expectedVersion", "3"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("lecturas que consume el POS")
    class LecturasDelPos {

        @Test
        @DisplayName("GET /unit-measures devuelve el catalogo global, sin empresa")
        void get_unidades() throws Exception {
            when(useCase.listUnitMeasures())
                    .thenReturn(List.of(new UnitMeasureDto("KGM", "Kilogramo", "kg")));

            mockMvc.perform(get("/petshop-catalog/unit-measures")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].code").value("KGM"))
                    .andExpect(jsonPath("$[0].symbol").value("kg"));
        }

        @Test
        @DisplayName("GET /barcodes resuelve el codigo dentro de la empresa del contexto")
        void get_barcode() throws Exception {
            when(useCase.findByBarcode("7701234567890", COMPANY_ID))
                    .thenReturn(new BarcodeLookupDto("7701234567890", SellableItemType.PRESENTATION,
                            PRESENTATION_ID, "Bulto 15 kg", "KGM", new BigDecimal("120000.00"),
                            15));

            // El barcode se resuelve por empresa: el mismo codigo puede apuntar a
            // articulos distintos en dos clinicas.
            mockMvc.perform(get("/petshop-catalog/barcodes").param("value", "7701234567890"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itemType").value("PRESENTATION"))
                    .andExpect(jsonPath("$.itemId").value(PRESENTATION_ID))
                    .andExpect(jsonPath("$.conversionFactor").value(15));
        }

        @Test
        @DisplayName("GET /barcodes de un codigo desconocido responde 404, no 500")
        void get_barcode_desconocido_responde_404() throws Exception {
            when(useCase.findByBarcode(anyString(), anyLong()))
                    .thenThrow(new PetshopCatalogNotFoundException("barcode", "000"));

            mockMvc.perform(get("/petshop-catalog/barcodes").param("value", "000"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /barcodes sin el parametro value responde 400")
        void get_barcode_sin_valor_responde_400() throws Exception {
            mockMvc.perform(get("/petshop-catalog/barcodes")).andExpect(status().isBadRequest());

            verify(useCase, never()).findByBarcode(anyString(), anyLong());
        }
    }
}
