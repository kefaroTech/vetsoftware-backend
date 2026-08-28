package com.vetsoftware.app.accountmapping.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.application.port.out.AccountMappingRepository;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import com.vetsoftware.app.accountmapping.domain.NoEffectiveAccountMappingException;
import com.vetsoftware.app.accountmapping.testsupport.AccountMappingMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LA consulta del negocio: de ella sale el asiento entero, y si no hay mapeo
 * vigente el service tiene que lanzar en vez de devolver vacio (ver
 * {@link NoEffectiveAccountMappingException}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResolveAccountMappingService")
class ResolveAccountMappingServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private AccountMappingRepository repository;

    private ResolveAccountMappingService service;

    @BeforeEach
    void setUp() {
        service = new ResolveAccountMappingService(repository, RELOJ);
    }

    @Nested
    @DisplayName("fecha explicita")
    class FechaExplicita {

        @Test
        @DisplayName("resuelve con la fecha del hecho economico, no la de hoy")
        void resuelve_con_la_fecha_del_hecho_economico() {
            LocalDate fechaDelHecho = LocalDate.of(2025, 12, 15);
            when(repository.findEffective(MappingKind.BANK, AccountMappingMother.MAPPING_KEY, null,
                    null, null, fechaDelHecho))
                    .thenReturn(Optional.of(AccountMappingMother.mapeoBancoAbierto()));

            AccountMappingDto dto = service.resolve(MappingKind.BANK,
                    AccountMappingMother.MAPPING_KEY, null, null, null, fechaDelHecho);

            assertThat(dto.mappingKey()).isEqualTo(AccountMappingMother.MAPPING_KEY);
            // La fecha inyectada por el caller manda: LocalDate.now(clock) ni se toca.
            verify(repository).findEffective(MappingKind.BANK, AccountMappingMother.MAPPING_KEY,
                    null, null, null, fechaDelHecho);
        }
    }

    @Nested
    @DisplayName("fecha nula: la pone el reloj inyectado")
    class FechaNula {

        @Test
        @DisplayName("con on=null, resuelve con LocalDate.now(clock), no con la fecha real")
        void con_fecha_nula_usa_el_reloj_inyectado() {
            LocalDate hoySegunElReloj = LocalDate.now(RELOJ);
            when(repository.findEffective(MappingKind.BANK, AccountMappingMother.MAPPING_KEY, null,
                    null, null, hoySegunElReloj))
                    .thenReturn(Optional.of(AccountMappingMother.mapeoBancoAbierto()));

            service.resolve(MappingKind.BANK, AccountMappingMother.MAPPING_KEY, null, null, null,
                    null);

            verify(repository).findEffective(MappingKind.BANK, AccountMappingMother.MAPPING_KEY,
                    null, null, null, hoySegunElReloj);
        }
    }

    @Nested
    @DisplayName("sin mapeo vigente")
    class SinMapeoVigente {

        @Test
        @DisplayName("lanza en vez de devolver vacio, con el supuesto completo en el mensaje")
        void lanza_en_vez_de_devolver_vacio() {
            LocalDate fecha = LocalDate.of(2026, 3, 1);
            when(repository.findEffective(MappingKind.REVENUE, "CONSULTA",
                    AccountMappingMother.CATALOG_ITEM_ID, "SERVICIO", "GRAVADO", fecha))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve(MappingKind.REVENUE, "CONSULTA",
                    AccountMappingMother.CATALOG_ITEM_ID, "SERVICIO", "GRAVADO", fecha))
                    .isInstanceOf(NoEffectiveAccountMappingException.class)
                    .hasMessageContaining("kind=REVENUE").hasMessageContaining("key=CONSULTA")
                    .hasMessageContaining("on 2026-03-01");
        }
    }
}
