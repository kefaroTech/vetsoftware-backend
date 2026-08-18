package com.vetsoftware.app.purchaseorder.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.purchaseorder.application.command.CreatePurchaseOrderCommand;
import com.vetsoftware.app.purchaseorder.application.command.PurchaseOrderLineCommand;
import com.vetsoftware.app.purchaseorder.application.command.SearchPurchaseOrdersCommand;
import com.vetsoftware.app.purchaseorder.application.command.UpdatePurchaseOrderCommand;
import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.in.CancelPurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.CreatePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.DeletePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.FindPurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.ListPurchaseOrdersUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.PlacePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.ReactivatePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.SearchPurchaseOrdersUseCase;
import com.vetsoftware.app.purchaseorder.application.port.in.UpdatePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.domain.InvalidPurchaseOrderStatusTransitionException;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderNotFoundException;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.purchaseorder.infrastructure.web.request.CreatePurchaseOrderRequest;
import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
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

/**
 * Rodaja HTTP de {@link PurchaseOrderController}: rutas, binding, validacion
 * del request, codigos de estado y forma del JSON. Lo que hay debajo son dobles
 * de los nueve casos de uso.
 *
 * <p>
 * La empresa y el autor nunca viajan en el cuerpo: los sella {@code Authz}
 * desde el contexto ({@link WebMvcSliceConfig#COMPANY_ID},
 * {@link WebMvcSliceConfig#EMPLOYEE_ID}). Cada escritura lo comprueba
 * capturando el command que llega al caso de uso.
 */
@WebMvcTest(PurchaseOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PurchaseOrderController — contrato HTTP")
class PurchaseOrderControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;
    private static final Long ID = 1L;
    private static final Long AJENO_ID = 99L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @Autowired
    private PurchaseOrderController controller;

    @MockitoBean
    private CreatePurchaseOrderUseCase createUseCase;
    @MockitoBean
    private UpdatePurchaseOrderUseCase updateUseCase;
    @MockitoBean
    private FindPurchaseOrderUseCase findUseCase;
    @MockitoBean
    private ListPurchaseOrdersUseCase listUseCase;
    @MockitoBean
    private SearchPurchaseOrdersUseCase searchUseCase;
    @MockitoBean
    private DeletePurchaseOrderUseCase deleteUseCase;
    @MockitoBean
    private ReactivatePurchaseOrderUseCase reactivateUseCase;
    @MockitoBean
    private PlacePurchaseOrderUseCase placeUseCase;
    @MockitoBean
    private CancelPurchaseOrderUseCase cancelUseCase;

    private static PurchaseOrderDto emitidaDto() {
        return PurchaseOrderDto.from(PurchaseOrderMother.emitida());
    }

    private static PurchaseOrderDto borradorDto() {
        return PurchaseOrderDto.from(PurchaseOrderMother.borrador());
    }

    private static PurchaseOrderDto pausadaDto() {
        return PurchaseOrderDto.from(PurchaseOrderMother.pausada());
    }

    @Nested
    @DisplayName("POST /purchase-orders — creacion")
    class Creacion {

        @Test
        @DisplayName("crea la orden y sella empresa y autor desde el contexto")
        void crea_la_orden_y_sella_empresa_y_autor() throws Exception {
            when(createUseCase.execute(any())).thenReturn(borradorDto());

            mockMvc.perform(
                    post("/purchase-orders").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":4,"supplierId":7,"orderDate":"2026-03-10",
                             "expectedDate":"2026-03-20","notes":"Pedido mensual",
                             "lines":[{"productId":11,"quantityOrdered":10,"unitCost":15000.00}]}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.company.id").value(9))
                    .andExpect(jsonPath("$.branch.name").value("Sede Norte"));

            ArgumentCaptor<CreatePurchaseOrderCommand> captor = ArgumentCaptor
                    .forClass(CreatePurchaseOrderCommand.class);
            verify(createUseCase).execute(captor.capture());
            CreatePurchaseOrderCommand command = captor.getValue();
            org.assertj.core.api.Assertions
                    .assertThat(
                            command)
                    .isEqualTo(new CreatePurchaseOrderCommand(4L, 7L, LocalDate.of(2026, 3, 10),
                            LocalDate.of(2026, 3, 20), "Pedido mensual",
                            List.of(new PurchaseOrderLineCommand(11L, 10,
                                    new BigDecimal("15000.00"))),
                            COMPANY_ID, EMPLOYEE_ID));
        }

        @Test
        @DisplayName("sin lineas responde 400 y no toca el caso de uso")
        void sin_lineas_responde_400() throws Exception {
            mockMvc.perform(
                    post("/purchase-orders").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":4,"supplierId":7,"orderDate":"2026-03-10","lines":[]}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("con una linea de cantidad cero responde 400 y no toca el caso de uso")
        void linea_con_cantidad_cero_responde_400() throws Exception {
            mockMvc.perform(
                    post("/purchase-orders").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":4,"supplierId":7,"orderDate":"2026-03-10",
                             "lines":[{"productId":11,"quantityOrdered":0,"unitCost":15000.00}]}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("sin sede responde 400 y no toca el caso de uso")
        void sin_sede_responde_400() throws Exception {
            mockMvc.perform(
                    post("/purchase-orders").contentType(MediaType.APPLICATION_JSON).content("""
                            {"supplierId":7,"orderDate":"2026-03-10",
                             "lines":[{"productId":11,"quantityOrdered":10,"unitCost":15000.00}]}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("una lista de lineas nula en el command se traduce en lista vacia — "
                + "el @NotEmpty del request nunca deja pasar esto por HTTP, se invoca el "
                + "controller directo para cerrar el defensivo null-check de toLineCommands")
        void una_lista_de_lineas_nula_se_traduce_en_lista_vacia() {
            when(createUseCase.execute(any())).thenReturn(borradorDto());
            CreatePurchaseOrderRequest requestConLineasNulas = new CreatePurchaseOrderRequest(4L,
                    7L, LocalDate.of(2026, 3, 10), null, null, null);

            controller.create(requestConLineasNulas);

            ArgumentCaptor<CreatePurchaseOrderCommand> captor = ArgumentCaptor
                    .forClass(CreatePurchaseOrderCommand.class);
            verify(createUseCase).execute(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue().lines()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /purchase-orders — listados")
    class Listados {

        @Test
        @DisplayName("lista las ordenes activas de la empresa del contexto")
        void lista_las_ordenes_activas() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID)).thenReturn(List.of(emitidaDto()));

            mockMvc.perform(get("/purchase-orders")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].status").value("PLACED"));

            verify(listUseCase).listByCompany(COMPANY_ID);
        }

        @Test
        @DisplayName("GET /purchase-orders/disabled lista las ordenes pausadas")
        void lista_las_ordenes_pausadas() throws Exception {
            when(listUseCase.listDisabledByCompany(COMPANY_ID)).thenReturn(List.of(pausadaDto()));

            mockMvc.perform(get("/purchase-orders/disabled")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].enabled").value(false));

            verify(listUseCase).listDisabledByCompany(COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("GET /purchase-orders/search — busqueda paginada")
    class Busqueda {

        @Test
        @DisplayName("sin parametros usa pagina 0, tamano 20 y ningun filtro")
        void sin_parametros_usa_los_defectos() throws Exception {
            when(searchUseCase.execute(any())).thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/purchase-orders/search")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20))
                    .andExpect(jsonPath("$.totalElements").value(0));

            verify(searchUseCase).execute(new SearchPurchaseOrdersCommand(COMPANY_ID, null, null,
                    null, null, null, 0, 20));
        }

        @Test
        @DisplayName("traslada proveedor, sede, estado y rango de fechas al command")
        void traslada_los_filtros_al_command() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(emitidaDto()), 1, 5, 11L, 3));

            mockMvc.perform(get("/purchase-orders/search").param("supplierId", "7")
                    .param("branchId", "4").param("status", "PLACED").param("from", "2026-01-01")
                    .param("to", "2026-01-31").param("page", "1").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));

            verify(searchUseCase).execute(
                    new SearchPurchaseOrdersCommand(COMPANY_ID, 7L, 4L, PurchaseOrderStatus.PLACED,
                            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 1, 5));
        }
    }

    @Nested
    @DisplayName("GET /purchase-orders/{id} — detalle")
    class Detalle {

        @Test
        @DisplayName("devuelve la orden de la empresa del contexto")
        void devuelve_la_orden() throws Exception {
            when(findUseCase.findById(ID, COMPANY_ID)).thenReturn(emitidaDto());

            mockMvc.perform(get("/purchase-orders/{id}", ID)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.supplier.name").value("Distribuidora Animal"));
        }

        @Test
        @DisplayName("una orden inexistente o ajena responde 404, no 500")
        void orden_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(AJENO_ID, COMPANY_ID))
                    .thenThrow(new PurchaseOrderNotFoundException(AJENO_ID));

            mockMvc.perform(get("/purchase-orders/{id}", AJENO_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /purchase-orders/{id} — actualizacion")
    class Actualizacion {

        private static final String CUERPO_VALIDO = """
                {"branchId":5,"supplierId":8,"orderDate":"2026-03-10","expectedDate":"2026-03-20",
                 "notes":"Ajuste de pedido",
                 "lines":[{"productId":12,"quantityOrdered":4,"unitCost":2500.00}],"version":0}
                """;

        @Test
        @DisplayName("actualiza la orden y sella empresa, autor y version en el command")
        void actualiza_la_orden() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(emitidaDto());

            mockMvc.perform(put("/purchase-orders/{id}", ID).contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            ArgumentCaptor<UpdatePurchaseOrderCommand> captor = ArgumentCaptor
                    .forClass(UpdatePurchaseOrderCommand.class);
            verify(updateUseCase).execute(captor.capture());
            UpdatePurchaseOrderCommand command = captor.getValue();
            org.assertj.core.api.Assertions.assertThat(command)
                    .isEqualTo(
                            new UpdatePurchaseOrderCommand(ID, 5L, 8L, LocalDate.of(2026, 3, 10),
                                    LocalDate.of(2026, 3, 20), "Ajuste de pedido",
                                    List.of(new PurchaseOrderLineCommand(12L, 4,
                                            new BigDecimal("2500.00"))),
                                    COMPANY_ID, EMPLOYEE_ID, 0L));
        }

        @Test
        @DisplayName("sin version responde 400 y no toca el caso de uso")
        void sin_version_responde_400() throws Exception {
            mockMvc.perform(put("/purchase-orders/{id}", ID).contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"branchId":5,"supplierId":8,"orderDate":"2026-03-10",
                             "lines":[{"productId":12,"quantityOrdered":4,"unitCost":2500.00}]}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(updateUseCase);
        }

        @Test
        @DisplayName("una orden inexistente o ajena responde 404")
        void orden_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new PurchaseOrderNotFoundException(ID));

            mockMvc.perform(put("/purchase-orders/{id}", ID).contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("una transicion de estado invalida responde 409, no 500")
        void transicion_invalida_responde_409() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(
                    new InvalidPurchaseOrderStatusTransitionException(PurchaseOrderStatus.RECEIVED,
                            PurchaseOrderStatus.DRAFT));

            mockMvc.perform(put("/purchase-orders/{id}", ID).contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /purchase-orders/{id} — pausa")
    class Eliminacion {

        @Test
        @DisplayName("pausa la orden de la empresa del contexto y responde 204")
        void pausa_la_orden() throws Exception {
            mockMvc.perform(delete("/purchase-orders/{id}", ID)).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(ID, COMPANY_ID);
        }

        @Test
        @DisplayName("una orden inexistente o ajena responde 404")
        void orden_inexistente_responde_404() throws Exception {
            doThrow(new PurchaseOrderNotFoundException(AJENO_ID)).when(deleteUseCase)
                    .execute(AJENO_ID, COMPANY_ID);

            mockMvc.perform(delete("/purchase-orders/{id}", AJENO_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /purchase-orders/{id}/enable — reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva una orden pausada de la empresa del contexto")
        void reactiva_la_orden() throws Exception {
            when(reactivateUseCase.execute(ID, COMPANY_ID)).thenReturn(borradorDto());

            mockMvc.perform(patch("/purchase-orders/{id}/enable", ID)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("una orden inexistente o ajena responde 404")
        void orden_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(AJENO_ID, COMPANY_ID))
                    .thenThrow(new PurchaseOrderNotFoundException(AJENO_ID));

            mockMvc.perform(patch("/purchase-orders/{id}/enable", AJENO_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /purchase-orders/{id}/place — emision")
    class Emision {

        @Test
        @DisplayName("emite la orden y sella el autor desde el contexto")
        void emite_la_orden() throws Exception {
            when(placeUseCase.execute(ID, COMPANY_ID, EMPLOYEE_ID)).thenReturn(emitidaDto());

            mockMvc.perform(post("/purchase-orders/{id}/place", ID)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PLACED"));

            verify(placeUseCase).execute(ID, COMPANY_ID, EMPLOYEE_ID);
        }

        @Test
        @DisplayName("una orden inexistente o ajena responde 404")
        void orden_inexistente_responde_404() throws Exception {
            when(placeUseCase.execute(AJENO_ID, COMPANY_ID, EMPLOYEE_ID))
                    .thenThrow(new PurchaseOrderNotFoundException(AJENO_ID));

            mockMvc.perform(post("/purchase-orders/{id}/place", AJENO_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("emitir una orden que no esta en DRAFT responde 409, no 500")
        void transicion_invalida_responde_409() throws Exception {
            when(placeUseCase.execute(ID, COMPANY_ID, EMPLOYEE_ID)).thenThrow(
                    new InvalidPurchaseOrderStatusTransitionException(PurchaseOrderStatus.CANCELLED,
                            PurchaseOrderStatus.PLACED));

            mockMvc.perform(post("/purchase-orders/{id}/place", ID))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /purchase-orders/{id}/cancel — cancelacion")
    class Cancelacion {

        @Test
        @DisplayName("cancela la orden y sella el autor desde el contexto")
        void cancela_la_orden() throws Exception {
            when(cancelUseCase.execute(ID, COMPANY_ID, EMPLOYEE_ID)).thenReturn(emitidaDto());

            mockMvc.perform(post("/purchase-orders/{id}/cancel", ID)).andExpect(status().isOk());

            verify(cancelUseCase).execute(ID, COMPANY_ID, EMPLOYEE_ID);
        }

        @Test
        @DisplayName("una orden inexistente o ajena responde 404")
        void orden_inexistente_responde_404() throws Exception {
            when(cancelUseCase.execute(AJENO_ID, COMPANY_ID, EMPLOYEE_ID))
                    .thenThrow(new PurchaseOrderNotFoundException(AJENO_ID));

            mockMvc.perform(post("/purchase-orders/{id}/cancel", AJENO_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("cancelar una orden ya recibida responde 409, no 500")
        void transicion_invalida_responde_409() throws Exception {
            when(cancelUseCase.execute(ID, COMPANY_ID, EMPLOYEE_ID)).thenThrow(
                    new InvalidPurchaseOrderStatusTransitionException(PurchaseOrderStatus.RECEIVED,
                            PurchaseOrderStatus.CANCELLED));

            mockMvc.perform(post("/purchase-orders/{id}/cancel", ID))
                    .andExpect(status().isConflict());
        }
    }
}
