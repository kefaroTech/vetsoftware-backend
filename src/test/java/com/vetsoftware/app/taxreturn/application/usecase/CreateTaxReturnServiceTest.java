package com.vetsoftware.app.taxreturn.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.taxreturn.application.command.CreateTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.application.port.out.MunicipalityValidationPort;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.application.port.out.VatFilingPeriodValidationPort;
import com.vetsoftware.app.taxreturn.domain.TaxKind;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnStatus;
import com.vetsoftware.app.taxreturn.domain.VatFrequency;
import com.vetsoftware.app.taxreturn.testsupport.TaxReturnMother;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
@DisplayName("CreateTaxReturnService")
class CreateTaxReturnServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-20T09:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);

    @Mock
    private TaxReturnRepository repository;
    @Mock
    private MunicipalityValidationPort municipalityValidationPort;
    @Mock
    private VatFilingPeriodValidationPort vatFilingPeriodValidationPort;

    @Captor
    private ArgumentCaptor<TaxReturn> captor;

    private CreateTaxReturnService service;

    @BeforeEach
    void setUp() {
        service = new CreateTaxReturnService(repository, municipalityValidationPort,
                vatFilingPeriodValidationPort, RELOJ);
    }

    private static CreateTaxReturnCommand comandoRetencion() {
        return new CreateTaxReturnCommand(TaxKind.WITHHOLDING, TaxReturnMother.ANIO, "2026-M03",
                null, null, new BigDecimal("100.00"), new BigDecimal("20.00"),
                new BigDecimal("80.00"), BigDecimal.ZERO);
    }

    private static CreateTaxReturnCommand comandoIca(String municipio) {
        return new CreateTaxReturnCommand(TaxKind.ICA, TaxReturnMother.ANIO, "2026-B02", municipio,
                null, new BigDecimal("500.00"), new BigDecimal("100.00"), new BigDecimal("400.00"),
                BigDecimal.ZERO);
    }

    private static CreateTaxReturnCommand comandoIva(VatFrequency frecuencia) {
        return new CreateTaxReturnCommand(TaxKind.VAT, TaxReturnMother.ANIO, "2026-B02", null,
                frecuencia, new BigDecimal("900.00"), new BigDecimal("300.00"), BigDecimal.ZERO,
                new BigDecimal("600.00"));
    }

    @Nested
    @DisplayName("declaracion sin dato externo que validar")
    class SinValidacionExterna {

        @Test
        @DisplayName("una retencion se persiste sin tocar ningun puerto de validacion")
        void una_retencion_se_persiste_sin_tocar_ningun_puerto() {
            when(repository.save(any()))
                    .thenAnswer(inv -> TaxReturnMother.conId(900L, inv.getArgument(0)));

            TaxReturnDto dto = service.execute(comandoRetencion());

            verifyNoInteractions(municipalityValidationPort, vatFilingPeriodValidationPort);
            verify(repository).save(captor.capture());
            TaxReturn borrador = captor.getValue();
            assertThat(borrador.getTaxKind()).isEqualTo(TaxKind.WITHHOLDING);
            assertThat(borrador.getSequenceNumber()).isEqualTo(1);
            assertThat(borrador.getStatus()).isEqualTo(TaxReturnStatus.DRAFT);
            assertThat(borrador.getCreatedDate()).isEqualTo(AHORA);
            assertThat(dto.id()).isEqualTo(900L);
        }
    }

    @Nested
    @DisplayName("declaracion de ICA: el municipio es un hecho externo")
    class ValidacionDeMunicipio {

        @Test
        @DisplayName("municipio existente: se persiste con el codigo DIVIPOLA del comando")
        void municipio_existente_se_persiste() {
            when(municipalityValidationPort.existsByDaneCode(TaxReturnMother.MUNICIPIO_ICA))
                    .thenReturn(true);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoIca(TaxReturnMother.MUNICIPIO_ICA));

            verifyNoInteractions(vatFilingPeriodValidationPort);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getMunicipalityCode())
                    .isEqualTo(TaxReturnMother.MUNICIPIO_ICA);
        }

        @Test
        @DisplayName("municipio inexistente: aborta la creacion antes de persistir")
        void municipio_inexistente_aborta_la_creacion() {
            when(municipalityValidationPort.existsByDaneCode(TaxReturnMother.MUNICIPIO_ICA))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoIca(TaxReturnMother.MUNICIPIO_ICA)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Municipality not found: " + TaxReturnMother.MUNICIPIO_ICA);

            verifyNoInteractions(repository, vatFilingPeriodValidationPort);
        }
    }

    @Nested
    @DisplayName("declaracion de IVA: la periodicidad del año es un hecho externo")
    class ValidacionDePeriodicidadIva {

        @Test
        @DisplayName("periodicidad publicada: se persiste con la frecuencia del comando")
        void periodicidad_publicada_se_persiste() {
            when(vatFilingPeriodValidationPort.existsByFiscalYearAndFrequency(TaxReturnMother.ANIO,
                    VatFrequency.BIMONTHLY)).thenReturn(true);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoIva(VatFrequency.BIMONTHLY));

            verifyNoInteractions(municipalityValidationPort);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getVatFrequency()).isEqualTo(VatFrequency.BIMONTHLY);
        }

        @Test
        @DisplayName("periodicidad no publicada: aborta la creacion antes de persistir")
        void periodicidad_no_publicada_aborta_la_creacion() {
            when(vatFilingPeriodValidationPort.existsByFiscalYearAndFrequency(TaxReturnMother.ANIO,
                    VatFrequency.BIMONTHLY)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoIva(VatFrequency.BIMONTHLY)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "No VAT filing period published for year " + TaxReturnMother.ANIO);

            verifyNoInteractions(repository);
        }
    }
}
