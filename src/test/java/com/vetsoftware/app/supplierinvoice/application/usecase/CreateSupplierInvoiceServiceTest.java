package com.vetsoftware.app.supplierinvoice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplierinvoice.application.command.CreateSupplierInvoiceCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import com.vetsoftware.app.supplierinvoice.application.port.out.BranchQueryPort;
import com.vetsoftware.app.supplierinvoice.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNumberAlreadyExistsException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@DisplayName("CreateSupplierInvoiceService")
class CreateSupplierInvoiceServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long BRANCH_ID = 3L;
    private static final Long SUPPLIER_ID = 7L;
    private static final CompanyRef CO = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    private static final BranchRef BR = new BranchRef(BRANCH_ID, "Sede Centro");
    private static final SupplierRef SUP = new SupplierRef(SUPPLIER_ID, "Distribuidora Sur",
            "800111222");

    @Mock
    private SupplierInvoiceRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private BranchQueryPort branchQueryPort;
    @Mock
    private SupplierQueryPort supplierQueryPort;

    @InjectMocks
    private CreateSupplierInvoiceService service;

    @Captor
    private ArgumentCaptor<SupplierInvoice> captor;

    private CreateSupplierInvoiceCommand comando(BigDecimal retencion) {
        return new CreateSupplierInvoiceCommand(BRANCH_ID, SUPPLIER_ID, null, null, "FAC-001",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9), new BigDecimal("1000000"),
                new BigDecimal("190000"), retencion, "Compra de insumos", COMPANY_ID, 940L);
    }

    private void referenciasResueltas() {
        when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CO));
        when(branchQueryPort.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                .thenReturn(Optional.of(BR));
        when(supplierQueryPort.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                .thenReturn(Optional.of(SUP));
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste la factura con las referencias resueltas y estado PENDING")
        void persiste_la_factura_con_las_referencias_resueltas() {
            referenciasResueltas();
            when(repository.existsByCompanySupplierAndNumber(COMPANY_ID, SUPPLIER_ID, "FAC-001"))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SupplierInvoiceDto dto = service.execute(comando(new BigDecimal("25000")));

            verify(repository).save(captor.capture());
            SupplierInvoice guardada = captor.getValue();
            assertThat(guardada.getCompany()).isEqualTo(CO);
            assertThat(guardada.getBranch()).isEqualTo(BR);
            assertThat(guardada.getSupplier()).isEqualTo(SUP);
            assertThat(guardada.getInvoiceNumber()).isEqualTo("FAC-001");
            assertThat(guardada.getStatus()).isEqualTo(SupplierInvoiceStatus.PENDING);
            assertThat(dto.invoiceNumber()).isEqualTo("FAC-001");
            assertThat(dto.status()).isEqualTo(SupplierInvoiceStatus.PENDING);
        }

        @Test
        @DisplayName("una retencion nula se guarda como cero")
        void una_retencion_nula_se_guarda_como_cero() {
            referenciasResueltas();
            when(repository.existsByCompanySupplierAndNumber(any(), any(), any()))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando(null));

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getWithholdingAmount()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("empresa inexistente aborta antes de resolver sede y proveedor")
        void empresa_inexistente() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(BigDecimal.ZERO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + COMPANY_ID);

            verifyNoInteractions(branchQueryPort, supplierQueryPort, repository);
        }

        @Test
        @DisplayName("sede inexistente en la empresa")
        void sede_inexistente() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CO));
            when(branchQueryPort.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(BigDecimal.ZERO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch not found: " + BRANCH_ID);

            verifyNoInteractions(supplierQueryPort, repository);
        }

        @Test
        @DisplayName("proveedor inexistente en la empresa")
        void proveedor_inexistente() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CO));
            when(branchQueryPort.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(BR));
            when(supplierQueryPort.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(BigDecimal.ZERO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Supplier not found: " + SUPPLIER_ID);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("numero de factura ya registrado para el proveedor")
        void numero_duplicado() {
            referenciasResueltas();
            when(repository.existsByCompanySupplierAndNumber(COMPANY_ID, SUPPLIER_ID, "FAC-001"))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(comando(BigDecimal.ZERO)))
                    .isInstanceOf(SupplierInvoiceNumberAlreadyExistsException.class)
                    .hasMessageContaining("FAC-001");

            verify(repository, never()).save(any());
        }
    }
}
