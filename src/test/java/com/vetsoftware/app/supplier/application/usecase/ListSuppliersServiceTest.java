package com.vetsoftware.app.supplier.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import com.vetsoftware.app.supplier.testsupport.SupplierMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSuppliersService")
class ListSuppliersServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final CompanyRef CLINICA = SupplierMother.empresa(COMPANY_ID, "Clinica Norte",
            "900123456");

    @Mock
    private SupplierRepository repository;

    @InjectMocks
    private ListSuppliersService service;

    @Nested
    @DisplayName("listByCompany")
    class ListaActivos {

        @Test
        @DisplayName("mapea cada proveedor activo de la empresa a su dto")
        void mapea_cada_proveedor_activo_a_su_dto() {
            Supplier sur = SupplierMother.completo("Distribuidora Sur", CLINICA);
            when(repository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(sur));

            List<SupplierDto> resultado = service.listByCompany(COMPANY_ID);

            assertThat(resultado).extracting(SupplierDto::name)
                    .containsExactly("Distribuidora Sur");
        }

        @Test
        @DisplayName("una empresa sin proveedores devuelve una lista vacia")
        void una_empresa_sin_proveedores_devuelve_lista_vacia() {
            when(repository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of());

            assertThat(service.listByCompany(COMPANY_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("listDisabledByCompany")
    class ListaPausados {

        @Test
        @DisplayName("mapea cada proveedor pausado de la empresa a su dto")
        void mapea_cada_proveedor_pausado_a_su_dto() {
            Supplier norte = SupplierMother.completo("Insumos Norte", CLINICA);
            when(repository.findAllDisabledByCompanyId(COMPANY_ID)).thenReturn(List.of(norte));

            List<SupplierDto> resultado = service.listDisabledByCompany(COMPANY_ID);

            assertThat(resultado).extracting(SupplierDto::name).containsExactly("Insumos Norte");
        }
    }
}
