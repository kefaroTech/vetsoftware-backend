package com.vetsoftware.app.company.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.out.CompanyCreationPort;
import com.vetsoftware.app.company.application.port.out.InitialContractProvisioningPort;
import com.vetsoftware.app.company.testsupport.CompanyMother;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProvisionCompanyService - empresa y contrato nacen juntos")
class ProvisionCompanyServiceTest {

    @Mock
    private CompanyCreationPort companyCreationPort;
    @Mock
    private InitialContractProvisioningPort initialContractProvisioningPort;

    @InjectMocks
    private ProvisionCompanyService service;

    @Nested
    @DisplayName("Provisionamiento")
    class Provisionamiento {

        @Test
        @DisplayName("crea el contrato inicial despues de crear la empresa y devuelve esa empresa")
        void crea_el_contrato_despues_de_crear_la_empresa() {
            CompanyDto company = CompanyDto.from(CompanyMother.clinicaNorte());
            when(companyCreationPort.create(CompanyMother.comandoCrear())).thenReturn(company);

            CompanyDto result = service.execute(CompanyMother.comandoCrear());

            assertThat(result).isEqualTo(company);
            InOrder order = inOrder(companyCreationPort, initialContractProvisioningPort);
            order.verify(companyCreationPort).create(CompanyMother.comandoCrear());
            order.verify(initialContractProvisioningPort)
                    .provisionForCompany(CompanyMother.COMPANY_ID);
        }

        @Test
        @DisplayName("si falla el contrato propaga el fallo para que la transaccion revierta la empresa")
        void si_falla_el_contrato_propaga_el_fallo() {
            CompanyDto company = CompanyDto.from(CompanyMother.clinicaNorte());
            IllegalStateException failure = new IllegalStateException("catalog not configured");
            when(companyCreationPort.create(CompanyMother.comandoCrear())).thenReturn(company);
            doThrow(failure).when(initialContractProvisioningPort)
                    .provisionForCompany(CompanyMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(CompanyMother.comandoCrear()))
                    .isSameAs(failure).hasMessageContaining("catalog not configured");

            verify(companyCreationPort).create(CompanyMother.comandoCrear());
            verify(initialContractProvisioningPort).provisionForCompany(CompanyMother.COMPANY_ID);
        }
    }

    @Test
    @DisplayName("execute declara la frontera transaccional que revierte ambas operaciones")
    void execute_declara_la_frontera_transaccional() throws NoSuchMethodException {
        Method execute = ProvisionCompanyService.class.getMethod("execute",
                CreateCompanyCommand.class);

        assertThat(AnnotatedElementUtils.hasAnnotation(execute, Transactional.class)).isTrue();
    }
}
