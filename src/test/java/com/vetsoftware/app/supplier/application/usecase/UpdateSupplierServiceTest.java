package com.vetsoftware.app.supplier.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplier.application.command.UpdateSupplierCommand;
import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import com.vetsoftware.app.supplier.domain.SupplierNameAlreadyExistsException;
import com.vetsoftware.app.supplier.domain.SupplierNotFoundException;
import java.time.LocalDateTime;
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
@DisplayName("UpdateSupplierService")
class UpdateSupplierServiceTest {

    private static final Long SUPPLIER_ID = 55L;
    private static final Long COMPANY_ID = 10L;
    private static final Long EMPLOYEE_ID = 3L;
    private static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "900123456");

    @Mock
    private SupplierRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private UpdateSupplierService service;

    @Captor
    private ArgumentCaptor<Supplier> supplierCaptor;

    private static Supplier existente() {
        return new Supplier(SUPPLIER_ID, "Distribuidora Sur", "901555444-1", "Marta Gil",
                "3001234567", "compras@sur.test", "Calle 10 # 5-20", 30, "Entrega los martes",
                CLINICA, LocalDateTime.of(2026, 1, 15, 10, 30), null, null, 3L, true);
    }

    private static UpdateSupplierCommand comandoValido() {
        return new UpdateSupplierCommand(SUPPLIER_ID, "Distribuidora Sur Actualizada",
                "901555444-1", "Marta Gil", "3001234567", "compras@sur.test", "Calle 10 # 5-20", 30,
                "Entrega los martes", COMPANY_ID, EMPLOYEE_ID, 3L);
    }

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("actualiza el proveedor existente con la empresa resuelta por el puerto")
        void actualiza_el_proveedor_existente() {
            when(repository.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente()));
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CLINICA));
            when(repository.existsByCompanyIdAndNameExcludingId(COMPANY_ID,
                    "Distribuidora Sur Actualizada", SUPPLIER_ID)).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoValido());

            verify(repository).save(supplierCaptor.capture());
            Supplier guardado = supplierCaptor.getValue();
            assertThat(guardado.getId()).isEqualTo(SUPPLIER_ID);
            assertThat(guardado.getName()).isEqualTo("Distribuidora Sur Actualizada");
            assertThat(guardado.getUpdatedBy()).isEqualTo(EMPLOYEE_ID);
            assertThat(guardado.getVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("devuelve el dto de lo que quedo persistido")
        void devuelve_el_dto_de_lo_persistido() {
            when(repository.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente()));
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CLINICA));
            when(repository.existsByCompanyIdAndNameExcludingId(COMPANY_ID,
                    "Distribuidora Sur Actualizada", SUPPLIER_ID)).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SupplierDto dto = service.execute(comandoValido());

            assertThat(dto.id()).isEqualTo(SUPPLIER_ID);
            assertThat(dto.name()).isEqualTo("Distribuidora Sur Actualizada");
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("no consulta la empresa ni escribe si el proveedor no existe en esa empresa")
        void no_hace_nada_si_el_proveedor_no_existe() {
            when(repository.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(SupplierNotFoundException.class)
                    .hasMessageContaining("Supplier not found: " + SUPPLIER_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no guarda si la empresa del comando no existe")
        void no_guarda_si_la_empresa_no_existe() {
            when(repository.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente()));
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + COMPANY_ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no guarda si el nuevo nombre ya lo usa otro proveedor de la empresa")
        void no_guarda_si_el_nombre_ya_lo_usa_otro() {
            when(repository.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente()));
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CLINICA));
            when(repository.existsByCompanyIdAndNameExcludingId(COMPANY_ID,
                    "Distribuidora Sur Actualizada", SUPPLIER_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(SupplierNameAlreadyExistsException.class)
                    .hasMessageContaining("Distribuidora Sur Actualizada");

            verify(repository, never()).save(any());
        }
    }
}
