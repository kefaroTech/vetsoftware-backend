package com.vetsoftware.app.supplier.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplier.application.dto.SupplierDto;
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
@DisplayName("ReactivateSupplierService")
class ReactivateSupplierServiceTest {

    private static final Long SUPPLIER_ID = 55L;
    private static final Long COMPANY_ID = 10L;
    private static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "900123456");

    @Mock
    private SupplierRepository repository;

    @InjectMocks
    private ReactivateSupplierService service;

    private static Supplier reactivado() {
        return new Supplier(SUPPLIER_ID, "Distribuidora Sur", "901555444-1", "Marta Gil",
                "3001234567", "compras@sur.test", "Calle 10 # 5-20", 30, "Entrega los martes",
                CLINICA, LocalDateTime.of(2026, 1, 15, 10, 30), null, null, 0L, true);
    }

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("devuelve el proveedor reactivado cuando el UPDATE afecta una fila")
        void devuelve_el_proveedor_reactivado() {
            when(repository.reactivate(SUPPLIER_ID, COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(reactivado()));

            SupplierDto dto = service.execute(SUPPLIER_ID, COMPANY_ID);

            assertThat(dto.id()).isEqualTo(SUPPLIER_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("lanza SupplierNotFoundException si el UPDATE no afecto ninguna fila")
        void lanza_excepcion_si_no_afecta_ninguna_fila() {
            when(repository.reactivate(SUPPLIER_ID, COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(SUPPLIER_ID, COMPANY_ID))
                    .isInstanceOf(SupplierNotFoundException.class)
                    .hasMessageContaining("Supplier not found: " + SUPPLIER_ID);

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }

        @Test
        @DisplayName("lanza SupplierNotFoundException si la relectura tras reactivar no encuentra la fila")
        void lanza_excepcion_si_la_relectura_no_encuentra_la_fila() {
            when(repository.reactivate(SUPPLIER_ID, COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(SUPPLIER_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SUPPLIER_ID, COMPANY_ID))
                    .isInstanceOf(SupplierNotFoundException.class)
                    .hasMessageContaining("Supplier not found: " + SUPPLIER_ID);
        }
    }
}
