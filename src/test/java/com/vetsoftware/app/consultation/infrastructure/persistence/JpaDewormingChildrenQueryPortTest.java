package com.vetsoftware.app.consultation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.deworming.infrastructure.persistence.DewormingJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaDewormingChildrenQueryPort")
class JpaDewormingChildrenQueryPortTest {

    @Mock
    private DewormingJpaRepository jpaRepository;
    @InjectMocks
    private JpaDewormingChildrenQueryPort port;

    @Test
    @DisplayName("delega en existsByConsultation_Id cuando hay desparasitaciones activas")
    void delega_cuando_hay_desparasitaciones_activas() {
        when(jpaRepository.existsByConsultation_Id(200L)).thenReturn(true);

        assertThat(port.existsActiveByConsultationId(200L)).isTrue();
    }

    @Test
    @DisplayName("delega en existsByConsultation_Id cuando no hay desparasitaciones activas")
    void delega_cuando_no_hay_desparasitaciones_activas() {
        when(jpaRepository.existsByConsultation_Id(200L)).thenReturn(false);

        assertThat(port.existsActiveByConsultationId(200L)).isFalse();
    }
}
