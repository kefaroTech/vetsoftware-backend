package com.vetsoftware.app.withholdingcertificate.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.withholdingcertificate.application.command.AttachSubstituteEvidenceCommand;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import com.vetsoftware.app.withholdingcertificate.domain.SubstituteEvidenceKind;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificateAlreadyReceivedException;
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
@DisplayName("AttachSubstituteEvidenceService — el soporte que la ley admite")
class AttachSubstituteEvidenceServiceTest {

    private static final Long ID = 41L;
    private static final String COMPROBANTE = "s3://pagos/2025/REC-77120.pdf";

    @Mock
    private WithholdingCertificateRepository repository;
    @InjectMocks
    private AttachSubstituteEvidenceService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("acredita la retencion con el comprobante de pago sin marcarla como recibida")
        void acredita_con_el_comprobante_sin_marcarla_como_recibida() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(WithholdingCertificateMother.conId(ID)));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            WithholdingCertificateDto dto = service.execute(new AttachSubstituteEvidenceCommand(ID,
                    SubstituteEvidenceKind.PAYMENT_RECEIPT, COMPROBANTE));

            assertThat(dto.substituteEvidenceKind())
                    .isEqualTo(SubstituteEvidenceKind.PAYMENT_RECEIPT);
            assertThat(dto.substituteEvidenceRef()).isEqualTo(COMPROBANTE);
            // Sigue faltando el papel: el sustituto acredita, no sustituye al hecho.
            assertThat(dto.receivedOn()).isNull();
            assertThat(dto.supported()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un certificado inexistente sale como no encontrado y no escribe")
        void un_certificado_inexistente_no_escribe() {
            when(repository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new AttachSubstituteEvidenceCommand(404L,
                    SubstituteEvidenceKind.PAYMENT_RECEIPT, COMPROBANTE)))
                    .isInstanceOf(WithholdingCertificateNotFoundException.class)
                    .hasMessageContaining("Withholding certificate not found: 404");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("adjuntarlo a un certificado que ya llego es un conflicto")
        void adjuntarlo_a_uno_que_ya_llego_es_un_conflicto() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(WithholdingCertificateMother.recibido(ID)));

            assertThatThrownBy(() -> service.execute(new AttachSubstituteEvidenceCommand(ID,
                    SubstituteEvidenceKind.PAYMENT_RECEIPT, COMPROBANTE)))
                    .isInstanceOf(WithholdingCertificateAlreadyReceivedException.class)
                    .hasMessageContaining("was already received on");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un sustituto sin referencia no escribe")
        void un_sustituto_sin_referencia_no_escribe() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(WithholdingCertificateMother.conId(ID)));

            assertThatThrownBy(() -> service.execute(new AttachSubstituteEvidenceCommand(ID,
                    SubstituteEvidenceKind.PAYMENT_RECEIPT, "  ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("substitute evidence needs both");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("carga por la variante ancha porque a este servicio solo llega SYSTEM")
        void carga_por_la_variante_ancha_porque_solo_llega_system() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(WithholdingCertificateMother.conId(ID)));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(new AttachSubstituteEvidenceCommand(ID,
                    SubstituteEvidenceKind.PAYMENT_RECEIPT, COMPROBANTE));

            verify(repository).findById(ID);
            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }
}
