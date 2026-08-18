package com.vetsoftware.app.service.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.service.application.command.CreateServiceCommand;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.service.application.port.out.ServiceCategoryQueryPort;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.application.port.out.TaxQueryPort;
import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.service.domain.TaxTreatment;
import com.vetsoftware.app.service.testsupport.ServiceMother;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateServiceService")
class CreateServiceServiceTest {

    @Mock
    private ServiceRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private ServiceCategoryQueryPort serviceCategoryQueryPort;
    @Mock
    private TaxQueryPort taxQueryPort;

    @InjectMocks
    private CreateServiceService service;

    private static CreateServiceCommand comandoGravado() {
        return new CreateServiceCommand("Consulta general", new BigDecimal("50000.00"),
                TaxTreatment.GRAVADO, "notas", ServiceMother.SERVICE_CATEGORY_ID,
                ServiceMother.TAX_ID, ServiceMother.COMPANY_ID);
    }

    private static CreateServiceCommand comandoExento() {
        return new CreateServiceCommand("Vacunacion", new BigDecimal("30000.00"),
                TaxTreatment.EXENTO, null, ServiceMother.SERVICE_CATEGORY_ID, null,
                ServiceMother.COMPANY_ID);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("resuelve empresa, categoria e impuesto y guarda el servicio")
        void resuelve_las_tres_referencias_y_guarda_el_servicio() {
            when(companyQueryPort.findById(ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.CLINICA));
            when(serviceCategoryQueryPort.findById(ServiceMother.SERVICE_CATEGORY_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.of(ServiceMother.CONSULTAS));
            when(taxQueryPort.findById(ServiceMother.TAX_ID, ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.IVA_19));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceDto dto = service.execute(comandoGravado());

            ArgumentCaptor<Service> guardado = ArgumentCaptor.forClass(Service.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getName()).isEqualTo("Consulta general");
            assertThat(guardado.getValue().getServiceCategory()).isEqualTo(ServiceMother.CONSULTAS);
            assertThat(guardado.getValue().getTax()).isEqualTo(ServiceMother.IVA_19);
            assertThat(guardado.getValue().getCompany()).isEqualTo(ServiceMother.CLINICA);
            assertThat(dto.name()).isEqualTo("Consulta general");
        }

        @Test
        @DisplayName("un servicio EXENTO sin taxId no consulta el puerto de impuestos")
        void exento_sin_tax_id_no_consulta_impuestos() {
            when(companyQueryPort.findById(ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.CLINICA));
            when(serviceCategoryQueryPort.findById(ServiceMother.SERVICE_CATEGORY_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.of(ServiceMother.CONSULTAS));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoExento());

            verifyNoInteractions(taxQueryPort);
        }
    }

    @Nested
    @DisplayName("referencias inexistentes")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("empresa inexistente no guarda ni consulta las demas referencias")
        void empresa_inexistente_no_guarda() {
            when(companyQueryPort.findById(ServiceMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoGravado()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + ServiceMother.COMPANY_ID);

            verifyNoInteractions(serviceCategoryQueryPort, taxQueryPort, repository);
        }

        @Test
        @DisplayName("categoria inexistente no guarda ni consulta el impuesto")
        void categoria_inexistente_no_guarda() {
            when(companyQueryPort.findById(ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.CLINICA));
            when(serviceCategoryQueryPort.findById(ServiceMother.SERVICE_CATEGORY_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoGravado()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "ServiceCategory not found: " + ServiceMother.SERVICE_CATEGORY_ID);

            verifyNoInteractions(taxQueryPort, repository);
        }

        @Test
        @DisplayName("impuesto inexistente no guarda el servicio")
        void impuesto_inexistente_no_guarda() {
            when(companyQueryPort.findById(ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.CLINICA));
            when(serviceCategoryQueryPort.findById(ServiceMother.SERVICE_CATEGORY_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.of(ServiceMother.CONSULTAS));
            when(taxQueryPort.findById(ServiceMother.TAX_ID, ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoGravado()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tax not found: " + ServiceMother.TAX_ID);

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("validaciones de dominio")
    class Validaciones {

        @Test
        @DisplayName("un GRAVADO sin impuesto no llega a guardarse: lo corta el agregado")
        void gravado_sin_impuesto_no_guarda() {
            when(companyQueryPort.findById(ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.CLINICA));
            when(serviceCategoryQueryPort.findById(ServiceMother.SERVICE_CATEGORY_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.of(ServiceMother.CONSULTAS));
            CreateServiceCommand gravadoSinTax = new CreateServiceCommand("Consulta",
                    BigDecimal.TEN, TaxTreatment.GRAVADO, null, ServiceMother.SERVICE_CATEGORY_ID,
                    null, ServiceMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(gravadoSinTax))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("requires a tax");

            verifyNoInteractions(repository);
        }
    }
}
