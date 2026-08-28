package com.vetsoftware.app.platformtaxprofile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import com.vetsoftware.app.platformtaxprofile.application.port.out.EconomicActivityQueryPort;
import com.vetsoftware.app.platformtaxprofile.application.port.out.PlatformTaxProfileRepository;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfile;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfileAlreadyOpenException;
import com.vetsoftware.app.platformtaxprofile.testsupport.PlatformTaxProfileMother;
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
@DisplayName("OpenPlatformTaxProfileService")
class OpenPlatformTaxProfileServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 1, 1, 8, 0);

    @Mock
    private PlatformTaxProfileRepository repository;

    @Mock
    private EconomicActivityQueryPort economicActivityQueryPort;

    private final Clock clock = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private OpenPlatformTaxProfileService service;

    @BeforeEach
    void setUp() {
        service = new OpenPlatformTaxProfileService(repository, economicActivityQueryPort, clock);
    }

    @Nested
    @DisplayName("apertura")
    class Apertura {

        @Test
        @DisplayName("abre la primera identidad resolviendo la actividad economica")
        void abre_la_primera_identidad_resolviendo_la_actividad() {
            when(repository.findCurrent()).thenReturn(Optional.empty());
            when(economicActivityQueryPort.findById(PlatformTaxProfileMother.ACTIVIDAD.id()))
                    .thenReturn(Optional.of(PlatformTaxProfileMother.ACTIVIDAD));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlatformTaxProfileDto dto = service.execute(PlatformTaxProfileMother.comandoAbrir());

            ArgumentCaptor<PlatformTaxProfile> captor = ArgumentCaptor
                    .forClass(PlatformTaxProfile.class);
            verify(repository).save(captor.capture());
            PlatformTaxProfile guardado = captor.getValue();
            assertThat(guardado.getId()).isNull();
            assertThat(guardado.isCurrent()).isTrue();
            assertThat(guardado.getEconomicActivity())
                    .isEqualTo(PlatformTaxProfileMother.ACTIVIDAD);
            assertThat(guardado.getCreatedDate()).isEqualTo(AHORA);
            assertThat(dto.legalName()).isEqualTo("VetSoftware SAS");
        }

        @Test
        @DisplayName("la actividad economica es opcional: null entra y no consulta el puerto")
        void la_actividad_economica_es_opcional() {
            when(repository.findCurrent()).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(PlatformTaxProfileMother.comandoAbrirSinActividad());

            ArgumentCaptor<PlatformTaxProfile> captor = ArgumentCaptor
                    .forClass(PlatformTaxProfile.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEconomicActivity()).isNull();
            verifyNoInteractions(economicActivityQueryPort);
        }

        @Test
        @DisplayName("no abre una segunda identidad si ya hay una vigente")
        void no_abre_si_ya_hay_una_vigente() {
            when(repository.findCurrent())
                    .thenReturn(Optional.of(PlatformTaxProfileMother.vigente()));

            assertThatThrownBy(() -> service.execute(PlatformTaxProfileMother.comandoAbrir()))
                    .isInstanceOf(PlatformTaxProfileAlreadyOpenException.class)
                    .hasMessageContaining("already has a platform tax profile in force");

            verifyNoInteractions(economicActivityQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una actividad economica inexistente no abre nada")
        void una_actividad_economica_inexistente_no_abre_nada() {
            when(repository.findCurrent()).thenReturn(Optional.empty());
            when(economicActivityQueryPort.findById(PlatformTaxProfileMother.ACTIVIDAD.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PlatformTaxProfileMother.comandoAbrir()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Economic activity not found: "
                            + PlatformTaxProfileMother.ACTIVIDAD.id());

            verify(repository, never()).save(any());
        }
    }
}
