package com.vetsoftware.app.supplier.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplier.application.command.CreateSupplierCommand;
import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import com.vetsoftware.app.supplier.domain.SupplierNameAlreadyExistsException;
import com.vetsoftware.app.supplier.testsupport.SupplierMother;
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
@DisplayName("CreateSupplierService")
class CreateSupplierServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final CompanyRef CLINICA = SupplierMother.empresa(COMPANY_ID, "Clinica Norte",
            "900123456");

    @Mock
    private SupplierRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateSupplierService service;

    @Captor
    private ArgumentCaptor<Supplier> supplierCaptor;

    private static CreateSupplierCommand comandoValido() {
        return new CreateSupplierCommand("Distribuidora Sur", "901555444-1", "Marta Gil",
                "3001234567", "compras@sur.test", "Calle 10 # 5-20", 30, "Entrega los martes",
                COMPANY_ID);
    }

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("persiste el proveedor nuevo con la empresa resuelta por el puerto")
        void persiste_el_proveedor_con_la_empresa_resuelta() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CLINICA));
            when(repository.existsByCompanyIdAndName(COMPANY_ID, "Distribuidora Sur"))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoValido());

            verify(repository).save(supplierCaptor.capture());
            Supplier guardado = supplierCaptor.getValue();
            assertThat(guardado.getId()).isNull();
            assertThat(guardado.getName()).isEqualTo("Distribuidora Sur");
            assertThat(guardado.getTaxId()).isEqualTo("901555444-1");
            assertThat(guardado.getCompany()).isEqualTo(CLINICA);
            assertThat(guardado.getPaymentTermsDays()).isEqualTo(30);
            assertThat(guardado.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("devuelve el dto de lo que quedo persistido")
        void devuelve_el_dto_de_lo_persistido() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CLINICA));
            when(repository.existsByCompanyIdAndName(COMPANY_ID, "Distribuidora Sur"))
                    .thenReturn(false);
            when(repository.save(any()))
                    .thenReturn(SupplierMother.completo("Distribuidora Sur", CLINICA));

            SupplierDto dto = service.execute(comandoValido());

            assertThat(dto.name()).isEqualTo("Distribuidora Sur");
            assertThat(dto.company().id()).isEqualTo(COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("no escribe nada si la empresa del comando no existe")
        void no_escribe_nada_si_la_empresa_no_existe() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + COMPANY_ID);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("no guarda si ya existe un proveedor activo con ese nombre en la empresa")
        void no_guarda_si_el_nombre_ya_existe() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CLINICA));
            when(repository.existsByCompanyIdAndName(COMPANY_ID, "Distribuidora Sur"))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(SupplierNameAlreadyExistsException.class)
                    .hasMessageContaining("Distribuidora Sur");

            verify(repository, never()).save(any());
        }
    }
}
