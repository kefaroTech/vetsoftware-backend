package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaRoleChildrenQueryPort (company) — adaptador sobre RoleJpaRepository")
class JpaRoleChildrenQueryPortTest {

    @Mock
    private RoleJpaRepository roleJpaRepository;

    private JpaRoleChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaRoleChildrenQueryPort(roleJpaRepository);
    }

    @Nested
    @DisplayName("existencia de roles activos")
    class ExistenciaDeRolesActivos {

        @Test
        @DisplayName("delega en el repositorio de roles por el id de la empresa")
        void delega_en_el_repositorio_de_roles() {
            when(roleJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID)).thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin roles devuelve falso")
        void una_empresa_sin_roles_devuelve_falso() {
            when(roleJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID)).thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
