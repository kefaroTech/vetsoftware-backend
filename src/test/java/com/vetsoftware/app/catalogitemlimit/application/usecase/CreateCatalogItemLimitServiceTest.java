package com.vetsoftware.app.catalogitemlimit.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitemlimit.application.command.CreateCatalogItemLimitCommand;
import com.vetsoftware.app.catalogitemlimit.application.dto.CatalogItemLimitDto;
import com.vetsoftware.app.catalogitemlimit.application.port.out.CatalogItemLimitRepository;
import com.vetsoftware.app.catalogitemlimit.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.catalogitemlimit.domain.CatalogItemLimitAlreadyExistsException;
import com.vetsoftware.app.catalogitemlimit.domain.LimitDimensionRef;
import com.vetsoftware.app.catalogitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.catalogitemlimit.domain.LimitMode;
import com.vetsoftware.app.catalogitemlimit.domain.MeasureKind;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCatalogItemLimitService — declarar el techo de fábrica")
class CreateCatalogItemLimitServiceTest {

    private static final Long HISTORIA_CLINICA = 3L;
    private static final Long EJE_ANIMAL = 1L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-01-15T08:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private CatalogItemLimitRepository repository;
    @Mock
    private LimitDimensionQueryPort limitDimensionQueryPort;

    private CreateCatalogItemLimitService service;

    @BeforeEach
    void crearElServicio() {
        service = new CreateCatalogItemLimitService(repository, limitDimensionQueryPort, RELOJ);
    }

    private static CreateCatalogItemLimitCommand cienMascotas() {
        return new CreateCatalogItemLimitCommand(HISTORIA_CLINICA, EJE_ANIMAL, LimitMode.LIMITED,
                100, null, LimitEnforcement.BLOCK, null, 80, LimitMode.FULL, null);
    }

    @Test
    @DisplayName("el tipo de medida se lee del eje y no se acepta de fuera")
    void el_tipo_de_medida_se_lee_del_eje() {
        when(repository.existsByCatalogItemIdAndLimitDimensionId(HISTORIA_CLINICA, EJE_ANIMAL))
                .thenReturn(false);
        when(limitDimensionQueryPort.findById(EJE_ANIMAL)).thenReturn(
                Optional.of(new LimitDimensionRef(EJE_ANIMAL, "ANIMAL", MeasureKind.CUMULATIVE)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogItemLimitDto creado = service.execute(cienMascotas());

        assertThat(creado.measureKind()).isEqualTo(MeasureKind.CUMULATIVE);
        assertThat(creado.limitQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("R-LIMIT-12 · declarar OVERAGE sobre un eje acumulativo no llega al motor: lo"
            + " para el dominio con el tipo que resolvió el puerto")
    void declarar_OVERAGE_sobre_un_eje_acumulativo_no_llega_al_motor() {
        when(repository.existsByCatalogItemIdAndLimitDimensionId(HISTORIA_CLINICA, EJE_ANIMAL))
                .thenReturn(false);
        when(limitDimensionQueryPort.findById(EJE_ANIMAL)).thenReturn(
                Optional.of(new LimitDimensionRef(EJE_ANIMAL, "ANIMAL", MeasureKind.CUMULATIVE)));

        assertThatThrownBy(() -> service.execute(new CreateCatalogItemLimitCommand(HISTORIA_CLINICA,
                EJE_ANIMAL, LimitMode.LIMITED, 100, null, LimitEnforcement.OVERAGE,
                new BigDecimal("500.00"), 80, LimitMode.FULL, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CUMULATIVE");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("con el techo ya declarado no escribe nada")
    void con_el_techo_ya_declarado_no_escribe_nada() {
        when(repository.existsByCatalogItemIdAndLimitDimensionId(HISTORIA_CLINICA, EJE_ANIMAL))
                .thenReturn(true);

        assertThatThrownBy(() -> service.execute(cienMascotas()))
                .isInstanceOf(CatalogItemLimitAlreadyExistsException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("un eje inexistente se rechaza antes de llegar a la clave foránea")
    void un_eje_inexistente_se_rechaza_antes_de_la_clave_foranea() {
        when(repository.existsByCatalogItemIdAndLimitDimensionId(HISTORIA_CLINICA, EJE_ANIMAL))
                .thenReturn(false);
        when(limitDimensionQueryPort.findById(EJE_ANIMAL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(cienMascotas()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit dimension 1 not found");

        verify(repository, never()).save(any());
    }
}
