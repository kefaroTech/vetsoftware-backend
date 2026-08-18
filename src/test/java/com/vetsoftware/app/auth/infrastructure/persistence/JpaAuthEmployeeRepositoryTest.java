package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository.AuthEmployee;
import com.vetsoftware.app.auth.testsupport.ReflectionEntities;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Envuelve {@code EmployeeJpaRepository} (Spring Data, de la feature
 * {@code employee}). Su constructor protegido —correcto para su propio paquete—
 * es inaccesible desde este: la entidad se construye por reflexión (ver
 * {@code ReflectionEntities}), no se dobla.
 */
@ExtendWith(MockitoExtension.class)
class JpaAuthEmployeeRepositoryTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;
    @InjectMocks
    private JpaAuthEmployeeRepository repository;

    private static EmployeeJpaEntity entidad(Long id, Long companyId, long authVersion)
            throws ReflectiveOperationException {
        CompanyJpaEntity company = ReflectionEntities.newInstance(CompanyJpaEntity.class);
        company.setId(companyId);
        EmployeeJpaEntity entity = ReflectionEntities.newInstance(EmployeeJpaEntity.class);
        entity.setId(id);
        entity.setCompany(company);
        entity.setAuthVersion(authVersion);
        return entity;
    }

    @Nested
    @DisplayName("findActiveById")
    class BuscarActivo {

        @Test
        @DisplayName("mapea la fila activa con su empresa y versión")
        void mapea_la_fila_activa() throws Exception {
            when(employeeJpaRepository.findActiveWithCompanyById(7L))
                    .thenReturn(Optional.of(entidad(7L, 3L, 5L)));

            Optional<AuthEmployee> result = repository.findActiveById(7L);

            assertThat(result).contains(new AuthEmployee(7L, 3L, 5L));
        }

        @Test
        @DisplayName("un empleado desactivado o inexistente no aparece")
        void empleado_desactivado_no_aparece() {
            when(employeeJpaRepository.findActiveWithCompanyById(7L)).thenReturn(Optional.empty());

            assertThat(repository.findActiveById(7L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("rotateAuthVersion")
    class RotarVersion {

        @Test
        @DisplayName("sube la versión en uno, la persiste y devuelve la ya rotada")
        void sube_la_version_y_la_persiste() throws Exception {
            EmployeeJpaEntity entity = entidad(7L, 3L, 4L);
            when(employeeJpaRepository.findActiveWithCompanyByIdForUpdate(7L))
                    .thenReturn(Optional.of(entity));

            Optional<AuthEmployee> result = repository.rotateAuthVersion(7L);

            assertThat(result).contains(new AuthEmployee(7L, 3L, 5L));
            assertThat(entity.getAuthVersion()).isEqualTo(5L);
            verify(employeeJpaRepository).saveAndFlush(entity);
        }

        @Test
        @DisplayName("sin fila activa bajo lock, no rota ni persiste nada")
        void sin_fila_activa_no_rota_nada() {
            when(employeeJpaRepository.findActiveWithCompanyByIdForUpdate(7L))
                    .thenReturn(Optional.empty());

            assertThat(repository.rotateAuthVersion(7L)).isEmpty();
            verify(employeeJpaRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
        }
    }

    @Test
    @DisplayName("bumpAuthVersion delega en el UPDATE nativo del repositorio")
    void bump_auth_version_delega_en_el_update_nativo() {
        repository.bumpAuthVersion(7L);

        verify(employeeJpaRepository).bumpAuthVersion(7L);
    }

    @Test
    @DisplayName("bumpAuthVersion acotado delega en el UPDATE con AND company_id")
    void bump_auth_version_acotado_delega_en_el_update_acotado() {
        // La sobrecarga acotada es la del logout; la ancha se queda para el refresh,
        // donde no hay empresa en el contexto. Son dos caminos declarados, no uno.
        repository.bumpAuthVersion(7L, 3L);

        verify(employeeJpaRepository).bumpAuthVersion(7L, 3L);
        verify(employeeJpaRepository, never()).bumpAuthVersion(7L);
    }
}
