package com.vetsoftware.app.companyusageevent.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companylimitoverride.application.dto.EffectiveLimitDto;
import com.vetsoftware.app.companylimitoverride.application.port.in.ResolveEffectiveLimitUseCase;
import com.vetsoftware.app.companylimitoverride.domain.LimitSource;
import com.vetsoftware.app.companyusageevent.domain.EffectiveUsageLimit;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResolveEffectiveUsageLimitAdapter")
class ResolveEffectiveUsageLimitAdapterTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long DIMENSION_ID = 5L;

    @Mock
    private ResolveEffectiveLimitUseCase resolveEffectiveLimit;
    @Mock
    private SystemAuthRunner systemAuthRunner;

    @InjectMocks
    private ResolveEffectiveUsageLimitAdapter adapter;

    @SuppressWarnings("unchecked")
    private void systemAuthRunnerEjecutaLaAccion() {
        when(systemAuthRunner.call(org.mockito.ArgumentMatchers.<Supplier<EffectiveLimitDto>>any()))
                .thenAnswer(inv -> ((Supplier<EffectiveLimitDto>) inv.getArgument(0)).get());
    }

    @Test
    @DisplayName("corre bajo SystemAuthRunner y traduce el techo sin techo a EffectiveUsageLimit ilimitado")
    void traduce_el_techo_sin_techo() {
        systemAuthRunnerEjecutaLaAccion();
        when(resolveEffectiveLimit.resolve(COMPANY_ID, DIMENSION_ID))
                .thenReturn(new EffectiveLimitDto(COMPANY_ID, DIMENSION_ID, null, LimitSource.NONE,
                        null, true));

        EffectiveUsageLimit limit = adapter.resolve(COMPANY_ID, DIMENSION_ID);

        assertThat(limit.unlimited()).isTrue();
        assertThat(limit.limitQuantity()).isNull();
    }

    @Test
    @DisplayName("traduce un techo con cantidad")
    void traduce_un_techo_con_cantidad() {
        systemAuthRunnerEjecutaLaAccion();
        when(resolveEffectiveLimit.resolve(COMPANY_ID, DIMENSION_ID))
                .thenReturn(new EffectiveLimitDto(COMPANY_ID, DIMENSION_ID, 30,
                        LimitSource.CATALOG_DEFAULT, null, false));

        EffectiveUsageLimit limit = adapter.resolve(COMPANY_ID, DIMENSION_ID);

        assertThat(limit.unlimited()).isFalse();
        assertThat(limit.limitQuantity()).isEqualTo(30);
    }

    @Test
    @DisplayName("si SystemAuthRunner no ejecuta la accion, no se resuelve el techo")
    void sin_ejecutar_la_accion_no_se_resuelve() {
        when(systemAuthRunner.call(org.mockito.ArgumentMatchers.<Supplier<EffectiveLimitDto>>any()))
                .thenReturn(new EffectiveLimitDto(COMPANY_ID, DIMENSION_ID, null, LimitSource.NONE,
                        null, true));

        adapter.resolve(COMPANY_ID, DIMENSION_ID);

        org.mockito.Mockito.verifyNoInteractions(resolveEffectiveLimit);
    }
}
