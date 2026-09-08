package com.vetsoftware.app.service.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companylimitevent.application.command.RecordLimitEventCommand;
import com.vetsoftware.app.companylimitevent.application.port.in.RecordLimitEventUseCase;
import com.vetsoftware.app.companylimitoverride.application.dto.EffectiveLimitDto;
import com.vetsoftware.app.companylimitoverride.application.port.in.ResolveEffectiveLimitUseCase;
import com.vetsoftware.app.companylimitoverride.domain.LimitSource;
import com.vetsoftware.app.service.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.domain.LimitDimensionRef;
import com.vetsoftware.app.service.domain.ServiceLimitExceededException;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntitlementServiceUsageLimitAdapter")
class EntitlementServiceUsageLimitAdapterTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long DIMENSION_ID = 6L;
    private static final LimitDimensionRef SERVICE_ITEM_DIMENSION = new LimitDimensionRef(
            DIMENSION_ID, "SERVICE_ITEM");

    @Mock
    private LimitDimensionQueryPort limitDimensionQueryPort;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private ResolveEffectiveLimitUseCase resolveEffectiveLimit;
    @Mock
    private RecordLimitEventUseCase recordLimitEvent;
    @Mock
    private SystemAuthRunner systemAuthRunner;
    @Mock
    private Authz authz;

    @InjectMocks
    private EntitlementServiceUsageLimitAdapter adapter;

    @SuppressWarnings("unchecked")
    private void systemAuthRunnerEjecutaLaAccion() {
        when(systemAuthRunner.call(any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<EffectiveLimitDto>) inv.getArgument(0)).get());
    }

    @Nested
    @DisplayName("dentro del cupo")
    class DentroDelCupo {

        @Test
        @DisplayName("un techo sin limite no cuenta las tarifas activas")
        void techo_sin_limite_no_cuenta() {
            when(limitDimensionQueryPort.findByCode("SERVICE_ITEM"))
                    .thenReturn(Optional.of(SERVICE_ITEM_DIMENSION));
            systemAuthRunnerEjecutaLaAccion();
            when(resolveEffectiveLimit.resolve(COMPANY_ID, DIMENSION_ID))
                    .thenReturn(new EffectiveLimitDto(COMPANY_ID, DIMENSION_ID, null,
                            LimitSource.NONE, null, true));

            assertThatCode(() -> adapter.checkNotExceeded(COMPANY_ID)).doesNotThrowAnyException();

            verifyNoInteractions(serviceRepository, recordLimitEvent);
        }

        @Test
        @DisplayName("por debajo del techo deja pasar")
        void por_debajo_del_techo_pasa() {
            when(limitDimensionQueryPort.findByCode("SERVICE_ITEM"))
                    .thenReturn(Optional.of(SERVICE_ITEM_DIMENSION));
            systemAuthRunnerEjecutaLaAccion();
            when(resolveEffectiveLimit.resolve(COMPANY_ID, DIMENSION_ID))
                    .thenReturn(new EffectiveLimitDto(COMPANY_ID, DIMENSION_ID, 20,
                            LimitSource.CATALOG_DEFAULT, null, false));
            when(serviceRepository.countEnabledByCompanyId(COMPANY_ID)).thenReturn(19L);

            assertThatCode(() -> adapter.checkNotExceeded(COMPANY_ID)).doesNotThrowAnyException();

            verifyNoInteractions(recordLimitEvent);
        }
    }

    @Test
    @DisplayName("en el techo, deniega, registra el portazo y lanza ServiceLimitExceededException")
    void en_el_techo_deniega_y_registra() {
        when(limitDimensionQueryPort.findByCode("SERVICE_ITEM"))
                .thenReturn(Optional.of(SERVICE_ITEM_DIMENSION));
        systemAuthRunnerEjecutaLaAccion();
        when(resolveEffectiveLimit.resolve(COMPANY_ID, DIMENSION_ID))
                .thenReturn(new EffectiveLimitDto(COMPANY_ID, DIMENSION_ID, 20,
                        LimitSource.CATALOG_DEFAULT, null, false));
        when(serviceRepository.countEnabledByCompanyId(COMPANY_ID)).thenReturn(20L);
        when(authz.currentEmployeeIdOrNull()).thenReturn(3L);

        assertThatThrownBy(() -> adapter.checkNotExceeded(COMPANY_ID))
                .isInstanceOf(ServiceLimitExceededException.class)
                .hasMessageContaining("SERVICE_ITEM");

        ArgumentCaptor<RecordLimitEventCommand> captor = ArgumentCaptor
                .forClass(RecordLimitEventCommand.class);
        verify(recordLimitEvent).execute(captor.capture());
        assertThat(captor.getValue().limitQuantity()).isEqualTo(20);
        assertThat(captor.getValue().usedQuantity()).isEqualTo(20);
        assertThat(captor.getValue().requestedDelta()).isEqualTo(1);
    }

    @Test
    @DisplayName("un eje que no esta en el catalogo no consulta nada mas")
    void un_eje_desconocido_no_consulta_nada_mas() {
        when(limitDimensionQueryPort.findByCode("SERVICE_ITEM")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.checkNotExceeded(COMPANY_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verify(serviceRepository, never()).countEnabledByCompanyId(any());
        verifyNoInteractions(resolveEffectiveLimit, recordLimitEvent, systemAuthRunner);
    }
}
