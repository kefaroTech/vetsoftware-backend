package com.vetsoftware.app.subscription.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.port.in.FindOverlappingSubscriptionItemsUseCase;
import com.vetsoftware.app.subscription.application.port.in.ListAllSubscriptionsUseCase;
import com.vetsoftware.app.subscription.testsupport.SubscriptionMother;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubscriptionAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SubscriptionAdminController — contrato HTTP")
class SubscriptionAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ListAllSubscriptionsUseCase listAllUseCase;
    @MockitoBean
    private FindOverlappingSubscriptionItemsUseCase findOverlapsUseCase;

    @Test
    @DisplayName("pagina contratos de todas las clínicas desde la consola")
    void pagina_contratos_de_todas_las_clinicas_desde_la_consola() throws Exception {
        when(listAllUseCase.listAll(4, 8)).thenReturn(PageResult.empty(4, 8));

        mockMvc.perform(get("/platform-subscriptions").param("page", "4").param("pageSize", "8"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(4)).andExpect(jsonPath("$.pageSize").value(8));

        verify(listAllUseCase).listAll(4, 8);
    }

    @Test
    @DisplayName("el listado de plataforma trae la empresa de cada contrato, sin filtrar")
    void el_listado_de_plataforma_trae_la_empresa_de_cada_contrato() throws Exception {
        when(listAllUseCase.listAll(0, 20))
                .thenReturn(PageResult.of(List.of(SubscriptionMother.dto()), 0, 20, 1L));

        mockMvc.perform(get("/platform-subscriptions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].companyId").value(42))
                .andExpect(jsonPath("$.content[0].subscriptionNumber").value("SUS-2026-00184"))
                .andExpect(jsonPath("$.content[0].current").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("la vigilancia devuelve una lista vacía cuando la plataforma está sana")
    void la_vigilancia_devuelve_lista_vacia_cuando_esta_sana() throws Exception {
        // Cero filas = sano. Que el endpoint responda 200 con [] y no 404 es parte del
        // contrato: quien lo consulta a diario necesita distinguir «no hay solapes» de
        // «el endpoint dejó de existir».
        when(findOverlapsUseCase.findAllOverlaps()).thenReturn(List.of());

        mockMvc.perform(get("/platform-subscriptions/item-overlaps")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("cada solape sale con los dos tramos que se pisan, identificables uno a uno")
    void cada_solape_sale_con_los_dos_tramos() throws Exception {
        // Sin los dos ids y las cuatro fechas, la alerta no es accionable: quien la
        // recibe tiene que poder abrir las dos líneas y decidir cuál cerrar.
        when(findOverlapsUseCase.findAllOverlaps())
                .thenReturn(List.of(SubscriptionMother.solapeDto()));

        mockMvc.perform(get("/platform-subscriptions/item-overlaps")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyId").value(42))
                .andExpect(jsonPath("$[0].subscriptionId").value(7))
                .andExpect(jsonPath("$[0].catalogItemId").value(100))
                .andExpect(jsonPath("$[0].itemCode").value("EXTRA_USER"))
                .andExpect(jsonPath("$[0].firstItemId").value(1))
                .andExpect(jsonPath("$[0].firstFrom").value("2026-01-01"))
                .andExpect(jsonPath("$[0].firstTo").value("2026-06-30"))
                .andExpect(jsonPath("$[0].secondItemId").value(2))
                .andExpect(jsonPath("$[0].secondFrom").value("2026-05-01"))
                .andExpect(jsonPath("$[0].secondTo").value("2026-12-31"));
    }
}
