package com.vetsoftware.app.servicecategory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.servicecategory.domain.CompanyRef;
import com.vetsoftware.app.servicecategory.testsupport.ServiceCategoryMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort — adaptador sobre CompanyJpaRepository")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private CompanyJpaEntity companyEntity;

    private JpaCompanyQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaCompanyQueryPort(companyJpaRepository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la entidad encontrada a su companion VO")
        void mapea_la_entidad_encontrada_a_su_companion_vo() {
            when(companyEntity.getId()).thenReturn(ServiceCategoryMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(ServiceCategoryMother.CLINICA.name());
            when(companyEntity.getIdentifier())
                    .thenReturn(ServiceCategoryMother.CLINICA.identifier());
            when(companyJpaRepository.findById(ServiceCategoryMother.COMPANY_ID))
                    .thenReturn(Optional.of(companyEntity));

            Optional<CompanyRef> resultado = port.findById(ServiceCategoryMother.COMPANY_ID);

            assertThat(resultado).contains(ServiceCategoryMother.CLINICA);
        }

        @Test
        @DisplayName("una compania inexistente devuelve vacio")
        void una_compania_inexistente_devuelve_vacio() {
            when(companyJpaRepository.findById(ServiceCategoryMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            Optional<CompanyRef> resultado = port.findById(ServiceCategoryMother.COMPANY_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
