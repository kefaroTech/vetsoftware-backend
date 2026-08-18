package com.vetsoftware.app.daycare.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.daycare.domain.CompanyRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort (daycare)")
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
        @DisplayName("una empresa existente se mapea a su companion VO")
        void empresa_existente_se_mapea_a_company_ref() {
            when(companyJpaRepository.findById(10L)).thenReturn(Optional.of(companyEntity));
            when(companyEntity.getId()).thenReturn(10L);
            when(companyEntity.getName()).thenReturn("Veterinaria de prueba");
            when(companyEntity.getIdentifier()).thenReturn("900123456");

            Optional<CompanyRef> encontrado = port.findById(10L);

            assertThat(encontrado)
                    .contains(new CompanyRef(10L, "Veterinaria de prueba", "900123456"));
        }

        @Test
        @DisplayName("una empresa inexistente devuelve vacio")
        void empresa_inexistente_devuelve_vacio() {
            when(companyJpaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(port.findById(99L)).isEmpty();
        }
    }
}
