package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyAdminContext;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Las entidades JPA se mockean porque sus constructores sin argumentos son
 * {@code protected} y no son instanciables desde este paquete. No tienen logica
 * propia: son portadoras de datos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyCatalogQueryPort — empresas con rol ADMIN")
class JpaCompanyCatalogQueryPortTest {

    @Mock
    private RoleJpaRepository roleJpaRepository;
    @Mock
    private RoleJpaEntity rolAdmin;
    @Mock
    private CompanyJpaEntity empresa;
    @Mock
    private MembershipJpaEntity membresia;
    @InjectMocks
    private JpaCompanyCatalogQueryPort port;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea cada rol ADMIN a su contexto de empresa")
        void mapea_cada_rol_admin_a_su_contexto() {
            when(rolAdmin.getId()).thenReturn(200L);
            when(rolAdmin.getCompany()).thenReturn(empresa);
            when(empresa.getId()).thenReturn(1L);
            when(empresa.getMembership()).thenReturn(membresia);
            when(membresia.getId()).thenReturn(10L);
            when(roleJpaRepository.findAllByCode("ADMIN")).thenReturn(List.of(rolAdmin));

            List<CompanyAdminContext> resultado = port.findAllWithAdminRole();

            assertThat(resultado).containsExactly(new CompanyAdminContext(1L, 10L, 200L));
        }

        @Test
        @DisplayName("sin roles ADMIN en el catalogo devuelve lista vacia")
        void sin_roles_admin_devuelve_lista_vacia() {
            when(roleJpaRepository.findAllByCode("ADMIN")).thenReturn(List.of());

            assertThat(port.findAllWithAdminRole()).isEmpty();
        }
    }
}
