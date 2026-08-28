package com.vetsoftware.app.withholdingcertificate.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.withholdingcertificate.application.command.ReceiveWithholdingCertificateCommand;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificateAlreadyReceivedException;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificateNotFoundException;
import com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiveWithholdingCertificateService — cerrar la expectativa")
class ReceiveWithholdingCertificateServiceTest {

    private static final Long ID = 41L;
    private static final String ARCHIVO = "s3://certificados/2025/CERT-2025-0001.pdf";

    @Mock
    private WithholdingCertificateRepository repository;
    @InjectMocks
    private ReceiveWithholdingCertificateService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("guarda el certificado con su fecha de llegada y su archivo")
        void guarda_el_certificado_con_su_fecha_y_su_archivo() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(WithholdingCertificateMother.conId(ID)));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            WithholdingCertificateDto dto = service
                    .execute(new ReceiveWithholdingCertificateCommand(ID,
                            WithholdingCertificateMother.RECIBIDO_EL, ARCHIVO));

            ArgumentCaptor<WithholdingCertificate> guardado = ArgumentCaptor
                    .forClass(WithholdingCertificate.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getReceivedOn())
                    .isEqualTo(WithholdingCertificateMother.RECIBIDO_EL);
            assertThat(guardado.getValue().getFileRef()).isEqualTo(ARCHIVO);
            assertThat(dto.supported()).isTrue();
        }

        @Test
        @DisplayName("al llegar el papel retira el sustituto que ya no aplica")
        void al_llegar_el_papel_retira_el_sustituto() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(WithholdingCertificateMother.conSustituto(ID)));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            WithholdingCertificateDto dto = service
                    .execute(new ReceiveWithholdingCertificateCommand(ID,
                            WithholdingCertificateMother.RECIBIDO_EL, ARCHIVO));

            assertThat(dto.substituteEvidenceKind()).isNull();
            assertThat(dto.substituteEvidenceRef()).isNull();
            assertThat(dto.receivedOn()).isEqualTo(WithholdingCertificateMother.RECIBIDO_EL);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un certificado inexistente sale como no encontrado y no escribe")
        void un_certificado_inexistente_no_escribe() {
            when(repository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new ReceiveWithholdingCertificateCommand(404L,
                    WithholdingCertificateMother.RECIBIDO_EL, ARCHIVO)))
                    .isInstanceOf(WithholdingCertificateNotFoundException.class)
                    .hasMessageContaining("Withholding certificate not found: 404");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("recibir dos veces el mismo certificado es un conflicto y no una sobreescritura")
        void recibir_dos_veces_es_un_conflicto() {
            // La unica regla que la base no cuida: el segundo UPDATE seria valido para
            // el motor y se llevaria por delante el archivo ya guardado.
            when(repository.findById(ID))
                    .thenReturn(Optional.of(WithholdingCertificateMother.recibido(ID)));

            assertThatThrownBy(() -> service.execute(new ReceiveWithholdingCertificateCommand(ID,
                    LocalDate.of(2026, 3, 25), "s3://certificados/otro.pdf")))
                    .isInstanceOf(WithholdingCertificateAlreadyReceivedException.class)
                    .hasMessageContaining("was already received on");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una fecha de llegada anterior a la expedicion no escribe")
        void una_fecha_de_llegada_anterior_a_la_expedicion_no_escribe() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(WithholdingCertificateMother.conId(ID)));

            assertThatThrownBy(() -> service.execute(new ReceiveWithholdingCertificateCommand(ID,
                    WithholdingCertificateMother.EXPEDIDO_EL.minusDays(1), ARCHIVO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("receivedOn cannot be before issuedOn");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("carga por la variante ancha porque a este servicio solo llega SYSTEM")
        void carga_por_la_variante_ancha_porque_solo_llega_system() {
            // Es la exencion que declara CARGA_POR_ID_ACOTADA_POR_EMPRESA: el puerto es
            // hasRole('SYSTEM') a secas y un principal SYSTEM no tiene empresa de la
            // que tirar. Si algun dia se abriera a un empleado, la carga tendria que
            // pasar a findByIdAndCompanyId en el mismo cambio y este caso se pondria
            // rojo por el verify de abajo.
            when(repository.findById(ID))
                    .thenReturn(Optional.of(WithholdingCertificateMother.conId(ID)));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(new ReceiveWithholdingCertificateCommand(ID,
                    WithholdingCertificateMother.RECIBIDO_EL, ARCHIVO));

            verify(repository).findById(ID);
            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }
}
