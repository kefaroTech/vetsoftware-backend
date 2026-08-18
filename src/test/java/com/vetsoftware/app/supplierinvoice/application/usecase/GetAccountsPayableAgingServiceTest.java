package com.vetsoftware.app.supplierinvoice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplierinvoice.application.dto.AccountsPayableAgingDto;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
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
 * Test del aging: reparte el saldo de las facturas pendientes en los tramos
 * según los días vencidos vs asOf.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetAccountsPayableAgingService")
class GetAccountsPayableAgingServiceTest {

    private static final CompanyRef CO = new CompanyRef(1L, "Vet SAS", "900123456-7");
    private static final BranchRef BR = new BranchRef(10L, "Principal");
    private static final SupplierRef SUP = new SupplierRef(5L, "Distribuidora", "800111222-3");
    private static final LocalDate AS_OF = LocalDate.of(2026, 7, 31);

    @Mock
    private SupplierInvoiceRepository repository;

    @InjectMocks
    private GetAccountsPayableAgingService service;

    private static SupplierInvoice inv(String number, LocalDate due, String amount) {
        LocalDate issue = due.minusDays(30);
        return SupplierInvoice.create(CO, BR, SUP, null, null, number, issue, due,
                new BigDecimal(amount), BigDecimal.ZERO, BigDecimal.ZERO, null, 7L);
    }

    @Nested
    @DisplayName("reparto por tramos")
    class RepartoPorTramos {

        @Test
        @DisplayName("reparte los saldos por tramo de antiguedad")
        void reparte_saldos_por_tramo_de_antiguedad() {
            List<SupplierInvoice> outstanding = List.of(inv("A", LocalDate.of(2026, 8, 15), "1000"), // futuro
                                                                                                     // →
                                                                                                     // al
                                                                                                     // día
                    inv("B", LocalDate.of(2026, 7, 20), "500"), // 11 días vencida → 1–30
                    inv("C", LocalDate.of(2026, 4, 1), "300")); // >90 días → +90
            when(repository.findOutstandingByCompany(1L, null)).thenReturn(outstanding);

            AccountsPayableAgingDto dto = service.get(1L, null, AS_OF);

            assertThat(dto.asOf()).isEqualTo(AS_OF);
            assertThat(dto.suppliers()).hasSize(1);
            AccountsPayableAgingDto.Bucket b = dto.suppliers().get(0).bucket();
            assertThat(b.current()).isEqualByComparingTo("1000");
            assertThat(b.days1to30()).isEqualByComparingTo("500");
            assertThat(b.days31to60()).isEqualByComparingTo("0");
            assertThat(b.over90()).isEqualByComparingTo("300");
            assertThat(b.total()).isEqualByComparingTo("1800");
            assertThat(dto.totals().total()).isEqualByComparingTo("1800");
        }

        @Test
        @DisplayName("sin pendientes devuelve totales en cero")
        void sin_pendientes_devuelve_totales_en_cero() {
            when(repository.findOutstandingByCompany(1L, null)).thenReturn(List.of());

            AccountsPayableAgingDto dto = service.get(1L, null, AS_OF);

            assertThat(dto.suppliers()).isEmpty();
            assertThat(dto.totals().total()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("agrupa varios proveedores por separado, cada uno con su propio tramo")
        void agrupa_varios_proveedores_por_separado() {
            SupplierRef otroProveedor = new SupplierRef(6L, "Otro Proveedor", "900999888-1");
            SupplierInvoice deSup = inv("A", LocalDate.of(2026, 7, 20), "500");
            SupplierInvoice deOtro = SupplierInvoice.create(CO, BR, otroProveedor, null, null, "B",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), new BigDecimal("700"),
                    BigDecimal.ZERO, BigDecimal.ZERO, null, 7L);
            when(repository.findOutstandingByCompany(1L, null)).thenReturn(List.of(deSup, deOtro));

            AccountsPayableAgingDto dto = service.get(1L, null, AS_OF);

            assertThat(dto.suppliers()).hasSize(2);
            assertThat(dto.totals().total()).isEqualByComparingTo("1200");
        }

        @Test
        @DisplayName("filtra por sede cuando se indica branchId")
        void filtra_por_sede_cuando_se_indica_branchId() {
            when(repository.findOutstandingByCompany(1L, 10L))
                    .thenReturn(List.of(inv("A", LocalDate.of(2026, 7, 20), "500")));

            AccountsPayableAgingDto dto = service.get(1L, 10L, AS_OF);

            assertThat(dto.totals().total()).isEqualByComparingTo("500");
        }

        @Test
        @DisplayName("sin asOf explicito usa la fecha actual como referencia")
        void sin_asof_explicito_usa_la_fecha_actual() {
            when(repository.findOutstandingByCompany(1L, null)).thenReturn(List.of());

            AccountsPayableAgingDto dto = service.get(1L, null, null);

            assertThat(dto.asOf()).isNotNull();
        }
    }

    @Nested
    @DisplayName("limites exactos de cada tramo")
    class LimitesExactosDeCadaTramo {

        @Test
        @DisplayName("0 dias vencidos cae en el tramo al dia")
        void cero_dias_vencidos_cae_en_el_tramo_al_dia() {
            when(repository.findOutstandingByCompany(1L, null))
                    .thenReturn(List.of(inv("A", AS_OF, "100")));

            AccountsPayableAgingDto.Bucket b = service.get(1L, null, AS_OF).totals();

            assertThat(b.current()).isEqualByComparingTo("100");
            assertThat(b.days1to30()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("30 dias vencidos todavia cae en el tramo 1-30")
        void treinta_dias_vencidos_cae_en_el_tramo_1_30() {
            when(repository.findOutstandingByCompany(1L, null))
                    .thenReturn(List.of(inv("A", AS_OF.minusDays(30), "100")));

            AccountsPayableAgingDto.Bucket b = service.get(1L, null, AS_OF).totals();

            assertThat(b.days1to30()).isEqualByComparingTo("100");
            assertThat(b.days31to60()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("31 dias vencidos entra al tramo 31-60")
        void treinta_y_un_dias_vencidos_entra_al_tramo_31_60() {
            when(repository.findOutstandingByCompany(1L, null))
                    .thenReturn(List.of(inv("A", AS_OF.minusDays(31), "100")));

            AccountsPayableAgingDto.Bucket b = service.get(1L, null, AS_OF).totals();

            assertThat(b.days1to30()).isEqualByComparingTo("0");
            assertThat(b.days31to60()).isEqualByComparingTo("100");
        }

        @Test
        @DisplayName("60 dias vencidos todavia cae en el tramo 31-60")
        void sesenta_dias_vencidos_cae_en_el_tramo_31_60() {
            when(repository.findOutstandingByCompany(1L, null))
                    .thenReturn(List.of(inv("A", AS_OF.minusDays(60), "100")));

            AccountsPayableAgingDto.Bucket b = service.get(1L, null, AS_OF).totals();

            assertThat(b.days31to60()).isEqualByComparingTo("100");
            assertThat(b.days61to90()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("61 dias vencidos entra al tramo 61-90")
        void sesenta_y_un_dias_vencidos_entra_al_tramo_61_90() {
            when(repository.findOutstandingByCompany(1L, null))
                    .thenReturn(List.of(inv("A", AS_OF.minusDays(61), "100")));

            AccountsPayableAgingDto.Bucket b = service.get(1L, null, AS_OF).totals();

            assertThat(b.days31to60()).isEqualByComparingTo("0");
            assertThat(b.days61to90()).isEqualByComparingTo("100");
        }

        @Test
        @DisplayName("90 dias vencidos todavia cae en el tramo 61-90")
        void noventa_dias_vencidos_cae_en_el_tramo_61_90() {
            when(repository.findOutstandingByCompany(1L, null))
                    .thenReturn(List.of(inv("A", AS_OF.minusDays(90), "100")));

            AccountsPayableAgingDto.Bucket b = service.get(1L, null, AS_OF).totals();

            assertThat(b.days61to90()).isEqualByComparingTo("100");
            assertThat(b.over90()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("91 dias vencidos entra al tramo +90")
        void noventa_y_un_dias_vencidos_entra_al_tramo_mas_90() {
            when(repository.findOutstandingByCompany(1L, null))
                    .thenReturn(List.of(inv("A", AS_OF.minusDays(91), "100")));

            AccountsPayableAgingDto.Bucket b = service.get(1L, null, AS_OF).totals();

            assertThat(b.days61to90()).isEqualByComparingTo("0");
            assertThat(b.over90()).isEqualByComparingTo("100");
        }
    }
}
