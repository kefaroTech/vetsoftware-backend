package com.vetsoftware.app.appointment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort (appointment) — adaptador sobre CompanyJpaRepository")
class JpaCompanyQueryPortTest {

    private static final Long COMPANY_ID = 9L;

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @InjectMocks
    private JpaCompanyQueryPort port;

    @Test
    @DisplayName("devuelve el nombre de la empresa encontrada")
    void devuelve_el_nombre_de_la_empresa_encontrada() {
        CompanyJpaEntity entity = mock(CompanyJpaEntity.class);
        when(entity.getName()).thenReturn("Clinica Norte");
        when(companyJpaRepository.findById(COMPANY_ID)).thenReturn(Optional.of(entity));

        assertThat(port.findNameById(COMPANY_ID)).contains("Clinica Norte");
    }

    @Test
    @DisplayName("una empresa inexistente devuelve vacio")
    void una_empresa_inexistente_devuelve_vacio() {
        when(companyJpaRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThat(port.findNameById(COMPANY_ID)).isEmpty();
    }
}
