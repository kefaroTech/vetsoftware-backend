package com.vetsoftware.app.withholdingcertificate.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificateNotFoundException;
import com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindWithholdingCertificateService — la consulta por id del tenant")
class FindWithholdingCertificateServiceTest {

    private static final Long ID = 41L;

    @Mock
    private WithholdingCertificateRepository repository;
    @InjectMocks
    private FindWithholdingCertificateService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("devuelve el certificado proyectado a DTO")
        void devuelve_el_certificado_proyectado_a_dto() {
            when(repository.findByIdAndCompanyId(ID, WithholdingCertificateMother.COMPANY_ID))
                    .thenReturn(Optional.of(WithholdingCertificateMother.recibido(ID)));

            WithholdingCertificateDto dto = service.findById(ID,
                    WithholdingCertificateMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(ID);
            assertThat(dto.certificateNumber()).isEqualTo("CERT-2025-0001");
            assertThat(dto.supported()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un certificado inexistente sale como no encontrado con su id en el mensaje")
        void un_certificado_inexistente_sale_como_no_encontrado() {
            when(repository.findByIdAndCompanyId(404L, WithholdingCertificateMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.findById(404L, WithholdingCertificateMother.COMPANY_ID))
                    .isInstanceOf(WithholdingCertificateNotFoundException.class)
                    .hasMessageContaining("Withholding certificate not found: 404");
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("el certificado de otra empresa sale como no encontrado, no como prohibido")
        void el_certificado_de_otra_empresa_sale_como_no_encontrado() {
            // Distinguir «no existe» de «no es tuyo» convertiria el endpoint en un
            // oraculo con el que enumerar los ids ajenos.
            when(repository.findByIdAndCompanyId(ID, WithholdingCertificateMother.OTRA_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.findById(ID, WithholdingCertificateMother.OTRA_COMPANY_ID))
                    .isInstanceOf(WithholdingCertificateNotFoundException.class)
                    .hasMessageContaining("Withholding certificate not found: 41");
        }

        @Test
        @DisplayName("nunca usa la carga ancha: la empresa va siempre en la consulta")
        void nunca_usa_la_carga_ancha() {
            when(repository.findByIdAndCompanyId(ID, WithholdingCertificateMother.COMPANY_ID))
                    .thenReturn(Optional.of(WithholdingCertificateMother.conId(ID)));

            service.findById(ID, WithholdingCertificateMother.COMPANY_ID);

            verify(repository, never()).findById(any());
        }
    }
}
