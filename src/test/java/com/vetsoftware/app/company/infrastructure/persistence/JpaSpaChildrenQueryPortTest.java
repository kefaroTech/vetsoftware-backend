package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.spa.infrastructure.persistence.SpaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSpaChildrenQueryPort (company) — adaptador sobre SpaJpaRepository")
class JpaSpaChildrenQueryPortTest {

    @Mock
    private SpaJpaRepository spaJpaRepository;

    private JpaSpaChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaSpaChildrenQueryPort(spaJpaRepository);
    }

    @Nested
    @DisplayName("existencia de servicios de spa activos")
    class ExistenciaDeServiciosDeSpaActivos {

        @Test
        @DisplayName("delega en el repositorio de spa por el id de la empresa")
        void delega_en_el_repositorio_de_spa() {
            when(spaJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID)).thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin servicios de spa devuelve falso")
        void una_empresa_sin_servicios_de_spa_devuelve_falso() {
            when(spaJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID)).thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
