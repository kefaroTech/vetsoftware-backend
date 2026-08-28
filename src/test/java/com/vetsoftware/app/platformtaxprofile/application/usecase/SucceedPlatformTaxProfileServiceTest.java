package com.vetsoftware.app.platformtaxprofile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import com.vetsoftware.app.platformtaxprofile.application.port.out.EconomicActivityQueryPort;
import com.vetsoftware.app.platformtaxprofile.application.port.out.PlatformTaxProfileRepository;
import com.vetsoftware.app.platformtaxprofile.domain.NoCurrentPlatformTaxProfileException;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfile;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfileSuccessionNotAfterCurrentException;
import com.vetsoftware.app.platformtaxprofile.testsupport.PlatformTaxProfileMother;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
@DisplayName("SucceedPlatformTaxProfileService")
class SucceedPlatformTaxProfileServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 6, 1, 9, 0);

    @Mock
    private PlatformTaxProfileRepository repository;

    @Mock
    private EconomicActivityQueryPort economicActivityQueryPort;

    private final Clock clock = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private SucceedPlatformTaxProfileService service;

    @BeforeEach
    void setUp() {
        service = new SucceedPlatformTaxProfileService(repository, economicActivityQueryPort,
                clock);
    }

    @Nested
    @DisplayName("sucesion")
    class Sucesion {

        @Test
        @DisplayName("sin identidad vigente no sucede nada")
        void sin_identidad_vigente_no_sucede_nada() {
            when(repository.findCurrent()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PlatformTaxProfileMother
                    .comandoSuceder(PlatformTaxProfileMother.VALID_FROM.plusDays(30))))
                    .isInstanceOf(NoCurrentPlatformTaxProfileException.class)
                    .hasMessageContaining("has no platform tax profile in force");

            verifyNoInteractions(economicActivityQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una actividad economica inexistente deja la vigente sin tocar")
        void una_actividad_economica_inexistente_deja_la_vigente_sin_tocar() {
            when(repository.findCurrent())
                    .thenReturn(Optional.of(PlatformTaxProfileMother.vigente()));
            when(economicActivityQueryPort.findById(PlatformTaxProfileMother.ACTIVIDAD.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PlatformTaxProfileMother
                    .comandoSuceder(PlatformTaxProfileMother.VALID_FROM.plusDays(30))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Economic activity not found");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("cierra la vigente y abre la sucesora en una sola operacion")
        void cierra_la_vigente_y_abre_la_sucesora() {
            when(repository.findCurrent())
                    .thenReturn(Optional.of(PlatformTaxProfileMother.vigente()));
            when(economicActivityQueryPort.findById(PlatformTaxProfileMother.ACTIVIDAD.id()))
                    .thenReturn(Optional.of(PlatformTaxProfileMother.ACTIVIDAD));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var effectiveFrom = PlatformTaxProfileMother.VALID_FROM.plusDays(30);
            PlatformTaxProfileDto dto = service
                    .execute(PlatformTaxProfileMother.comandoSuceder(effectiveFrom));

            ArgumentCaptor<PlatformTaxProfile> captor = ArgumentCaptor
                    .forClass(PlatformTaxProfile.class);
            verify(repository, times(2)).save(captor.capture());
            List<PlatformTaxProfile> guardadas = captor.getAllValues();

            PlatformTaxProfile cerrada = guardadas.get(0);
            assertThat(cerrada.getId()).isEqualTo(PlatformTaxProfileMother.PROFILE_ID);
            assertThat(cerrada.getValidTo()).isEqualTo(effectiveFrom);
            assertThat(cerrada.isCurrent()).isFalse();

            PlatformTaxProfile sucesora = guardadas.get(1);
            assertThat(sucesora.getId()).isNull();
            assertThat(sucesora.getValidFrom()).isEqualTo(effectiveFrom);
            assertThat(sucesora.isCurrent()).isTrue();
            assertThat(sucesora.getEconomicActivity())
                    .isEqualTo(PlatformTaxProfileMother.ACTIVIDAD);
            assertThat(sucesora.getLegalName()).isEqualTo("VetSoftware SAS BIC");
            assertThat(sucesora.getCreatedDate()).isEqualTo(AHORA);

            assertThat(dto.legalName()).isEqualTo("VetSoftware SAS BIC");
        }

        @Test
        @DisplayName("la sucesion en el mismo dia no es representable")
        void la_sucesion_en_el_mismo_dia_no_es_representable() {
            when(repository.findCurrent())
                    .thenReturn(Optional.of(PlatformTaxProfileMother.vigente()));
            when(economicActivityQueryPort.findById(PlatformTaxProfileMother.ACTIVIDAD.id()))
                    .thenReturn(Optional.of(PlatformTaxProfileMother.ACTIVIDAD));

            assertThatThrownBy(() -> service.execute(
                    PlatformTaxProfileMother.comandoSuceder(PlatformTaxProfileMother.VALID_FROM)))
                    .isInstanceOf(PlatformTaxProfileSuccessionNotAfterCurrentException.class)
                    .hasMessageContaining("cannot be succeeded");

            verify(repository, never()).save(any());
        }
    }
}
