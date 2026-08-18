package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.port.out.EmployeeCredentialsRepository.EmployeeCredentials;
import com.vetsoftware.app.auth.testsupport.ReflectionEntities;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaEmployeeCredentialsRepositoryTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;
    @InjectMocks
    private JpaEmployeeCredentialsRepository repository;

    @Test
    @DisplayName("mapea el hash de clave y el estado de verificación de correo por código")
    void mapea_hash_y_verificacion_de_correo() throws Exception {
        CompanyJpaEntity company = ReflectionEntities.newInstance(CompanyJpaEntity.class);
        company.setId(3L);
        EmployeeJpaEntity entity = ReflectionEntities.newInstance(EmployeeJpaEntity.class);
        entity.setId(7L);
        entity.setCompany(company);
        entity.setAuthVersion(4L);
        entity.setHashPassword("hash");
        entity.setEmailVerified(false);
        when(employeeJpaRepository.findByEmployeeCode("EMP-1")).thenReturn(Optional.of(entity));

        assertThat(repository.findByCode("EMP-1"))
                .contains(new EmployeeCredentials(7L, 3L, 4L, "hash", false));
    }

    @Test
    @DisplayName("un código inexistente no aparece")
    void codigo_inexistente_no_aparece() {
        when(employeeJpaRepository.findByEmployeeCode("NO-EXISTE")).thenReturn(Optional.empty());

        assertThat(repository.findByCode("NO-EXISTE")).isEmpty();
    }
}
