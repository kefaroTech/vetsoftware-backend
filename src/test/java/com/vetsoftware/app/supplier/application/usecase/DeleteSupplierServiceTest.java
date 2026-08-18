package com.vetsoftware.app.supplier.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import com.vetsoftware.app.supplier.domain.SupplierNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteSupplierService")
class DeleteSupplierServiceTest {

    private static final Long SUPPLIER_ID = 55L;
    private static final Long COMPANY_ID = 10L;
    private static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "900123456");

    @Mock
    private SupplierRepository repository;

    @InjectMocks
    private DeleteSupplierService service;

    private static Supplier existente() {
        return new Supplier(SUPPLIER_ID, "Distribuidora Sur", "901555444-1", "Marta Gil",
                "3001234567", "compras@sur.test", "Calle 10 # 5-20", 30, "Entrega los martes",
                CLINICA, LocalDateTime.of(2026, 1, 15, 10, 30), null, null, 0L, true);
    }

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("soft-deletea el proveedor cuando existe en la empresa")
        void soft_deletea_el_proveedor_existente() {
            when(repository.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente()));

            service.execute(SUPPLIER_ID, COMPANY_ID);

            verify(repository).delete(SUPPLIER_ID);
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("no borra si el proveedor no existe en esa empresa")
        void no_borra_si_no_existe_en_la_empresa() {
            when(repository.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SUPPLIER_ID, COMPANY_ID))
                    .isInstanceOf(SupplierNotFoundException.class)
                    .hasMessageContaining("Supplier not found: " + SUPPLIER_ID);

            verify(repository, never()).delete(SUPPLIER_ID);
        }
    }
}
