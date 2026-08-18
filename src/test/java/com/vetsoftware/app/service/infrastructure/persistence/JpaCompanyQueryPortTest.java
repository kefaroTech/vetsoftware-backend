package com.vetsoftware.app.service.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.service.domain.CompanyRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort (service)")
class JpaCompanyQueryPortTest {

    private static final Long COMPANY_ID = 9L;

    @Mock
    private CompanyJpaRepository companyJpaRepository;

    @InjectMocks
    private JpaCompanyQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea la empresa encontrada a su CompanyRef")
        void mapea_la_empresa_encontrada() {
            CompanyJpaEntity entidad = mock(CompanyJpaEntity.class);
            when(entidad.getId()).thenReturn(COMPANY_ID);
            when(entidad.getName()).thenReturn("Veterinaria de prueba");
            when(entidad.getIdentifier()).thenReturn("900123456");
            when(companyJpaRepository.findById(COMPANY_ID)).thenReturn(Optional.of(entidad));

            Optional<CompanyRef> ref = port.findById(COMPANY_ID);

            assertThat(ref)
                    .contains(new CompanyRef(COMPANY_ID, "Veterinaria de prueba", "900123456"));
        }

        @Test
        @DisplayName("devuelve vacio si la empresa no existe")
        void devuelve_vacio_si_no_existe() {
            when(companyJpaRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

            assertThat(port.findById(COMPANY_ID)).isEmpty();
        }
    }
}
