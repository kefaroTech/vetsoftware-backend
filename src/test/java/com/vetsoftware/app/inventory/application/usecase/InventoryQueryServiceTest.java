package com.vetsoftware.app.inventory.application.usecase;

import static com.vetsoftware.app.inventory.testsupport.InventoryMother.BRANCH_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COMPANY_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.PRODUCT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.compra;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.inventory.application.command.InventoryAlertsQuery;
import com.vetsoftware.app.inventory.application.command.InventoryValuationQuery;
import com.vetsoftware.app.inventory.application.command.ListLotsCommand;
import com.vetsoftware.app.inventory.application.command.SearchKardexCommand;
import com.vetsoftware.app.inventory.application.command.SearchPurchasesQuery;
import com.vetsoftware.app.inventory.application.command.SearchStockCommand;
import com.vetsoftware.app.inventory.application.dto.InventoryAlertsView;
import com.vetsoftware.app.inventory.application.dto.InventoryValuationView;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.inventory.application.dto.PurchaseView;
import com.vetsoftware.app.inventory.application.dto.StockLotView;
import com.vetsoftware.app.inventory.application.dto.StockMovementView;
import com.vetsoftware.app.inventory.application.dto.StockView;
import com.vetsoftware.app.inventory.application.port.out.StockQueryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lecturas del inventario. El servicio es una fachada sobre el read model, asi
 * que lo unico que hay que fijar es que cada consulta llegue al puerto con sus
 * filtros intactos: reinterpretar aqui un branchId o un rango daria un reporte
 * que no es el que se pidio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryQueryService — lecturas del inventario")
class InventoryQueryServiceTest {

    @Mock
    private StockQueryPort stockQueryPort;

    @InjectMocks
    private InventoryQueryService service;

    @Test
    @DisplayName("las alertas viajan con la sede y la ventana de vencimiento pedidas")
    void las_alertas_viajan_con_sus_filtros() {
        InventoryAlertsView esperada = new InventoryAlertsView(List.of(), List.of());
        when(stockQueryPort.alerts(COMPANY_ID, BRANCH_ID, 30)).thenReturn(esperada);

        InventoryAlertsView vista = service
                .alerts(new InventoryAlertsQuery(COMPANY_ID, BRANCH_ID, 30));

        assertThat(vista).isSameAs(esperada);
        verify(stockQueryPort).alerts(COMPANY_ID, BRANCH_ID, 30);
    }

    @Test
    @DisplayName("la valuacion viaja con la sede pedida")
    void la_valuacion_viaja_con_la_sede_pedida() {
        InventoryValuationView esperada = new InventoryValuationView(BigDecimal.TEN, 0, List.of());
        when(stockQueryPort.valuation(COMPANY_ID, BRANCH_ID)).thenReturn(esperada);

        assertThat(service.valuation(new InventoryValuationQuery(COMPANY_ID, BRANCH_ID)))
                .isSameAs(esperada);
    }

    @Test
    @DisplayName("una sede null en la valuacion significa todas las sedes")
    void una_sede_null_significa_todas_las_sedes() {
        InventoryValuationView esperada = new InventoryValuationView(BigDecimal.TEN, 0, List.of());
        when(stockQueryPort.valuation(COMPANY_ID, null)).thenReturn(esperada);

        service.valuation(new InventoryValuationQuery(COMPANY_ID, null));

        // El null tiene que viajar tal cual: convertirlo en algo aqui dejaria la
        // valuacion consolidada de la empresa sin poder pedirse.
        verify(stockQueryPort).valuation(COMPANY_ID, null);
    }

    @Test
    @DisplayName("el libro de compras devuelve la pagina del read model")
    void el_libro_de_compras_devuelve_la_pagina() {
        SearchPurchasesQuery consulta = new SearchPurchasesQuery(COMPANY_ID, BRANCH_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 1, 20);
        when(stockQueryPort.purchases(consulta))
                .thenReturn(new PageResult<>(List.of(compra(1L, 10, "15000")), 1, 20, 41L, 3));

        PageResult<PurchaseView> pagina = service.purchases(consulta);

        assertThat(pagina.content()).extracting(PurchaseView::id).containsExactly(1L);
        assertThat(pagina.totalElements()).isEqualTo(41L);
    }

    @Test
    @DisplayName("la busqueda de stock traslada el comando entero, incluido el filtro de bajo stock")
    void la_busqueda_de_stock_traslada_el_comando_entero() {
        SearchStockCommand comando = new SearchStockCommand(COMPANY_ID, BRANCH_ID, "amox", true,
                List.of(), 2, 20);
        when(stockQueryPort.searchStock(comando))
                .thenReturn(new PageResult<>(List.of(), 2, 20, 0L, 0));

        PageResult<StockView> pagina = service.search(comando);

        assertThat(pagina.page()).isEqualTo(2);
        verify(stockQueryPort).searchStock(comando);
    }

    @Test
    @DisplayName("el listado de lotes desarma el comando en sus tres filtros")
    void el_listado_de_lotes_desarma_el_comando() {
        when(stockQueryPort.listLots(COMPANY_ID, BRANCH_ID, PRODUCT_ID)).thenReturn(List.of());

        List<StockLotView> lotes = service
                .listLots(new ListLotsCommand(COMPANY_ID, BRANCH_ID, PRODUCT_ID));

        assertThat(lotes).isEmpty();
        verify(stockQueryPort).listLots(COMPANY_ID, BRANCH_ID, PRODUCT_ID);
    }

    @Test
    @DisplayName("el kardex traslada el comando con su rango de fechas")
    void el_kardex_traslada_el_comando_con_su_rango() {
        SearchKardexCommand comando = new SearchKardexCommand(COMPANY_ID, BRANCH_ID, PRODUCT_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 0, 50);
        when(stockQueryPort.searchKardex(comando))
                .thenReturn(new PageResult<>(List.of(), 0, 50, 0L, 0));

        PageResult<StockMovementView> pagina = service.searchKardex(comando);

        assertThat(pagina.pageSize()).isEqualTo(50);
        verify(stockQueryPort).searchKardex(comando);
    }
}
