package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaOwnerChildrenQueryPort (company) — adaptador sobre OwnerJpaRepository")
class JpaOwnerChildrenQueryPortTest {

    @Mock
    private OwnerJpaRepository ownerJpaRepository;

    private JpaOwnerChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaOwnerChildrenQueryPort(ownerJpaRepository);
    }

    @Nested
    @DisplayName("existencia de propietarios activos")
    class ExistenciaDePropietariosActivos {

        @Test
        @DisplayName("delega en el repositorio de propietarios por el id de la empresa")
        void delega_en_el_repositorio_de_propietarios() {
            when(ownerJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID)).thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin propietarios devuelve falso")
        void una_empresa_sin_propietarios_devuelve_falso() {
            when(ownerJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID)).thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
