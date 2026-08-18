package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.electronicdocument.testsupport.ReflectionEntities;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBillingEntitlementQueryPort — resuelve el derecho a facturacion electronica")
class JpaBillingEntitlementQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private MembershipSubModuleJpaRepository membershipSubModuleJpaRepository;

    private JpaBillingEntitlementQueryPort port;

    @BeforeEach
    void montar() {
        port = new JpaBillingEntitlementQueryPort(companyJpaRepository,
                membershipSubModuleJpaRepository);
    }

    private static MembershipJpaEntity membresia(Long id) throws Exception {
        MembershipJpaEntity membership = ReflectionEntities.newInstance(MembershipJpaEntity.class);
        Field idField = MembershipJpaEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(membership, id);
        return membership;
    }

    private static CompanyJpaEntity empresaConMembresia(MembershipJpaEntity membership)
            throws Exception {
        CompanyJpaEntity company = ReflectionEntities.newInstance(CompanyJpaEntity.class);
        company.setMembership(membership);
        return company;
    }

    @Test
    @DisplayName("companyId null nunca esta habilitada, sin consultar nada")
    void company_id_null_nunca_esta_habilitada() {
        assertThat(port.isElectronicInvoicingEnabled(null)).isFalse();

        verifyNoInteractions(companyJpaRepository, membershipSubModuleJpaRepository);
    }

    @Test
    @DisplayName("una empresa inexistente no esta habilitada")
    void empresa_inexistente_no_esta_habilitada() {
        when(companyJpaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(port.isElectronicInvoicingEnabled(1L)).isFalse();
    }

    @Nested
    @DisplayName("empresa existente — depende del submodulo BILLING de su membresia")
    class EmpresaExistente {

        @Test
        @DisplayName("con el submodulo BILLING habilitado en la membresia, esta habilitada")
        void con_billing_habilitado() throws Exception {
            CompanyJpaEntity company = empresaConMembresia(membresia(3L));
            when(companyJpaRepository.findById(9L)).thenReturn(Optional.of(company));
            when(membershipSubModuleJpaRepository.hasEnabledSubModuleCode(3L, "BILLING"))
                    .thenReturn(true);

            assertThat(port.isElectronicInvoicingEnabled(9L)).isTrue();
        }

        @Test
        @DisplayName("sin el submodulo BILLING en la membresia, no esta habilitada")
        void sin_billing_habilitado() throws Exception {
            CompanyJpaEntity company = empresaConMembresia(membresia(4L));
            when(companyJpaRepository.findById(9L)).thenReturn(Optional.of(company));
            when(membershipSubModuleJpaRepository.hasEnabledSubModuleCode(4L, "BILLING"))
                    .thenReturn(false);

            assertThat(port.isElectronicInvoicingEnabled(9L)).isFalse();
        }
    }
}
