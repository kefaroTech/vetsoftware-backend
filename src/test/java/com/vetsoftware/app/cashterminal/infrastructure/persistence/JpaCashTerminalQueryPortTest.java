package com.vetsoftware.app.cashterminal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.cashregister.application.port.out.CashTerminalQueryPort.TerminalRef;
import com.vetsoftware.app.cashterminal.testsupport.CashTerminalMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCashTerminalQueryPort")
class JpaCashTerminalQueryPortTest {

    private static final Long TERMINAL_ID = 1L;
    private static final Long COMPANY_ID = 2L;
    private static final Long BRANCH_ID = 3L;

    @Mock
    private CashTerminalJpaRepository repository;

    @InjectMocks
    private JpaCashTerminalQueryPort port;

    @Nested
    @DisplayName("findActive")
    class FindActive {

        @Test
        @DisplayName("devuelve vacío sin tocar el repositorio cuando terminalId es null")
        void devuelve_vacio_sin_tocar_el_repositorio_cuando_terminal_id_es_null() {
            Optional<TerminalRef> resultado = port.findActive(null, COMPANY_ID, BRANCH_ID);

            assertThat(resultado).isEmpty();
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("mapea la entidad activa encontrada a TerminalRef")
        void mapea_la_entidad_activa_encontrada_a_terminal_ref() {
            var entity = CashTerminalMother.activa(TERMINAL_ID, COMPANY_ID, BRANCH_ID,
                    "Caja principal", "CAJA-1");
            when(repository.findByIdAndCompanyIdAndBranchIdAndActiveTrue(TERMINAL_ID, COMPANY_ID,
                    BRANCH_ID)).thenReturn(Optional.of(entity));

            Optional<TerminalRef> resultado = port.findActive(TERMINAL_ID, COMPANY_ID, BRANCH_ID);

            assertThat(resultado)
                    .contains(new TerminalRef(TERMINAL_ID, "Caja principal", "CAJA-1"));
        }

        @Test
        @DisplayName("devuelve vacío cuando el repositorio no encuentra una terminal activa")
        void devuelve_vacio_cuando_el_repositorio_no_encuentra_una_terminal_activa() {
            when(repository.findByIdAndCompanyIdAndBranchIdAndActiveTrue(TERMINAL_ID, COMPANY_ID,
                    BRANCH_ID)).thenReturn(Optional.empty());

            Optional<TerminalRef> resultado = port.findActive(TERMINAL_ID, COMPANY_ID, BRANCH_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
