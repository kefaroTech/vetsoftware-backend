package com.vetsoftware.app.cashterminal.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.cashterminal.domain.CashTerminal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CashTerminalDto — from(CashTerminal)")
class CashTerminalDtoTest {

    @Test
    @DisplayName("copia cada campo de una terminal activa")
    void copia_cada_campo_de_una_terminal_activa() {
        LocalDateTime creado = LocalDateTime.of(2026, 3, 1, 9, 30);
        CashTerminal terminal = new CashTerminal(10L, 9L, 5L, "Caja principal", "CAJA-1", true,
                creado, 0L);

        CashTerminalDto dto = CashTerminalDto.from(terminal);

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.branchId()).isEqualTo(5L);
        assertThat(dto.name()).isEqualTo("Caja principal");
        assertThat(dto.code()).isEqualTo("CAJA-1");
        assertThat(dto.active()).isTrue();
        assertThat(dto.createdAt()).isEqualTo(creado);
    }

    @Test
    @DisplayName("refleja active=false cuando la terminal está desactivada")
    void refleja_active_false_cuando_esta_desactivada() {
        CashTerminal terminal = new CashTerminal(11L, 9L, 5L, "Caja secundaria", "CAJA-2", false,
                LocalDateTime.of(2026, 3, 1, 9, 30), 0L);

        CashTerminalDto dto = CashTerminalDto.from(terminal);

        assertThat(dto.active()).isFalse();
    }
}
