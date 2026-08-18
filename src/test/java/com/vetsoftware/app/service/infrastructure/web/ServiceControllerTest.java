package com.vetsoftware.app.service.infrastructure.web;

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

import com.vetsoftware.app.service.application.command.CreateServiceCommand;
import com.vetsoftware.app.service.application.command.SearchServicesCommand;
import com.vetsoftware.app.service.application.command.UpdateServiceCommand;
import com.vetsoftware.app.service.application.dto.CompanySummaryDto;
import com.vetsoftware.app.service.application.dto.ServiceCategorySummaryDto;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.dto.TaxSummaryDto;
import com.vetsoftware.app.service.application.port.in.CreateServiceUseCase;
import com.vetsoftware.app.service.application.port.in.DeleteServiceUseCase;
import com.vetsoftware.app.service.application.port.in.FindServiceUseCase;
import com.vetsoftware.app.service.application.port.in.ListServicesByCompanyUseCase;
import com.vetsoftware.app.service.application.port.in.ReactivateServiceUseCase;
import com.vetsoftware.app.service.application.port.in.SearchServicesUseCase;
import com.vetsoftware.app.service.application.port.in.UpdateServiceUseCase;
import com.vetsoftware.app.service.domain.TaxTreatment;
import com.vetsoftware.app.shared.pagination.PageResult;
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
 * Rodaja HTTP de {@code ServiceController}: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON — incluidos los companion
 * {@code ServiceCategorySummary}/{@code TaxSummary}/{@code CompanySummary} que
 * arma {@code toResponse}.
 *
 * <p>
 * La empresa nunca viaja en el cuerpo: la pone {@code Authz} desde el contexto.
 * Cada traduccion request→command se comprueba contra el {@code companyId} fijo
 * de {@link WebMvcSliceConfig}.
 */
@WebMvcTest(ServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ServiceController — contrato HTTP")
class ServiceControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;
    private static final Long SERVICE_ID = 1L;
    private static final Long CATEGORY_ID = 20L;
    private static final Long TAX_ID = 30L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateServiceUseCase createUseCase;
    @MockitoBean
    private UpdateServiceUseCase updateUseCase;
    @MockitoBean
    private FindServiceUseCase findUseCase;
    @MockitoBean
    private ListServicesByCompanyUseCase listByCompanyUseCase;
    @MockitoBean
    private SearchServicesUseCase searchUseCase;
    @MockitoBean
    private DeleteServiceUseCase deleteUseCase;
    @MockitoBean
    private ReactivateServiceUseCase reactivateUseCase;

    private static ServiceDto servicioDto() {
        return new ServiceDto(SERVICE_ID, "Consulta general", new BigDecimal("50000.00"),
                TaxTreatment.GRAVADO, "notas",
                new ServiceCategorySummaryDto(CATEGORY_ID, "Consultas"),
                new TaxSummaryDto(TAX_ID, "IVA 19%", new BigDecimal("19.00")),
                new CompanySummaryDto(COMPANY_ID, "Veterinaria de prueba", "900123456"),
                LocalDateTime.of(2026, 1, 15, 8, 0), null, null, 0L, true);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("POST /services sella la empresa del contexto y responde 201 con el body mapeado")
        void post_crea_y_sella_la_empresa_del_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(servicioDto());

            mockMvc.perform(post("/services").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Consulta general","price":50000.00,"notes":"notas",
                     "taxTreatment":"GRAVADO","serviceCategoryId":20,"taxId":30}
                    """)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.serviceCategory.id").value(20))
                    .andExpect(jsonPath("$.tax.id").value(30))
                    .andExpect(jsonPath("$.company.id").value(COMPANY_ID))
                    .andExpect(jsonPath("$.enabled").value(true));

            verify(createUseCase).execute(
                    new CreateServiceCommand("Consulta general", new BigDecimal("50000.00"),
                            TaxTreatment.GRAVADO, "notas", CATEGORY_ID, TAX_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("POST /services sin nombre responde 400 y no llama al caso de uso")
        void post_sin_nombre_responde_400() throws Exception {
            mockMvc.perform(post("/services").contentType(MediaType.APPLICATION_JSON).content("""
                    {"price":50000.00,"taxTreatment":"GRAVADO","serviceCategoryId":20}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST /services con precio negativo responde 400")
        void post_con_precio_negativo_responde_400() throws Exception {
            mockMvc.perform(post("/services").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Consulta","price":-1,"taxTreatment":"GRAVADO","serviceCategoryId":20}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un servicio EXENTO sin taxId tambien se acepta en el request")
        void servicio_exento_sin_tax_id_se_acepta() throws Exception {
            when(createUseCase.execute(any())).thenReturn(servicioDto());

            mockMvc.perform(post("/services").contentType(MediaType.APPLICATION_JSON).content(
                    """
                            {"name":"Vacunacion","price":30000.00,"taxTreatment":"EXENTO","serviceCategoryId":20}
                            """))
                    .andExpect(status().isCreated());

            verify(createUseCase)
                    .execute(new CreateServiceCommand("Vacunacion", new BigDecimal("30000.00"),
                            TaxTreatment.EXENTO, null, CATEGORY_ID, null, COMPANY_ID));
        }
    }

    @Nested
    @DisplayName("lectura")
    class Lectura {

        @Test
        @DisplayName("GET /services devuelve la lista de la empresa del contexto")
        void get_lista_los_servicios_de_la_empresa() throws Exception {
            when(listByCompanyUseCase.listByCompany(COMPANY_ID)).thenReturn(List.of(servicioDto()));

            mockMvc.perform(get("/services")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Consulta general"));
        }

        @Test
        @DisplayName("GET /services/disabled devuelve los servicios pausados de la empresa")
        void get_disabled_lista_los_pausados() throws Exception {
            when(listByCompanyUseCase.listDisabledByCompany(COMPANY_ID))
                    .thenReturn(List.of(servicioDto()));

            mockMvc.perform(get("/services/disabled")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("GET /services/{id} resuelve la empresa desde el contexto, no del cliente")
        void get_por_id_usa_la_empresa_del_contexto() throws Exception {
            when(findUseCase.findById(SERVICE_ID, COMPANY_ID)).thenReturn(servicioDto());

            mockMvc.perform(get("/services/1")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Consulta general"));
        }

        @Test
        @DisplayName("GET /services/search traduce los filtros y la empresa del contexto")
        void get_search_traduce_los_filtros() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(PageResult.of(List.of(servicioDto()), 0, 20, 1L));

            mockMvc.perform(get("/services/search").param("name", "Consulta")
                    .param("serviceCategoryId", "20").param("taxId", "30"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(searchUseCase).execute(
                    new SearchServicesCommand(COMPANY_ID, "Consulta", CATEGORY_ID, TAX_ID, 0, 20));
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("PUT /services/{id} traslada el id de la ruta, la empresa y el empleado del contexto")
        void put_actualiza_con_el_id_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(servicioDto());

            mockMvc.perform(put("/services/1").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Consulta general","price":55000.00,"taxTreatment":"GRAVADO",
                     "serviceCategoryId":20,"taxId":30,"version":0}
                    """)).andExpect(status().isOk());

            verify(updateUseCase).execute(new UpdateServiceCommand(SERVICE_ID, "Consulta general",
                    new BigDecimal("55000.00"), TaxTreatment.GRAVADO, null, CATEGORY_ID, TAX_ID,
                    COMPANY_ID, EMPLOYEE_ID, 0L));
        }

        @Test
        @DisplayName("PUT /services/{id} sin version responde 400")
        void put_sin_version_responde_400() throws Exception {
            mockMvc.perform(put("/services/1").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Consulta general","price":55000.00,"taxTreatment":"GRAVADO",
                     "serviceCategoryId":20}
                    """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("eliminacion y ciclo de estado")
    class EliminacionYCicloDeEstado {

        @Test
        @DisplayName("DELETE /services/{id} delega en el caso de uso con id y empresa, y responde 204")
        void delete_delega_en_el_caso_de_uso() throws Exception {
            mockMvc.perform(delete("/services/1")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(SERVICE_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("PATCH /services/{id}/enable delega en el caso de uso y responde con el servicio reactivado")
        void patch_enable_delega_en_el_caso_de_uso() throws Exception {
            when(reactivateUseCase.execute(SERVICE_ID, COMPANY_ID)).thenReturn(servicioDto());

            mockMvc.perform(patch("/services/1/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));
        }
    }
}
