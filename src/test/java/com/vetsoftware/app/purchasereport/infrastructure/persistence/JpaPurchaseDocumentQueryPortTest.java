package com.vetsoftware.app.purchasereport.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.purchasereport.application.port.out.PurchaseDocumentQueryPort.PurchaseDocumentView;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import com.vetsoftware.app.supplierinvoice.infrastructure.persistence.SupplierInvoiceJpaEntity;
import com.vetsoftware.app.supplierinvoice.infrastructure.persistence.SupplierInvoiceJpaRepository;
import com.vetsoftware.app.supplierinvoice.infrastructure.persistence.SupplierInvoicePaymentJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cruce de vertical slicing: este adapter de {@code purchasereport} lee el
 * {@code SupplierInvoiceJpaRepository} de {@code supplierinvoice}, así que se
 * dobla el repositorio Spring Data subyacente en vez de un {@code port/out}
 * propio de la feature.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaPurchaseDocumentQueryPort (purchasereport) — lee las facturas de proveedor para el libro de compras")
class JpaPurchaseDocumentQueryPortTest {

    private static final Long COMPANY_ID = 5L;
    private static final Long BRANCH_ID = 31L;
    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 1, 31);

    @Mock
    private SupplierInvoiceJpaRepository invoiceJpaRepository;

    @InjectMocks
    private JpaPurchaseDocumentQueryPort port;

    private static SupplierInvoiceJpaEntity factura(SupplierInvoiceStatus status,
            String withholding, String... abonos) {
        // Los abonos se construyen ANTES del when(...) que los devuelve: cada uno abre
        // su
        // propio when(payment.getAmount()), y hacerlo dentro del thenReturn deja el
        // stubbing exterior a medias -> UnfinishedStubbingException en el primer test
        // que
        // toque este fixture.
        List<SupplierInvoicePaymentJpaEntity> pagos = List.of(abonos).stream()
                .map(JpaPurchaseDocumentQueryPortTest::abono).toList();
        SupplierInvoiceJpaEntity entity = mock(SupplierInvoiceJpaEntity.class);
        SupplierJpaEntity supplier = mock(SupplierJpaEntity.class);
        when(supplier.getName()).thenReturn("Distribuidora Sur");
        when(supplier.getTaxId()).thenReturn("900123456");
        when(entity.getId()).thenReturn(1L);
        when(entity.getSupplier()).thenReturn(supplier);
        when(entity.getInvoiceNumber()).thenReturn("FC-100");
        when(entity.getIssueDate()).thenReturn(LocalDate.of(2026, 1, 10));
        when(entity.getDueDate()).thenReturn(LocalDate.of(2026, 2, 10));
        when(entity.getSubtotal()).thenReturn(new BigDecimal("100000.00"));
        when(entity.getTaxAmount()).thenReturn(new BigDecimal("19000.00"));
        when(entity.getWithholdingAmount()).thenReturn(new BigDecimal(withholding));
        when(entity.getTotal()).thenReturn(new BigDecimal("119000.00"));
        when(entity.getStatus()).thenReturn(status);
        when(entity.getPayments()).thenReturn(pagos);
        return entity;
    }

    private static SupplierInvoicePaymentJpaEntity abono(String monto) {
        SupplierInvoicePaymentJpaEntity payment = mock(SupplierInvoicePaymentJpaEntity.class);
        when(payment.getAmount()).thenReturn(new BigDecimal(monto));
        return payment;
    }

    @Nested
    @DisplayName("sede")
    class Sede {

        @Test
        @DisplayName("sin sede consulta todas las facturas de la empresa en el rango, excluyendo las anuladas")
        void sin_sede_consulta_todas_las_facturas_de_la_empresa() {
            SupplierInvoiceJpaEntity entity = factura(SupplierInvoiceStatus.PARTIAL, "0", "50000");
            when(invoiceJpaRepository
                    .findAllByCompany_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
                            COMPANY_ID, DESDE, HASTA, SupplierInvoiceStatus.CANCELLED))
                    .thenReturn(List.of(entity));

            List<PurchaseDocumentView> result = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).supplierName()).isEqualTo("Distribuidora Sur");
            verify(invoiceJpaRepository)
                    .findAllByCompany_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
                            COMPANY_ID, DESDE, HASTA, SupplierInvoiceStatus.CANCELLED);
        }

        @Test
        @DisplayName("con sede filtra ademas por la sucursal, sin tocar la consulta de toda la empresa")
        void con_sede_filtra_ademas_por_la_sucursal() {
            SupplierInvoiceJpaEntity entity = factura(SupplierInvoiceStatus.PENDING, "0");
            when(invoiceJpaRepository
                    .findAllByCompany_IdAndBranch_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
                            COMPANY_ID, BRANCH_ID, DESDE, HASTA, SupplierInvoiceStatus.CANCELLED))
                    .thenReturn(List.of(entity));

            List<PurchaseDocumentView> result = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, BRANCH_ID);

            assertThat(result).hasSize(1);
            verify(invoiceJpaRepository)
                    .findAllByCompany_IdAndBranch_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
                            COMPANY_ID, BRANCH_ID, DESDE, HASTA, SupplierInvoiceStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("estado")
    class Estado {

        @Test
        @DisplayName("un estado presente se expone como su nombre")
        void un_estado_presente_se_expone_como_su_nombre() {
            SupplierInvoiceJpaEntity entity = factura(SupplierInvoiceStatus.PARTIAL, "0", "50000");
            when(invoiceJpaRepository
                    .findAllByCompany_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
                            COMPANY_ID, DESDE, HASTA, SupplierInvoiceStatus.CANCELLED))
                    .thenReturn(List.of(entity));

            PurchaseDocumentView view = port
                    .findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, null).get(0);

            assertThat(view.status()).isEqualTo("PARTIAL");
        }

        @Test
        @DisplayName("un estado nulo se expone como null, no como el texto 'null'")
        void un_estado_nulo_se_expone_como_null() {
            SupplierInvoiceJpaEntity entity = factura(null, "0");
            when(invoiceJpaRepository
                    .findAllByCompany_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
                            COMPANY_ID, DESDE, HASTA, SupplierInvoiceStatus.CANCELLED))
                    .thenReturn(List.of(entity));

            PurchaseDocumentView view = port
                    .findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, null).get(0);

            assertThat(view.status()).isNull();
        }
    }

    @Nested
    @DisplayName("pagado y saldo")
    class PagadoYSaldo {

        @Test
        @DisplayName("el pagado es la suma de los abonos y el saldo descuenta la retencion del total")
        void el_pagado_suma_los_abonos_y_el_saldo_descuenta_la_retencion() {
            SupplierInvoiceJpaEntity entity = factura(SupplierInvoiceStatus.PARTIAL, "9000",
                    "50000");
            when(invoiceJpaRepository
                    .findAllByCompany_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
                            COMPANY_ID, DESDE, HASTA, SupplierInvoiceStatus.CANCELLED))
                    .thenReturn(List.of(entity));

            PurchaseDocumentView view = port
                    .findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, null).get(0);

            // total 119000 - retencion 9000 = 110000 pagable; pagado 50000 -> saldo 60000.
            assertThat(view.paidAmount()).isEqualByComparingTo("50000");
            assertThat(view.balance()).isEqualByComparingTo("60000");
        }

        @Test
        @DisplayName("varios abonos se suman para el pagado total")
        void varios_abonos_se_suman_para_el_pagado_total() {
            SupplierInvoiceJpaEntity entity = factura(SupplierInvoiceStatus.PARTIAL, "0", "30000",
                    "20000");
            when(invoiceJpaRepository
                    .findAllByCompany_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
                            COMPANY_ID, DESDE, HASTA, SupplierInvoiceStatus.CANCELLED))
                    .thenReturn(List.of(entity));

            PurchaseDocumentView view = port
                    .findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, null).get(0);

            assertThat(view.paidAmount()).isEqualByComparingTo("50000");
            assertThat(view.balance()).isEqualByComparingTo("69000");
        }

        @Test
        @DisplayName("sin abonos el pagado es cero y el saldo es el total menos la retencion")
        void sin_abonos_el_pagado_es_cero() {
            SupplierInvoiceJpaEntity entity = factura(SupplierInvoiceStatus.PENDING, "0");
            when(invoiceJpaRepository
                    .findAllByCompany_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
                            COMPANY_ID, DESDE, HASTA, SupplierInvoiceStatus.CANCELLED))
                    .thenReturn(List.of(entity));

            PurchaseDocumentView view = port
                    .findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, null).get(0);

            assertThat(view.paidAmount()).isEqualByComparingTo("0");
            assertThat(view.balance()).isEqualByComparingTo("119000");
        }
    }

    @Nested
    @DisplayName("periodo sin resultados")
    class PeriodoSinResultados {

        @Test
        @DisplayName("un periodo sin facturas devuelve una lista vacia, no null")
        void un_periodo_sin_facturas_devuelve_una_lista_vacia() {
            when(invoiceJpaRepository
                    .findAllByCompany_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
                            COMPANY_ID, DESDE, HASTA, SupplierInvoiceStatus.CANCELLED))
                    .thenReturn(List.of());

            assertThat(port.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, null)).isEmpty();
        }
    }
}
