package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.diagnosticimaging.infrastructure.persistence.DiagnosticImagingJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaDiagnosticImagingChildrenQueryPort (company) — adaptador sobre DiagnosticImagingJpaRepository")
class JpaDiagnosticImagingChildrenQueryPortTest {

    @Mock
    private DiagnosticImagingJpaRepository diagnosticImagingJpaRepository;

    private JpaDiagnosticImagingChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaDiagnosticImagingChildrenQueryPort(diagnosticImagingJpaRepository);
    }

    @Nested
    @DisplayName("existencia de imagenes diagnosticas activas")
    class ExistenciaDeImagenesDiagnosticasActivas {

        @Test
        @DisplayName("delega en el repositorio de imagenes diagnosticas por el id de la empresa")
        void delega_en_el_repositorio_de_imagenes_diagnosticas() {
            when(diagnosticImagingJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin imagenes diagnosticas devuelve falso")
        void una_empresa_sin_imagenes_diagnosticas_devuelve_falso() {
            when(diagnosticImagingJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
