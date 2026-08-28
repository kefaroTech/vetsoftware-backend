package com.vetsoftware.app.withholdingcertificate.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.withholdingcertificate.application.command.RegisterWithholdingCertificateCommand;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingType;
import com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>El reloj va fijo y por constructor.</b> {@code createdDate} sale de
 * {@code LocalDateTime.now(clock)}; con un {@code now()} pelado, este test
 * tendria que comparar contra el reloj de la maquina y se caeria solo el dia
 * que cruzara la medianoche entre dos lineas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterWithholdingCertificateService — abrir la expectativa del papel")
class RegisterWithholdingCertificateServiceTest {

    private static final Clock RELOJ_FIJO = Clock.fixed(Instant.parse("2026-02-12T09:15:30Z"),
            ZoneOffset.UTC);

    @Mock
    private WithholdingCertificateRepository repository;

    private RegisterWithholdingCertificateService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("persiste el certificado con los diez campos del command en su sitio")
        void persiste_el_certificado_con_los_campos_del_command() {
            service = new RegisterWithholdingCertificateService(repository, RELOJ_FIJO);
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(WithholdingCertificateMother.comandoDeRegistro());

            ArgumentCaptor<WithholdingCertificate> guardado = ArgumentCaptor
                    .forClass(WithholdingCertificate.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue()).satisfies(certificado -> {
                assertThat(certificado.getCompanyId())
                        .isEqualTo(WithholdingCertificateMother.COMPANY_ID);
                assertThat(certificado.getIssuedByTaxId())
                        .isEqualTo(WithholdingCertificateMother.NIT_DEL_CLIENTE);
                assertThat(certificado.getCertificateNumber()).isEqualTo("CERT-2025-0001");
                assertThat(certificado.getWithholdingType()).isEqualTo(WithholdingType.INCOME_TAX);
                assertThat(certificado.getFiscalYear()).isEqualTo(2025);
                assertThat(certificado.getFiscalPeriodKey()).isEqualTo("2025-A");
                assertThat(certificado.getRatePercent()).isEqualByComparingTo("2.500000");
                assertThat(certificado.getCertifiedAmount()).isEqualByComparingTo("1847320.55");
                assertThat(certificado.getIssuedOn())
                        .isEqualTo(WithholdingCertificateMother.EXPEDIDO_EL);
                assertThat(certificado.getLegalDeadlineOn())
                        .isEqualTo(WithholdingCertificateMother.VENCE_EL);
            });
        }

        @Test
        @DisplayName("sella la fecha de creacion con el reloj inyectado y no con el de la maquina")
        void sella_la_fecha_de_creacion_con_el_reloj_inyectado() {
            service = new RegisterWithholdingCertificateService(repository, RELOJ_FIJO);
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(WithholdingCertificateMother.comandoDeRegistro());

            ArgumentCaptor<WithholdingCertificate> guardado = ArgumentCaptor
                    .forClass(WithholdingCertificate.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCreatedDate())
                    .isEqualTo(LocalDateTime.of(2026, 2, 12, 9, 15, 30));
        }

        @Test
        @DisplayName("nace como expectativa abierta: ni recibido ni acreditado")
        void nace_como_expectativa_abierta() {
            service = new RegisterWithholdingCertificateService(repository, RELOJ_FIJO);
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            WithholdingCertificateDto dto = service
                    .execute(WithholdingCertificateMother.comandoDeRegistro());

            assertThat(dto.receivedOn()).isNull();
            assertThat(dto.fileRef()).isNull();
            assertThat(dto.substituteEvidenceKind()).isNull();
            assertThat(dto.supported()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un periodo fiscal que no cuadra con el impuesto no llega al repositorio")
        void un_periodo_que_no_cuadra_no_llega_al_repositorio() {
            // La invariante vive en el constructor de la entidad, no aqui: el servicio
            // no la comprueba y aun asi la peticion invalida no escribe nada.
            service = new RegisterWithholdingCertificateService(repository, RELOJ_FIJO);

            assertThatThrownBy(() -> service.execute(conPeriodo("2025-B01")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalPeriodKey must be 2025-A for INCOME_TAX");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un ano gravable fuera de rango tampoco escribe")
        void un_ano_gravable_fuera_de_rango_tampoco_escribe() {
            service = new RegisterWithholdingCertificateService(repository, RELOJ_FIJO);

            assertThatThrownBy(() -> service.execute(new RegisterWithholdingCertificateCommand(
                    WithholdingCertificateMother.COMPANY_ID,
                    WithholdingCertificateMother.NIT_DEL_CLIENTE, "CERT-2019-0001",
                    WithholdingType.INCOME_TAX, 2019, "2019-A", new BigDecimal("2.5"),
                    WithholdingCertificateMother.IMPORTE_CERTIFICADO,
                    WithholdingCertificateMother.EXPEDIDO_EL,
                    WithholdingCertificateMother.VENCE_EL)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalYear must be between 2020 and 2100");

            verifyNoInteractions(repository);
        }
    }

    private static RegisterWithholdingCertificateCommand conPeriodo(String periodo) {
        return new RegisterWithholdingCertificateCommand(WithholdingCertificateMother.COMPANY_ID,
                WithholdingCertificateMother.NIT_DEL_CLIENTE, "CERT-2025-0001",
                WithholdingType.INCOME_TAX, WithholdingCertificateMother.ANO_GRAVABLE, periodo,
                WithholdingCertificateMother.TARIFA_RENTA,
                WithholdingCertificateMother.IMPORTE_CERTIFICADO,
                WithholdingCertificateMother.EXPEDIDO_EL, WithholdingCertificateMother.VENCE_EL);
    }
}
