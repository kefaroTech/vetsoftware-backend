package com.vetsoftware.app.uvtvalue.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import com.vetsoftware.app.uvtvalue.application.dto.UvtValueDto;
import com.vetsoftware.app.uvtvalue.application.port.in.CreateUvtValueUseCase;
import com.vetsoftware.app.uvtvalue.application.port.in.FindUvtValueForYearUseCase;
import com.vetsoftware.app.uvtvalue.application.port.in.ListUvtValuesUseCase;
import java.math.BigDecimal;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UvtValueController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("UvtValueController — contrato HTTP de la UVT por ano")
class UvtValueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateUvtValueUseCase createUseCase;

    @MockitoBean
    private FindUvtValueForYearUseCase findUseCase;

    @MockitoBean
    private ListUvtValuesUseCase listUseCase;

    private static UvtValueDto uvt2026() {
        return new UvtValueDto(7200L, 2026, new BigDecimal("52374.00"),
                "Resolucion DIAN 000238 del 15-12-2025", LocalDateTime.of(2025, 12, 16, 8, 0, 0),
                true);
    }

    @Nested
    @DisplayName("Publicacion")
    class Publicacion {

        @Test
        @DisplayName("responde 201 con la cifra y la resolucion que la fija")
        void responde_201_con_la_cifra_y_la_resolucion() throws Exception {
            when(createUseCase.execute(any())).thenReturn(uvt2026());

            mockMvc.perform(post("/uvt-values").contentType(MediaType.APPLICATION_JSON).content("""
                    {"fiscalYear":2026,"valueAmount":52374.00,
                     "legalReference":"Resolucion DIAN 000238 del 15-12-2025"}
                    """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fiscalYear").value(2026))
                    .andExpect(jsonPath("$.valueAmount").value(52374.00))
                    .andExpect(jsonPath("$.legalReference")
                            .value("Resolucion DIAN 000238 del 15-12-2025"));
        }

        @Test
        @DisplayName("una cifra de cero sale 400 y NO llega al caso de uso")
        void una_cifra_de_cero_sale_400() throws Exception {
            mockMvc.perform(post("/uvt-values").contentType(MediaType.APPLICATION_JSON).content("""
                    {"fiscalYear":2026,"valueAmount":0,"legalReference":"Resolucion"}
                    """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("una referencia legal en blanco sale 400: la cifra sin norma no vale")
        void una_referencia_en_blanco_sale_400() throws Exception {
            mockMvc.perform(post("/uvt-values").contentType(MediaType.APPLICATION_JSON).content("""
                    {"fiscalYear":2026,"valueAmount":52374.00,"legalReference":"  "}
                    """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }
    }

    @Nested
    @DisplayName("Lectura por ano")
    class LecturaPorAno {

        @Test
        @DisplayName("la ruta exige el ano: no hay endpoint de «la UVT vigente»")
        void la_ruta_exige_el_ano() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(WebMvcSliceConfig.COMPANY_ID);
            when(findUseCase.findByYear(anyInt(), any())).thenReturn(uvt2026());

            mockMvc.perform(get("/uvt-values/years/2026")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.fiscalYear").value(2026));

            ArgumentCaptor<Integer> ano = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Long> empresa = ArgumentCaptor.forClass(Long.class);
            verify(findUseCase).findByYear(ano.capture(), empresa.capture());
            assertThat(ano.getValue()).isEqualTo(2026);
            // La empresa viaja aunque la tabla no la tenga: es lo que permite al puerto
            // cerrarle al empleado la via ancha con @authz.isMyCompany.
            assertThat(empresa.getValue()).isEqualTo(WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("el listado paginado usa el contrato unico de pagina")
        void el_listado_paginado_usa_el_contrato_unico() throws Exception {
            when(listUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(uvt2026()), 0, 20, 1));

            mockMvc.perform(get("/uvt-values")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
}
