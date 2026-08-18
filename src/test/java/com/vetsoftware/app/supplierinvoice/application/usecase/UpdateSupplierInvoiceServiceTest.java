package com.vetsoftware.app.supplierinvoice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplierinvoice.application.command.UpdateSupplierInvoiceCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import com.vetsoftware.app.supplierinvoice.application.port.out.BranchQueryPort;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import com.vetsoftware.app.supplierinvoice.domain.InvalidSupplierInvoiceStateException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNotFoundException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNumberAlreadyExistsException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import com.vetsoftware.app.supplierinvoice.testsupport.SupplierInvoiceMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateSupplierInvoiceService")
class UpdateSupplierInvoiceServiceTest {

    private static final Long INVOICE_ID = 55L;
    private static final Long COMPANY_ID = 1L;
    private static final Long BRANCH_ID = 3L;
    private static final Long SUPPLIER_ID = 7L;
    private static final Long OTRO_SUPPLIER_ID = 8L;
    private static final CompanyRef CO = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    private static final BranchRef BR = new BranchRef(BRANCH_ID, "Sede Centro");
    private static final SupplierRef SUP = new SupplierRef(SUPPLIER_ID, "Distribuidora Sur",
            "800111222");
    private static final SupplierRef OTRO_SUP = new SupplierRef(OTRO_SUPPLIER_ID, "Otro Proveedor",
            "900999888");

    @Mock
    private SupplierInvoiceRepository repository;
    @Mock
    private BranchQueryPort branchQueryPort;
    @Mock
    private SupplierQueryPort supplierQueryPort;

    @InjectMocks
    private UpdateSupplierInvoiceService service;

    @Captor
    private ArgumentCaptor<SupplierInvoice> captor;

    private SupplierInvoice facturaExistente(String numero, SupplierRef proveedor) {
        return SupplierInvoiceMother.nueva(CO, BR, proveedor, numero, LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 2, 9), new BigDecimal("1000000"), new BigDecimal("190000"),
                new BigDecimal("25000"));
    }

    private UpdateSupplierInvoiceCommand comando(String numero, Long supplierId,
            BigDecimal retencion) {
        return new UpdateSupplierInvoiceCommand(INVOICE_ID, BRANCH_ID, supplierId, null, null,
                numero, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                new BigDecimal("1000000"), new BigDecimal("190000"), retencion, "nota", COMPANY_ID,
                940L, 2L);
    }

    private void branchYSupplierResueltos() {
        when(branchQueryPort.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                .thenReturn(Optional.of(BR));
        when(supplierQueryPort.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                .thenReturn(Optional.of(SUP));
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("numero y proveedor sin cambios: no consulta duplicados y guarda")
        void numero_y_proveedor_sin_cambios_no_consulta_duplicados() {
            SupplierInvoice factura = facturaExistente("FAC-001", SUP);
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(factura));
            branchYSupplierResueltos();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando("FAC-001", SUPPLIER_ID, new BigDecimal("25000")));

            verify(repository, never()).existsByCompanySupplierAndNumber(any(), any(), any());
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getVersion()).isEqualTo(2L);
        }

        @Test
        @DisplayName("numero cambia y esta libre: consulta duplicados y guarda")
        void numero_cambia_y_esta_libre() {
            SupplierInvoice factura = facturaExistente("FAC-001", SUP);
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(factura));
            branchYSupplierResueltos();
            when(repository.existsByCompanySupplierAndNumber(COMPANY_ID, SUPPLIER_ID, "FAC-002"))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SupplierInvoiceDto dto = service
                    .execute(comando("FAC-002", SUPPLIER_ID, BigDecimal.ZERO));

            assertThat(dto.invoiceNumber()).isEqualTo("FAC-002");
            verify(repository).save(any());
        }

        @Test
        @DisplayName("una retencion nula se guarda como cero")
        void una_retencion_nula_se_guarda_como_cero() {
            SupplierInvoice factura = facturaExistente("FAC-001", SUP);
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(factura));
            branchYSupplierResueltos();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando("FAC-001", SUPPLIER_ID, null));

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getWithholdingAmount()).isEqualByComparingTo("0");
        }

        /**
         * BE-fix3: {@code SupplierInvoice.update(...)} validaba las notas pero nunca
         * las asignaba al campo. El command de este test manda {@code "nota"} (ver
         * {@link #comando}), distinta de la nota original de {@link #facturaExistente};
         * si la asignacion volviera a faltar, la factura guardada conservaria la nota
         * de creacion en vez de la nueva.
         */
        @Test
        @DisplayName("las notas nuevas del command se persisten en la factura guardada")
        void las_notas_nuevas_se_persisten() {
            SupplierInvoice factura = facturaExistente("FAC-001", SUP);
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(factura));
            branchYSupplierResueltos();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando("FAC-001", SUPPLIER_ID, new BigDecimal("25000")));

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getNotes()).isEqualTo("nota");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("factura inexistente en la empresa")
        void factura_inexistente() {
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(comando("FAC-001", SUPPLIER_ID, BigDecimal.ZERO)))
                    .isInstanceOf(SupplierInvoiceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(INVOICE_ID));

            verifyNoInteractions(branchQueryPort, supplierQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("sede inexistente en la empresa")
        void sede_inexistente() {
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(facturaExistente("FAC-001", SUP)));
            when(branchQueryPort.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(comando("FAC-001", SUPPLIER_ID, BigDecimal.ZERO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch not found: " + BRANCH_ID);

            verifyNoInteractions(supplierQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("proveedor inexistente en la empresa")
        void proveedor_inexistente() {
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(facturaExistente("FAC-001", SUP)));
            when(branchQueryPort.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(BR));
            when(supplierQueryPort.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(comando("FAC-001", SUPPLIER_ID, BigDecimal.ZERO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Supplier not found: " + SUPPLIER_ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("numero cambia y ya existe para el proveedor")
        void numero_cambia_y_ya_existe() {
            SupplierInvoice factura = facturaExistente("FAC-001", SUP);
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(factura));
            branchYSupplierResueltos();
            when(repository.existsByCompanySupplierAndNumber(COMPANY_ID, SUPPLIER_ID, "FAC-002"))
                    .thenReturn(true);

            assertThatThrownBy(
                    () -> service.execute(comando("FAC-002", SUPPLIER_ID, BigDecimal.ZERO)))
                    .isInstanceOf(SupplierInvoiceNumberAlreadyExistsException.class)
                    .hasMessageContaining("FAC-002");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("proveedor cambia y el numero ya existe para el nuevo proveedor")
        void proveedor_cambia_y_numero_choca() {
            SupplierInvoice factura = facturaExistente("FAC-001", SUP);
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(factura));
            when(branchQueryPort.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(BR));
            when(supplierQueryPort.findByIdAndCompanyId(OTRO_SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(OTRO_SUP));
            when(repository.existsByCompanySupplierAndNumber(COMPANY_ID, OTRO_SUPPLIER_ID,
                    "FAC-001")).thenReturn(true);

            assertThatThrownBy(
                    () -> service.execute(comando("FAC-001", OTRO_SUPPLIER_ID, BigDecimal.ZERO)))
                    .isInstanceOf(SupplierInvoiceNumberAlreadyExistsException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una factura con abonos no se puede editar")
        void una_factura_con_abonos_no_se_puede_editar() {
            SupplierInvoice factura = SupplierInvoiceMother.conEstado(CO, BR, SUP, "FAC-001",
                    LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9), new BigDecimal("1000000"),
                    new BigDecimal("190000"), new BigDecimal("25000"),
                    SupplierInvoiceStatus.PARTIAL,
                    List.of(SupplierInvoiceMother.abono(new BigDecimal("100000"),
                            LocalDate.of(2026, 1, 20), SupplierInvoicePaymentMethod.CASH, null)));
            when(repository.findByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(factura));
            branchYSupplierResueltos();

            assertThatThrownBy(
                    () -> service.execute(comando("FAC-001", SUPPLIER_ID, BigDecimal.ZERO)))
                    .isInstanceOf(InvalidSupplierInvoiceStateException.class)
                    .hasMessageContaining("can only be edited while PENDING");

            verify(repository, never()).save(any());
        }
    }
}
