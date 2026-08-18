package com.vetsoftware.app.supplierinvoice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaRepository;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSupplierQueryPort (supplierinvoice)")
class JpaSupplierQueryPortTest {

    private static final Long SUPPLIER_ID = 7L;
    private static final Long COMPANY_ID = 1L;

    @Mock
    private SupplierJpaRepository supplierJpaRepository;

    @InjectMocks
    private JpaSupplierQueryPort port;

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class FindByIdAndCompanyId {

        @Test
        @DisplayName("mapea el proveedor encontrado, incluido su NIT")
        void mapea_el_proveedor_encontrado() {
            SupplierJpaEntity entidad = mock(SupplierJpaEntity.class);
            when(entidad.getId()).thenReturn(SUPPLIER_ID);
            when(entidad.getName()).thenReturn("Distribuidora Sur");
            when(entidad.getTaxId()).thenReturn("800111222");
            when(supplierJpaRepository.findByIdAndCompany_Id(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            Optional<SupplierRef> ref = port.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID);

            assertThat(ref)
                    .contains(new SupplierRef(SUPPLIER_ID, "Distribuidora Sur", "800111222"));
        }

        @Test
        @DisplayName("devuelve vacio si el proveedor no existe en la empresa")
        void devuelve_vacio_si_no_existe() {
            when(supplierJpaRepository.findByIdAndCompany_Id(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID)).isEmpty();
        }
    }
}
