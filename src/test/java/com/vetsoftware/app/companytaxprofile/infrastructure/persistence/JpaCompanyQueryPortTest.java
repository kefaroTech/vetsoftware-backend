package com.vetsoftware.app.companytaxprofile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.companytaxprofile.domain.CompanyRef;
import com.vetsoftware.app.companytaxprofile.testsupport.ReflectionEntities;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort (companytaxprofile) — arma el CompanyRef desde CompanyJpaEntity")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;

    private JpaCompanyQueryPort port;

    @Test
    @DisplayName("empresa existente: mapea id, nombre e identificador")
    void empresa_existente_mapea_id_nombre_e_identificador() throws Exception {
        port = new JpaCompanyQueryPort(companyJpaRepository);
        CompanyJpaEntity entity = ReflectionEntities.newInstance(CompanyJpaEntity.class);
        entity.setId(9L);
        entity.setName("Clinica Norte");
        entity.setIdentifier("900123456-8");
        when(companyJpaRepository.findById(9L)).thenReturn(Optional.of(entity));

        Optional<CompanyRef> result = port.findById(9L);

        assertThat(result).contains(new CompanyRef(9L, "Clinica Norte", "900123456-8"));
    }

    @Test
    @DisplayName("empresa inexistente devuelve vacio")
    void empresa_inexistente_devuelve_vacio() {
        port = new JpaCompanyQueryPort(companyJpaRepository);
        when(companyJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
