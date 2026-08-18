package com.vetsoftware.app.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.port.out.RefreshTokenIssuer;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository.NewRefreshToken;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenSecret;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenIssuerAdapterTest {

    @Mock
    private RefreshTokenSecret refreshTokenSecret;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("emite el crudo al llamante y guarda solo su hash con la expiración configurada")
    void emite_el_crudo_y_guarda_solo_su_hash() {
        RefreshTokenIssuer adapter = new RefreshTokenIssuerAdapter(refreshTokenSecret,
                refreshTokenRepository, 30L);
        when(refreshTokenSecret.generateRaw()).thenReturn("raw-token");
        when(refreshTokenSecret.hash("raw-token")).thenReturn("hashed");
        LocalDateTime antes = LocalDateTime.now();

        String issued = adapter.issue(7L, "EMPLOYEE", 5L);

        LocalDateTime despues = LocalDateTime.now();
        assertThat(issued).isEqualTo("raw-token");
        ArgumentCaptor<NewRefreshToken> captor = ArgumentCaptor.forClass(NewRefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        NewRefreshToken saved = captor.getValue();
        assertThat(saved.tokenHash()).isEqualTo("hashed");
        assertThat(saved.subjectId()).isEqualTo(7L);
        assertThat(saved.subjectType()).isEqualTo("EMPLOYEE");
        assertThat(saved.authVersion()).isEqualTo(5L);
        assertThat(saved.expiresAt()).isAfter(antes.plusDays(29)).isBefore(despues.plusDays(31));
    }

    @Test
    @DisplayName("la expiración sale de la configuración, no de una constante")
    void la_expiracion_sale_de_la_configuracion() {
        RefreshTokenIssuer adapterDeUnDia = new RefreshTokenIssuerAdapter(refreshTokenSecret,
                refreshTokenRepository, 1L);
        when(refreshTokenSecret.generateRaw()).thenReturn("raw");
        when(refreshTokenSecret.hash("raw")).thenReturn("hashed");

        adapterDeUnDia.issue(2L, "SYSTEM_USER", 9L);

        ArgumentCaptor<NewRefreshToken> captor = ArgumentCaptor.forClass(NewRefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().expiresAt()).isBefore(LocalDateTime.now().plusDays(2));
    }
}
