package com.vetsoftware.app.product.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.product.domain.SupplierRef;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSupplierQueryPort (product)")
class JpaSupplierQueryPortTest {

    private static final Long SUPPLIER_ID = 6L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private SupplierJpaRepository supplierJpaRepository;

    @InjectMocks
    private JpaSupplierQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea el proveedor encontrado en la empresa a su SupplierRef")
        void mapea_el_proveedor_encontrado() {
            SupplierJpaEntity entidad = mock(SupplierJpaEntity.class);
            when(entidad.getId()).thenReturn(SUPPLIER_ID);
            when(entidad.getName()).thenReturn("Distribuidora Sur");
            when(supplierJpaRepository.findByIdAndCompany_Id(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            Optional<SupplierRef> ref = port.findById(SUPPLIER_ID, COMPANY_ID);

            assertThat(ref).contains(new SupplierRef(SUPPLIER_ID, "Distribuidora Sur"));
        }

        @Test
        @DisplayName("devuelve vacio si el proveedor no existe en esa empresa")
        void devuelve_vacio_si_no_existe_en_la_empresa() {
            when(supplierJpaRepository.findByIdAndCompany_Id(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findById(SUPPLIER_ID, COMPANY_ID)).isEmpty();
        }
    }
}
