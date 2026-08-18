package com.vetsoftware.app.service.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.service.application.command.UpdateServiceCommand;
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
@DisplayName("UpdateServiceService")
class UpdateServiceServiceTest {

    @Mock
    private ServiceRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private ServiceCategoryQueryPort serviceCategoryQueryPort;
    @Mock
    private TaxQueryPort taxQueryPort;

    @InjectMocks
    private UpdateServiceService service;

    private static UpdateServiceCommand comando() {
        return new UpdateServiceCommand(ServiceMother.SERVICE_ID, "Consulta actualizada",
                new BigDecimal("60000.00"), TaxTreatment.GRAVADO, "notas nuevas",
                ServiceMother.SERVICE_CATEGORY_ID, ServiceMother.TAX_ID, ServiceMother.COMPANY_ID,
                77L, 0L);
    }

    private static UpdateServiceCommand comandoSinImpuesto() {
        return new UpdateServiceCommand(ServiceMother.SERVICE_ID, "Consulta exenta",
                new BigDecimal("40000.00"), TaxTreatment.EXENTO, "notas nuevas",
                ServiceMother.SERVICE_CATEGORY_ID, null, ServiceMother.COMPANY_ID, 77L, 0L);
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza el agregado encontrado con las tres referencias resueltas")
        void actualiza_el_agregado_con_las_referencias_resueltas() {
            Service existente = ServiceMother.consultaGeneral();
            when(repository.findByIdAndCompanyId(ServiceMother.SERVICE_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            when(companyQueryPort.findById(ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.CLINICA));
            when(serviceCategoryQueryPort.findById(ServiceMother.SERVICE_CATEGORY_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.of(ServiceMother.CONSULTAS));
            when(taxQueryPort.findById(ServiceMother.TAX_ID, ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.IVA_19));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceDto dto = service.execute(comando());

            ArgumentCaptor<Service> guardado = ArgumentCaptor.forClass(Service.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue()).isSameAs(existente);
            assertThat(guardado.getValue().getName()).isEqualTo("Consulta actualizada");
            assertThat(guardado.getValue().getPrice()).isEqualByComparingTo("60000.00");
            assertThat(guardado.getValue().getUpdatedBy()).isEqualTo(77L);
            assertThat(dto.name()).isEqualTo("Consulta actualizada");
        }

        @Test
        @DisplayName("con taxId nulo no consulta el puerto de impuestos y guarda el agregado sin tax")
        void con_tax_id_nulo_no_consulta_el_puerto_de_impuestos() {
            Service existente = ServiceMother.consultaGeneral();
            when(repository.findByIdAndCompanyId(ServiceMother.SERVICE_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            when(companyQueryPort.findById(ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.CLINICA));
            when(serviceCategoryQueryPort.findById(ServiceMother.SERVICE_CATEGORY_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.of(ServiceMother.CONSULTAS));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceDto dto = service.execute(comandoSinImpuesto());

            ArgumentCaptor<Service> guardado = ArgumentCaptor.forClass(Service.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getTax()).isNull();
            assertThat(guardado.getValue().getTaxTreatment()).isEqualTo(TaxTreatment.EXENTO);
            assertThat(dto.name()).isEqualTo("Consulta exenta");
            verifyNoInteractions(taxQueryPort);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("servicio inexistente no consulta ninguna referencia ni guarda")
        void servicio_inexistente_no_consulta_ni_guarda() {
            when(repository.findByIdAndCompanyId(ServiceMother.SERVICE_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(com.vetsoftware.app.service.domain.ServiceNotFoundException.class)
                    .hasMessageContaining("Service not found: " + ServiceMother.SERVICE_ID);

            verifyNoInteractions(companyQueryPort, serviceCategoryQueryPort, taxQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("empresa inexistente no guarda")
        void empresa_inexistente_no_guarda() {
            when(repository.findByIdAndCompanyId(ServiceMother.SERVICE_ID,
                    ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.consultaGeneral()));
            when(companyQueryPort.findById(ServiceMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + ServiceMother.COMPANY_ID);

            verifyNoInteractions(serviceCategoryQueryPort, taxQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("categoria inexistente no guarda")
        void categoria_inexistente_no_guarda() {
            when(repository.findByIdAndCompanyId(ServiceMother.SERVICE_ID,
                    ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.consultaGeneral()));
            when(companyQueryPort.findById(ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.CLINICA));
            when(serviceCategoryQueryPort.findById(ServiceMother.SERVICE_CATEGORY_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "ServiceCategory not found: " + ServiceMother.SERVICE_CATEGORY_ID);

            verifyNoInteractions(taxQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("impuesto inexistente no guarda")
        void impuesto_inexistente_no_guarda() {
            when(repository.findByIdAndCompanyId(ServiceMother.SERVICE_ID,
                    ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.consultaGeneral()));
            when(companyQueryPort.findById(ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.CLINICA));
            when(serviceCategoryQueryPort.findById(ServiceMother.SERVICE_CATEGORY_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.of(ServiceMother.CONSULTAS));
            when(taxQueryPort.findById(ServiceMother.TAX_ID, ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tax not found: " + ServiceMother.TAX_ID);

            verify(repository, never()).save(any());
        }
    }
}
