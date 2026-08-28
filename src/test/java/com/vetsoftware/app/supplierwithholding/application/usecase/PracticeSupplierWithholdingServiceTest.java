package com.vetsoftware.app.supplierwithholding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplierwithholding.application.command.PracticeSupplierWithholdingCommand;
import com.vetsoftware.app.supplierwithholding.application.port.out.MunicipalityValidationPort;
import com.vetsoftware.app.supplierwithholding.application.port.out.SupplierWithholdingRepository;
import com.vetsoftware.app.supplierwithholding.domain.SupplierDocumentKind;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingType;
import com.vetsoftware.app.supplierwithholding.testsupport.SupplierWithholdingMother;
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
@DisplayName("PracticeSupplierWithholdingService")
class PracticeSupplierWithholdingServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-10T09:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);

    @Mock
    private SupplierWithholdingRepository repository;
    @Mock
    private MunicipalityValidationPort municipalityValidationPort;

    @Captor
    private ArgumentCaptor<SupplierWithholding> captor;

    private PracticeSupplierWithholdingService service;

    @BeforeEach
    void setUp() {
        service = new PracticeSupplierWithholdingService(repository, municipalityValidationPort,
                RELOJ);
    }

    private static PracticeSupplierWithholdingCommand comando(SupplierWithholdingType tipo,
            String municipio) {
        return new PracticeSupplierWithholdingCommand("900123456", "Proveedor SAS",
                SupplierDocumentKind.NIT, "FV-2026-001", tipo, "Servicios veterinarios",
                new BigDecimal("1000.00"), new BigDecimal("2.500000"), new BigDecimal("25.00"),
                municipio, SupplierWithholdingMother.ANIO,
                SupplierWithholdingMother.periodoValido(tipo),
                SupplierWithholdingMother.PRACTICADA_EL);
    }

    @Nested
    @DisplayName("retencion nacional: sin municipio que validar")
    class SinMunicipio {

        @Test
        @DisplayName("una retencion de renta se practica sin tocar el puerto de municipio")
        void una_retencion_de_renta_se_practica_sin_tocar_el_puerto() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando(SupplierWithholdingType.INCOME_TAX, null));

            verifyNoInteractions(municipalityValidationPort);
            verify(repository).save(captor.capture());
            SupplierWithholding practicada = captor.getValue();
            assertThat(practicada.getWithholdingType())
                    .isEqualTo(SupplierWithholdingType.INCOME_TAX);
            assertThat(practicada.getMunicipalityCode()).isNull();
            assertThat(practicada.getCreatedDate()).isEqualTo(AHORA);
            assertThat(practicada.isCertified()).isFalse();
        }
    }

    @Nested
    @DisplayName("retencion de ICA: el municipio es un hecho externo")
    class ValidacionDeMunicipio {

        @Test
        @DisplayName("municipio existente: se practica con el codigo DIVIPOLA del comando")
        void municipio_existente_se_practica() {
            when(municipalityValidationPort.existsByDaneCode(SupplierWithholdingMother.MUNICIPIO))
                    .thenReturn(true);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(
                    comando(SupplierWithholdingType.ICA, SupplierWithholdingMother.MUNICIPIO));

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getMunicipalityCode())
                    .isEqualTo(SupplierWithholdingMother.MUNICIPIO);
        }

        @Test
        @DisplayName("municipio inexistente: aborta el registro antes de persistir")
        void municipio_inexistente_aborta_el_registro() {
            when(municipalityValidationPort.existsByDaneCode(SupplierWithholdingMother.MUNICIPIO))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.execute(
                    comando(SupplierWithholdingType.ICA, SupplierWithholdingMother.MUNICIPIO)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Municipality not found: " + SupplierWithholdingMother.MUNICIPIO);

            verifyNoInteractions(repository);
        }
    }
}
