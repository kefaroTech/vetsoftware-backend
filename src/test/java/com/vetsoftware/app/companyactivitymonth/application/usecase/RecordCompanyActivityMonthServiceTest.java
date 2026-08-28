package com.vetsoftware.app.companyactivitymonth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companyactivitymonth.application.command.RecordCompanyActivityMonthCommand;
import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import com.vetsoftware.app.companyactivitymonth.application.port.out.CompanyActivityMonthRepository;
import com.vetsoftware.app.companyactivitymonth.application.port.out.CompanyValidationPort;
import com.vetsoftware.app.companyactivitymonth.domain.CommercialState;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonth;
import com.vetsoftware.app.companyactivitymonth.testsupport.CompanyActivityMonthMother;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordCompanyActivityMonthService")
class RecordCompanyActivityMonthServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 1, 0, 5);

    @Mock
    private CompanyActivityMonthRepository repository;

    @Mock
    private CompanyValidationPort companyValidationPort;

    private final Clock clock = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private RecordCompanyActivityMonthService service;

    @BeforeEach
    void setUp() {
        service = new RecordCompanyActivityMonthService(repository, companyValidationPort, clock);
    }

    @Nested
    @DisplayName("alta")
    class Alta {

        @Test
        @DisplayName("registra la actividad cuando la empresa existe")
        void registra_la_actividad_cuando_la_empresa_existe() {
            when(companyValidationPort.existsById(CompanyActivityMonthMother.COMPANY_ID))
                    .thenReturn(true);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CompanyActivityMonthDto dto = service
                    .execute(CompanyActivityMonthMother.comandoRegistrar());

            ArgumentCaptor<CompanyActivityMonth> captor = ArgumentCaptor
                    .forClass(CompanyActivityMonth.class);
            verify(repository).save(captor.capture());
            CompanyActivityMonth guardado = captor.getValue();
            assertThat(guardado.getId()).isNull();
            assertThat(guardado.getCompanyId()).isEqualTo(CompanyActivityMonthMother.COMPANY_ID);
            assertThat(guardado.getPeriodKey()).isEqualTo(CompanyActivityMonthMother.MARZO_2026);
            assertThat(guardado.getCreatedDate()).isEqualTo(AHORA);
            assertThat(dto.companyId()).isEqualTo(CompanyActivityMonthMother.COMPANY_ID);
        }

        @Test
        @DisplayName("companyId nulo no escribe nada")
        void company_id_nulo_no_escribe_nada() {
            RecordCompanyActivityMonthCommand comando = new RecordCompanyActivityMonthCommand(null,
                    "2026-03", CommercialState.PAID, 1, 1, 1, BigDecimal.ZERO);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");

            verifyNoInteractions(companyValidationPort);
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("una empresa inexistente no escribe nada")
        void una_empresa_inexistente_no_escribe_nada() {
            when(companyValidationPort.existsById(CompanyActivityMonthMother.COMPANY_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.execute(CompanyActivityMonthMother.comandoRegistrar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + CompanyActivityMonthMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }
    }
}
