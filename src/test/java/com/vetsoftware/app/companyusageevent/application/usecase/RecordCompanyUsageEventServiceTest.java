package com.vetsoftware.app.companyusageevent.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import com.vetsoftware.app.companyusageevent.application.port.out.CompanyUsageEventRepository;
import com.vetsoftware.app.companyusageevent.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEvent;
import com.vetsoftware.app.companyusageevent.domain.UsageBranch;
import com.vetsoftware.app.companyusageevent.testsupport.CompanyUsageEventMother;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordCompanyUsageEventService")
class RecordCompanyUsageEventServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 10, 23, 0);

    @Mock
    private CompanyUsageEventRepository repository;

    @Mock
    private LimitDimensionQueryPort limitDimensionQueryPort;

    private final Clock clock = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private RecordCompanyUsageEventService service;

    @BeforeEach
    void setUp() {
        service = new RecordCompanyUsageEventService(repository, limitDimensionQueryPort, clock);
    }

    @Nested
    @DisplayName("registro")
    class Registro {

        @Test
        @DisplayName("registra el hecho resolviendo el eje contra el catalogo")
        void registra_el_hecho_resolviendo_el_eje() {
            when(limitDimensionQueryPort.findByCode("ANIMAL"))
                    .thenReturn(Optional.of(CompanyUsageEventMother.DIMENSION_ANIMAL));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CompanyUsageEventDto dto = service.execute(CompanyUsageEventMother.comandoRegistrar());

            ArgumentCaptor<CompanyUsageEvent> captor = ArgumentCaptor
                    .forClass(CompanyUsageEvent.class);
            verify(repository).save(captor.capture());
            CompanyUsageEvent guardado = captor.getValue();
            assertThat(guardado.getCompanyId()).isEqualTo(CompanyUsageEventMother.COMPANY_ID);
            assertThat(guardado.getBranch()).isEqualTo(UsageBranch.ANIMAL);
            assertThat(guardado.getUsageReferenceId()).isEqualTo(CompanyUsageEventMother.ANIMAL_ID);
            assertThat(guardado.getChargeId()).isNull();

            // occurredAt es el instante del registro consumido, distinto del reloj del
            // proceso
            assertThat(guardado.getOccurredAt()).isEqualTo(CompanyUsageEventMother.OCCURRED_AT);
            assertThat(guardado.getOccurredAt()).isNotEqualTo(AHORA);
            assertThat(guardado.getCreatedDate()).isEqualTo(AHORA);

            assertThat(dto.companyId()).isEqualTo(CompanyUsageEventMother.COMPANY_ID);
        }

        @Test
        @DisplayName("un eje que no esta en el catalogo no escribe nada")
        void un_eje_desconocido_no_escribe_nada() {
            when(limitDimensionQueryPort.findByCode("ANIMAL")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CompanyUsageEventMother.comandoRegistrar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown limit dimension code: ANIMAL");

            verifyNoInteractions(repository);
        }
    }
}
