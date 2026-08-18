package com.vetsoftware.app.animalalert.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalalert.domain.CompanyRef;
import com.vetsoftware.app.animalalert.testsupport.AnimalAlertMother;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private CompanyJpaEntity companyEntity;

    @InjectMocks
    private JpaCompanyQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea la fila encontrada a CompanyRef")
        void mapea_la_fila_encontrada_a_company_ref() {
            when(companyJpaRepository.findById(AnimalAlertMother.COMPANY_ID))
                    .thenReturn(Optional.of(companyEntity));
            when(companyEntity.getId()).thenReturn(AnimalAlertMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(AnimalAlertMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(AnimalAlertMother.CLINICA.identifier());

            Optional<CompanyRef> found = port.findById(AnimalAlertMother.COMPANY_ID);

            assertThat(found).contains(AnimalAlertMother.CLINICA);
        }

        @Test
        @DisplayName("una empresa inexistente devuelve vacio")
        void una_empresa_inexistente_devuelve_vacio() {
            when(companyJpaRepository.findById(AnimalAlertMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findById(AnimalAlertMother.COMPANY_ID)).isEmpty();
        }
    }
}
