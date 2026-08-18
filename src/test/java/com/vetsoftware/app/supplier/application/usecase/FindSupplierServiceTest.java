package com.vetsoftware.app.supplier.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import com.vetsoftware.app.supplier.domain.SupplierNotFoundException;
import com.vetsoftware.app.supplier.testsupport.SupplierMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindSupplierService")
class FindSupplierServiceTest {

    private static final Long SUPPLIER_ID = 55L;
    private static final Long COMPANY_ID = 10L;
    private static final CompanyRef CLINICA = SupplierMother.empresa(COMPANY_ID, "Clinica Norte",
            "900123456");

    @Mock
    private SupplierRepository repository;

    @InjectMocks
    private FindSupplierService service;

    @Nested
    @DisplayName("Consulta")
    class Consulta {

        @Test
        @DisplayName("devuelve el dto del proveedor cuando existe en la empresa")
        void devuelve_el_dto_del_proveedor() {
            Supplier sur = SupplierMother.completo("Distribuidora Sur", CLINICA);
            when(repository.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(sur));

            SupplierDto dto = service.findById(SUPPLIER_ID, COMPANY_ID);

            assertThat(dto.name()).isEqualTo("Distribuidora Sur");
            assertThat(dto.company().id()).isEqualTo(COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("lanza SupplierNotFoundException si no existe en la empresa")
        void lanza_excepcion_si_no_existe_en_la_empresa() {
            when(repository.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(SUPPLIER_ID, COMPANY_ID))
                    .isInstanceOf(SupplierNotFoundException.class)
                    .hasMessageContaining("Supplier not found: " + SUPPLIER_ID);
        }
    }
}
