package com.vetsoftware.app.subscriptionitemlimit.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionitemlimit.application.command.PropagateCatalogLimitImprovementCommand;
import com.vetsoftware.app.subscriptionitemlimit.application.port.out.SubscriptionItemLimitRepository;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitMode;
import com.vetsoftware.app.subscriptionitemlimit.domain.MeasureKind;
import com.vetsoftware.app.subscriptionitemlimit.domain.SubscriptionItemLimit;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PropagateCatalogLimitImprovementService — D-75, solo las mejoras entran")
class PropagateCatalogLimitImprovementServiceTest {

    private static final Long HISTORIA_CLINICA = 3L;
    private static final Long EJE_ANIMAL = 1L;
    private static final LocalDateTime FIRMA = LocalDateTime.of(2026, 9, 1, 8, 0);

    @Mock
    private SubscriptionItemLimitRepository repository;
    @InjectMocks
    private PropagateCatalogLimitImprovementService service;

    private static SubscriptionItemLimit congelado(Long companyId, int cantidad) {
        return SubscriptionItemLimit.freeze(companyId, 8L, EJE_ANIMAL, MeasureKind.CUMULATIVE,
                LimitMode.LIMITED, cantidad, null, LimitEnforcement.BLOCK, null, 80, FIRMA);
    }

    @Test
    @DisplayName("R-LIMIT-36 · subir el cupo de fábrica de 100 a 200 llega a los contratos vivos"
            + " sin crear ninguna excepción negociada")
    void subir_el_cupo_de_fabrica_de_100_a_200_llega_a_los_contratos_vivos_sin_crear_excepciones() {
        List<SubscriptionItemLimit> vivos = List.of(congelado(1L, 100), congelado(2L, 100),
                congelado(3L, 100));
        when(repository.findAllLiveByCatalogItemIdAndLimitDimensionId(HISTORIA_CLINICA, EJE_ANIMAL))
                .thenReturn(vivos);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int movidos = service.execute(new PropagateCatalogLimitImprovementCommand(HISTORIA_CLINICA,
                EJE_ANIMAL, LimitMode.LIMITED, 200));

        assertThat(movidos).isEqualTo(3);
        ArgumentCaptor<List<SubscriptionItemLimit>> guardados = ArgumentCaptor.captor();
        verify(repository).saveAll(guardados.capture());
        assertThat(guardados.getValue())
                .allSatisfy(limite -> assertThat(limite.getLimitQuantity()).isEqualTo(200));
    }

    @Test
    @DisplayName("bajarlo de 100 a 80 no toca ningún contrato vivo y no escribe nada")
    void bajarlo_de_100_a_80_no_toca_ningun_contrato_vivo() {
        when(repository.findAllLiveByCatalogItemIdAndLimitDimensionId(HISTORIA_CLINICA, EJE_ANIMAL))
                .thenReturn(List.of(congelado(1L, 100), congelado(2L, 100)));

        int movidos = service.execute(new PropagateCatalogLimitImprovementCommand(HISTORIA_CLINICA,
                EJE_ANIMAL, LimitMode.LIMITED, 80));

        assertThat(movidos).isZero();
        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("guarda solo lo que cambió: los contratos que ya estaban por encima no se"
            + " reescriben")
    void guarda_solo_lo_que_cambio() {
        when(repository.findAllLiveByCatalogItemIdAndLimitDimensionId(HISTORIA_CLINICA, EJE_ANIMAL))
                .thenReturn(List.of(congelado(1L, 100), congelado(2L, 300)));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int movidos = service.execute(new PropagateCatalogLimitImprovementCommand(HISTORIA_CLINICA,
                EJE_ANIMAL, LimitMode.LIMITED, 200));

        assertThat(movidos).isEqualTo(1);
        ArgumentCaptor<List<SubscriptionItemLimit>> guardados = ArgumentCaptor.captor();
        verify(repository).saveAll(guardados.capture());
        assertThat(guardados.getValue()).singleElement()
                .satisfies(limite -> assertThat(limite.getCompanyId()).isEqualTo(1L));
    }

    @Test
    @DisplayName("sin contratos vivos no escribe nada")
    void sin_contratos_vivos_no_escribe_nada() {
        when(repository.findAllLiveByCatalogItemIdAndLimitDimensionId(HISTORIA_CLINICA, EJE_ANIMAL))
                .thenReturn(List.of());

        assertThat(service.execute(new PropagateCatalogLimitImprovementCommand(HISTORIA_CLINICA,
                EJE_ANIMAL, LimitMode.LIMITED, 200))).isZero();

        verify(repository, never()).saveAll(any());
    }
}
