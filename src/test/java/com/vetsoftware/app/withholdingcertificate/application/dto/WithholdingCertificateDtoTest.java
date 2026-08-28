package com.vetsoftware.app.withholdingcertificate.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.withholdingcertificate.domain.SubstituteEvidenceKind;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingType;
import com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La proyeccion, campo por campo. Sin mocks: un DTO se construye de verdad o no
 * se esta probando la traduccion sino el doble.
 */
@DisplayName("WithholdingCertificateDto — la proyeccion del certificado")
class WithholdingCertificateDtoTest {

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("copia los dieciseis campos del certificado sin cruzarlos")
        void copia_los_campos_sin_cruzarlos() {
            WithholdingCertificateDto dto = WithholdingCertificateDto
                    .from(WithholdingCertificateMother.conId(41L));

            assertThat(dto.id()).isEqualTo(41L);
            assertThat(dto.companyId()).isEqualTo(WithholdingCertificateMother.COMPANY_ID);
            assertThat(dto.issuedByTaxId()).isEqualTo(WithholdingCertificateMother.NIT_DEL_CLIENTE);
            assertThat(dto.certificateNumber()).isEqualTo("CERT-2025-0001");
            assertThat(dto.withholdingType()).isEqualTo(WithholdingType.INCOME_TAX);
            assertThat(dto.fiscalYear()).isEqualTo(WithholdingCertificateMother.ANO_GRAVABLE);
            assertThat(dto.fiscalPeriodKey()).isEqualTo("2025-A");
            assertThat(dto.ratePercent())
                    .isEqualByComparingTo(WithholdingCertificateMother.TARIFA_RENTA);
            assertThat(dto.certifiedAmount())
                    .isEqualByComparingTo(WithholdingCertificateMother.IMPORTE_CERTIFICADO);
            // Las tres fechas son distintas: un cruce entre expedicion y vencimiento
            // pasaria desapercibido con la misma fecha en las dos.
            assertThat(dto.issuedOn()).isEqualTo(WithholdingCertificateMother.EXPEDIDO_EL);
            assertThat(dto.legalDeadlineOn()).isEqualTo(WithholdingCertificateMother.VENCE_EL);
            assertThat(dto.createdDate()).isEqualTo(WithholdingCertificateMother.CREADO_EL);
            assertThat(dto.receivedOn()).isNull();
            assertThat(dto.fileRef()).isNull();
            assertThat(dto.substituteEvidenceKind()).isNull();
            assertThat(dto.substituteEvidenceRef()).isNull();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un certificado sin llegar y sin sustituto no esta acreditado")
        void un_certificado_sin_llegar_y_sin_sustituto_no_esta_acreditado() {
            assertThat(WithholdingCertificateDto.from(WithholdingCertificateMother.deRenta())
                    .supported()).isFalse();
        }

        @Test
        @DisplayName("el certificado recibido esta acreditado por el papel")
        void el_certificado_recibido_esta_acreditado_por_el_papel() {
            WithholdingCertificateDto dto = WithholdingCertificateDto
                    .from(WithholdingCertificateMother.recibido(41L));

            assertThat(dto.supported()).isTrue();
            assertThat(dto.receivedOn()).isEqualTo(WithholdingCertificateMother.RECIBIDO_EL);
            assertThat(dto.fileRef()).isEqualTo("s3://certificados/2025/CERT-2025-0001.pdf");
        }

        @Test
        @DisplayName("el comprobante de pago tambien acredita, aunque el papel no haya llegado")
        void el_comprobante_de_pago_tambien_acredita() {
            // Es la disyuncion que el DTO proyecta en vez de dejarsela al front: si
            // cada consumidor la reescribiera, el dia que la ley admita otro soporte
            // habria que corregirla en tres repositorios.
            WithholdingCertificateDto dto = WithholdingCertificateDto
                    .from(WithholdingCertificateMother.conSustituto(41L));

            assertThat(dto.supported()).isTrue();
            assertThat(dto.receivedOn()).isNull();
            assertThat(dto.substituteEvidenceKind())
                    .isEqualTo(SubstituteEvidenceKind.PAYMENT_RECEIPT);
            assertThat(dto.substituteEvidenceRef()).isEqualTo("s3://pagos/2025/REC-77120.pdf");
        }
    }
}
