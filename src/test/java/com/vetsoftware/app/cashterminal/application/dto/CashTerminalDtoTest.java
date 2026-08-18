package com.vetsoftware.app.cashterminal.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.cashterminal.infrastructure.persistence.CashTerminalJpaEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CashTerminalDto — from(CashTerminalJpaEntity)")
class CashTerminalDtoTest {

    @Test
    @DisplayName("copia cada campo de una terminal activa")
    void copia_cada_campo_de_una_terminal_activa() {
        CashTerminalJpaEntity entity = new CashTerminalJpaEntity();
        entity.setId(10L);
        entity.setBranchId(5L);
        entity.setName("Caja principal");
        entity.setCode("CAJA-1");
        entity.setActive(true);
        LocalDateTime creado = LocalDateTime.of(2026, 3, 1, 9, 30);
        entity.setCreatedAt(creado);

        CashTerminalDto dto = CashTerminalDto.from(entity);

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
        CashTerminalJpaEntity entity = new CashTerminalJpaEntity();
        entity.setId(11L);
        entity.setBranchId(5L);
        entity.setName("Caja secundaria");
        entity.setCode("CAJA-2");
        entity.setActive(false);
        entity.setCreatedAt(LocalDateTime.of(2026, 3, 1, 9, 30));

        CashTerminalDto dto = CashTerminalDto.from(entity);

        assertThat(dto.active()).isFalse();
    }
}
