package com.vetsoftware.app.cashterminal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.cashregister.application.port.out.BranchQueryPort;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashterminal.application.dto.CashTerminalDto;
import com.vetsoftware.app.cashterminal.infrastructure.persistence.CashTerminalJpaEntity;
import com.vetsoftware.app.cashterminal.infrastructure.persistence.CashTerminalJpaRepository;
import com.vetsoftware.app.cashterminal.testsupport.CashTerminalMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CashTerminalService")
class CashTerminalServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long BRANCH_ID = 2L;
    private static final Long TERMINAL_ID = 3L;

    @Mock
    private CashTerminalJpaRepository repository;
    @Mock
    private BranchQueryPort branchQueryPort;
    @Mock
    private CashSessionRepository cashSessionRepository;

    private CashTerminalService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new CashTerminalService(repository, branchQueryPort, cashSessionRepository);
    }

    @Nested
    @DisplayName("list")
    class Listado {

        @Test
        @DisplayName("lanza sin consultar la sede si branchId es null")
        void lanza_sin_consultar_la_sede_si_branch_id_es_null() {
            assertThatThrownBy(() -> service.list(COMPANY_ID, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sede no válida o inactiva");

            verifyNoInteractions(branchQueryPort);
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("lanza si la sede no está activa en la empresa")
        void lanza_si_la_sede_no_esta_activa() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.list(COMPANY_ID, BRANCH_ID, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sede no válida o inactiva");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("activeOnly=true consulta solo las terminales activas")
        void active_only_true_consulta_solo_las_terminales_activas() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);
            var activa = CashTerminalMother.activa(TERMINAL_ID, COMPANY_ID, BRANCH_ID,
                    "Caja principal", "CAJA-1");
            when(repository.findAllByCompanyIdAndBranchIdAndActiveTrueOrderByNameAsc(COMPANY_ID,
                    BRANCH_ID)).thenReturn(List.of(activa));

            List<CashTerminalDto> resultado = service.list(COMPANY_ID, BRANCH_ID, true);

            assertThat(resultado).extracting(CashTerminalDto::id).containsExactly(TERMINAL_ID);
            verify(repository, never())
                    .findAllByCompanyIdAndBranchIdOrderByActiveDescNameAsc(anyLong(), anyLong());
        }

        @Test
        @DisplayName("activeOnly=false consulta todas las terminales, activas e inactivas")
        void active_only_false_consulta_todas_las_terminales() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);
            var inactiva = CashTerminalMother.inactiva(TERMINAL_ID, COMPANY_ID, BRANCH_ID,
                    "Caja secundaria", "CAJA-2");
            when(repository.findAllByCompanyIdAndBranchIdOrderByActiveDescNameAsc(COMPANY_ID,
                    BRANCH_ID)).thenReturn(List.of(inactiva));

            List<CashTerminalDto> resultado = service.list(COMPANY_ID, BRANCH_ID, false);

            assertThat(resultado).extracting(CashTerminalDto::id).containsExactly(TERMINAL_ID);
            verify(repository, never())
                    .findAllByCompanyIdAndBranchIdAndActiveTrueOrderByNameAsc(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("create")
    class Creacion {

        @Test
        @DisplayName("lanza sin guardar si la sede no está activa")
        void lanza_sin_guardar_si_la_sede_no_esta_activa() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.create(COMPANY_ID, BRANCH_ID, "Caja", "COD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sede no válida o inactiva");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("lanza si el nombre está en blanco")
        void lanza_si_el_nombre_esta_en_blanco() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.create(COMPANY_ID, BRANCH_ID, "  ", "COD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nombre es obligatorio");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("lanza si el nombre supera 120 caracteres")
        void lanza_si_el_nombre_supera_120_caracteres() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.create(COMPANY_ID, BRANCH_ID, "a".repeat(121), "COD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nombre supera 120 caracteres");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("lanza si el código está en blanco")
        void lanza_si_el_codigo_esta_en_blanco() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.create(COMPANY_ID, BRANCH_ID, "Caja", " "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("código es obligatorio");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("lanza si el código supera 60 caracteres")
        void lanza_si_el_codigo_supera_60_caracteres() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.create(COMPANY_ID, BRANCH_ID, "Caja", "c".repeat(61)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("código supera 60 caracteres");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("lanza si ya existe un terminal con ese código en la sede")
        void lanza_si_ya_existe_un_terminal_con_ese_codigo_en_la_sede() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);
            when(repository.existsByCompanyIdAndBranchIdAndCodeIgnoreCase(COMPANY_ID, BRANCH_ID,
                    "CAJA-1")).thenReturn(true);

            assertThatThrownBy(() -> service.create(COMPANY_ID, BRANCH_ID, "Caja", "caja-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un terminal con ese código en la sede");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("persiste la terminal con el nombre recortado y el código normalizado en mayúsculas")
        void persiste_la_terminal_con_nombre_recortado_y_codigo_en_mayusculas() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);
            when(repository.existsByCompanyIdAndBranchIdAndCodeIgnoreCase(COMPANY_ID, BRANCH_ID,
                    "CAJA-1")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CashTerminalDto dto = service.create(COMPANY_ID, BRANCH_ID, "  Caja principal  ",
                    " caja-1 ");

            ArgumentCaptor<CashTerminalJpaEntity> captor = ArgumentCaptor
                    .forClass(CashTerminalJpaEntity.class);
            verify(repository).save(captor.capture());
            CashTerminalJpaEntity guardado = captor.getValue();
            assertThat(guardado.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(guardado.getBranchId()).isEqualTo(BRANCH_ID);
            assertThat(guardado.getName()).isEqualTo("Caja principal");
            assertThat(guardado.getCode()).isEqualTo("CAJA-1");
            assertThat(guardado.isActive()).isTrue();
            assertThat(guardado.getCreatedAt()).isNotNull();
            assertThat(dto.name()).isEqualTo("Caja principal");
            assertThat(dto.code()).isEqualTo("CAJA-1");
        }
    }

    @Nested
    @DisplayName("update")
    class Actualizacion {

        @Test
        @DisplayName("lanza si la terminal no existe en la empresa")
        void lanza_si_la_terminal_no_existe_en_la_empresa() {
            when(repository.findByIdAndCompanyId(TERMINAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(COMPANY_ID, TERMINAL_ID, "Caja", "COD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Terminal de caja no encontrado");

            verifyNoInteractions(cashSessionRepository);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no revisa cajas abiertas cuando el código no cambia")
        void no_revisa_cajas_abiertas_cuando_el_codigo_no_cambia() {
            var existente = CashTerminalMother.activa(TERMINAL_ID, COMPANY_ID, BRANCH_ID,
                    "Caja principal", "CAJA-1");
            when(repository.findByIdAndCompanyId(TERMINAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            when(repository.existsByCompanyIdAndBranchIdAndCodeIgnoreCaseAndIdNot(COMPANY_ID,
                    BRANCH_ID, "CAJA-1", TERMINAL_ID)).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.update(COMPANY_ID, TERMINAL_ID, "Caja renombrada", "caja-1");

            verifyNoInteractions(cashSessionRepository);
        }

        @Test
        @DisplayName("lanza si el código cambia y la terminal tiene una caja abierta")
        void lanza_si_el_codigo_cambia_y_la_terminal_tiene_una_caja_abierta() {
            var existente = CashTerminalMother.activa(TERMINAL_ID, COMPANY_ID, BRANCH_ID,
                    "Caja principal", "CAJA-1");
            when(repository.findByIdAndCompanyId(TERMINAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            when(cashSessionRepository.existsOpenByTerminalId(COMPANY_ID, BRANCH_ID, TERMINAL_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.update(COMPANY_ID, TERMINAL_ID, "Caja", "CAJA-2"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No se puede cambiar el código");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("permite cambiar el código cuando la terminal no tiene una caja abierta")
        void permite_cambiar_el_codigo_cuando_no_hay_una_caja_abierta() {
            var existente = CashTerminalMother.activa(TERMINAL_ID, COMPANY_ID, BRANCH_ID,
                    "Caja principal", "CAJA-1");
            when(repository.findByIdAndCompanyId(TERMINAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            when(cashSessionRepository.existsOpenByTerminalId(COMPANY_ID, BRANCH_ID, TERMINAL_ID))
                    .thenReturn(false);
            when(repository.existsByCompanyIdAndBranchIdAndCodeIgnoreCaseAndIdNot(COMPANY_ID,
                    BRANCH_ID, "CAJA-2", TERMINAL_ID)).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CashTerminalDto dto = service.update(COMPANY_ID, TERMINAL_ID, "Caja", "caja-2");

            assertThat(dto.code()).isEqualTo("CAJA-2");
        }

        @Test
        @DisplayName("lanza si el nuevo código ya lo usa otra terminal de la sede")
        void lanza_si_el_nuevo_codigo_ya_lo_usa_otra_terminal_de_la_sede() {
            var existente = CashTerminalMother.activa(TERMINAL_ID, COMPANY_ID, BRANCH_ID,
                    "Caja principal", "CAJA-1");
            when(repository.findByIdAndCompanyId(TERMINAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            when(cashSessionRepository.existsOpenByTerminalId(COMPANY_ID, BRANCH_ID, TERMINAL_ID))
                    .thenReturn(false);
            when(repository.existsByCompanyIdAndBranchIdAndCodeIgnoreCaseAndIdNot(COMPANY_ID,
                    BRANCH_ID, "CAJA-2", TERMINAL_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.update(COMPANY_ID, TERMINAL_ID, "Caja", "CAJA-2"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un terminal con ese código en la sede");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("setActive")
    class ActivacionDesactivacion {

        @Test
        @DisplayName("lanza si la terminal no existe en la empresa")
        void lanza_si_la_terminal_no_existe_en_la_empresa() {
            when(repository.findByIdAndCompanyId(TERMINAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setActive(COMPANY_ID, TERMINAL_ID, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Terminal de caja no encontrado");

            verifyNoInteractions(cashSessionRepository);
        }

        @Test
        @DisplayName("reactiva sin revisar cajas abiertas")
        void reactiva_sin_revisar_cajas_abiertas() {
            var inactiva = CashTerminalMother.inactiva(TERMINAL_ID, COMPANY_ID, BRANCH_ID,
                    "Caja principal", "CAJA-1");
            when(repository.findByIdAndCompanyId(TERMINAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(inactiva));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CashTerminalDto dto = service.setActive(COMPANY_ID, TERMINAL_ID, true);

            assertThat(dto.active()).isTrue();
            verifyNoInteractions(cashSessionRepository);
        }

        @Test
        @DisplayName("desactivar una terminal ya inactiva no revisa cajas abiertas")
        void desactivar_una_terminal_ya_inactiva_no_revisa_cajas_abiertas() {
            var inactiva = CashTerminalMother.inactiva(TERMINAL_ID, COMPANY_ID, BRANCH_ID,
                    "Caja principal", "CAJA-1");
            when(repository.findByIdAndCompanyId(TERMINAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(inactiva));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.setActive(COMPANY_ID, TERMINAL_ID, false);

            verifyNoInteractions(cashSessionRepository);
        }

        @Test
        @DisplayName("lanza si se intenta desactivar una terminal activa con una caja abierta")
        void lanza_si_se_intenta_desactivar_una_terminal_activa_con_caja_abierta() {
            var activa = CashTerminalMother.activa(TERMINAL_ID, COMPANY_ID, BRANCH_ID,
                    "Caja principal", "CAJA-1");
            when(repository.findByIdAndCompanyId(TERMINAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(activa));
            when(cashSessionRepository.existsOpenByTerminalId(COMPANY_ID, BRANCH_ID, TERMINAL_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.setActive(COMPANY_ID, TERMINAL_ID, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No se puede desactivar");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("desactiva una terminal activa sin cajas abiertas")
        void desactiva_una_terminal_activa_sin_cajas_abiertas() {
            var activa = CashTerminalMother.activa(TERMINAL_ID, COMPANY_ID, BRANCH_ID,
                    "Caja principal", "CAJA-1");
            when(repository.findByIdAndCompanyId(TERMINAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(activa));
            when(cashSessionRepository.existsOpenByTerminalId(COMPANY_ID, BRANCH_ID, TERMINAL_ID))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CashTerminalDto dto = service.setActive(COMPANY_ID, TERMINAL_ID, false);

            assertThat(dto.active()).isFalse();
        }
    }
}
