package com.vetsoftware.app.supplierwithholding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplierwithholding.application.command.IssueSupplierWithholdingCertificateCommand;
import com.vetsoftware.app.supplierwithholding.application.port.out.SupplierWithholdingRepository;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingCertificateAlreadyIssuedException;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingNotFoundException;
import com.vetsoftware.app.supplierwithholding.testsupport.SupplierWithholdingMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("IssueSupplierWithholdingCertificateService")
class IssueSupplierWithholdingCertificateServiceTest {

    private static final Long ID = 200L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-04-01T09:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);

    @Mock
    private SupplierWithholdingRepository repository;

    @Captor
    private ArgumentCaptor<SupplierWithholding> captor;

    private IssueSupplierWithholdingCertificateService service;

    @BeforeEach
    void setUp() {
        service = new IssueSupplierWithholdingCertificateService(repository, RELOJ);
    }

    @Nested
    @DisplayName("emision del certificado")
    class Emision {

        @Test
        @DisplayName("emite con la fecha del reloj inyectado, nunca la del cliente")
        void emite_con_la_fecha_del_reloj_inyectado() {
            SupplierWithholding sinCertificar = SupplierWithholdingMother.conId(ID,
                    SupplierWithholdingMother.renta());
            when(repository.findById(ID)).thenReturn(Optional.of(sinCertificar));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new IssueSupplierWithholdingCertificateCommand(ID, "CERT-2026-001"));

            verify(repository).save(captor.capture());
            SupplierWithholding certificada = captor.getValue();
            assertThat(certificada.getCertificateIssuedAt()).isEqualTo(AHORA);
            assertThat(certificada.getCertificateRef()).isEqualTo("CERT-2026-001");
            assertThat(certificada.isCertified()).isTrue();
        }

        @Test
        @DisplayName("retencion inexistente no emite certificado")
        void retencion_inexistente_no_emite_certificado() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new IssueSupplierWithholdingCertificateCommand(ID, "CERT-2026-001")))
                    .isInstanceOf(SupplierWithholdingNotFoundException.class)
                    .hasMessageContaining("Supplier withholding not found: " + ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un certificado ya emitido no se puede volver a emitir")
        void un_certificado_ya_emitido_no_se_puede_volver_a_emitir() {
            SupplierWithholding yaCertificada = SupplierWithholdingMother.conId(ID,
                    SupplierWithholdingMother.conCertificado(LocalDateTime.of(2026, 3, 15, 10, 0),
                            "CERT-VIEJO"));
            when(repository.findById(ID)).thenReturn(Optional.of(yaCertificada));

            assertThatThrownBy(() -> service
                    .execute(new IssueSupplierWithholdingCertificateCommand(ID, "CERT-NUEVO")))
                    .isInstanceOf(SupplierWithholdingCertificateAlreadyIssuedException.class)
                    .hasMessageContaining(
                            "Supplier withholding " + ID + " already has a certificate");

            verify(repository, never()).save(any());
        }
    }
}
