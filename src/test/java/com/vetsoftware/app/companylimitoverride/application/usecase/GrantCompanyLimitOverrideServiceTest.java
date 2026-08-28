package com.vetsoftware.app.companylimitoverride.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companylimitoverride.application.command.GrantCompanyLimitOverrideCommand;
import com.vetsoftware.app.companylimitoverride.application.dto.CompanyLimitOverrideDto;
import com.vetsoftware.app.companylimitoverride.application.port.out.CompanyLimitOverrideRepository;
import com.vetsoftware.app.companylimitoverride.domain.CompanyAlreadyHasLimitOverrideException;
import com.vetsoftware.app.companylimitoverride.domain.OverrideReasonCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GrantCompanyLimitOverrideService — negociar una excepción de techo")
class GrantCompanyLimitOverrideServiceTest {

    private static final Long ANA = 42L;
    private static final Long EJE_ANIMAL = 1L;
    private static final Long EJE_USUARIOS = 2L;
    private static final Long COMERCIAL = 3L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-14T16:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private CompanyLimitOverrideRepository repository;

    private GrantCompanyLimitOverrideService service;

    @BeforeEach
    void crearElServicio() {
        service = new GrantCompanyLimitOverrideService(repository, RELOJ);
    }

    private static GrantCompanyLimitOverrideCommand excepcion(Long eje, int cantidad) {
        return new GrantCompanyLimitOverrideCommand(ANA, eje, cantidad, LocalDate.of(2026, 3, 14),
                OverrideReasonCode.RETENTION, "Retención — llamada del 14/03", COMERCIAL);
    }

    /**
     * <b>Lo que este caso puede probar, y lo que no.</b> El repositorio es un
     * doble: responde lo que se le diga y no toca el indice unico. Con
     * {@code uq_company_limit_overrides_alive} escrito sobre
     * {@code alive_company_marker} a secas —el error exacto que R-LIMIT-20 existe
     * para evitar— este caso seguiria verde y la segunda negociacion moriria en
     * produccion. Por eso ya no se anuncia como la prueba de R-LIMIT-20.
     *
     * <p>
     * Lo que si demuestra, y es suyo: que el servicio pregunta <em>por eje</em> y
     * no por empresa antes de escribir. El {@code verify} de las dos consultas con
     * ejes distintos es la asercion; si el servicio consultara por empresa, la
     * segunda llamada no preguntaria por {@code EJE_USUARIOS} y STRICT_STUBS haria
     * caer el caso por el stub muerto.
     *
     * <p>
     * La otra mitad —que el motor deja convivir las dos filas— vive contra MySQL
     * real en {@code CompanyLimitOverridePersistenceIT
     * #dos_excepciones_vivas_sobre_ejes_distintos_de_la_misma_empresa_coexisten}.
     */
    @Test
    @DisplayName("negociar 300 mascotas y 5 usuarios en la misma llamada consulta por eje y no por"
            + " empresa, así que las dos entran")
    void negociar_300_mascotas_y_5_usuarios_a_la_vez_produce_dos_excepciones_y_ninguna_falla() {
        when(repository.existsAliveByCompanyIdAndLimitDimensionId(ANA, EJE_ANIMAL))
                .thenReturn(false);
        when(repository.existsAliveByCompanyIdAndLimitDimensionId(ANA, EJE_USUARIOS))
                .thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyLimitOverrideDto mascotas = service.execute(excepcion(EJE_ANIMAL, 300));
        CompanyLimitOverrideDto usuarios = service.execute(excepcion(EJE_USUARIOS, 5));

        assertThat(mascotas.limitQuantity()).isEqualTo(300);
        assertThat(usuarios.limitQuantity()).isEqualTo(5);
        assertThat(mascotas.alive()).isTrue();
        assertThat(usuarios.alive()).isTrue();
        verify(repository).existsAliveByCompanyIdAndLimitDimensionId(ANA, EJE_ANIMAL);
        verify(repository).existsAliveByCompanyIdAndLimitDimensionId(ANA, EJE_USUARIOS);
    }

    @Test
    @DisplayName("dos excepciones vivas sobre el mismo eje no se escriben: sería un techo"
            + " indeterminado")
    void dos_excepciones_vivas_sobre_el_mismo_eje_no_se_escriben() {
        when(repository.existsAliveByCompanyIdAndLimitDimensionId(ANA, EJE_ANIMAL))
                .thenReturn(true);

        assertThatThrownBy(() -> service.execute(excepcion(EJE_ANIMAL, 500)))
                .isInstanceOf(CompanyAlreadyHasLimitOverrideException.class)
                .hasMessageContaining("indeterminate");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("R-LIMIT-34 · sin motivo escrito no llega a la base")
    void sin_motivo_escrito_no_llega_a_la_base() {
        when(repository.existsAliveByCompanyIdAndLimitDimensionId(ANA, EJE_ANIMAL))
                .thenReturn(false);

        assertThatThrownBy(
                () -> service.execute(new GrantCompanyLimitOverrideCommand(ANA, EJE_ANIMAL, 300,
                        LocalDate.of(2026, 3, 14), OverrideReasonCode.RETENTION, "  ", COMERCIAL)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any());
    }
}
