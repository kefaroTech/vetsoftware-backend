package com.vetsoftware.app.animal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimaging.infrastructure.persistence.DiagnosticImagingJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaDiagnosticImagingChildrenQueryPort")
class JpaDiagnosticImagingChildrenQueryPortTest {

    @Mock
    private DiagnosticImagingJpaRepository jpaRepository;
    @InjectMocks
    private JpaDiagnosticImagingChildrenQueryPort port;

    @Test
    @DisplayName("delega en existsByAnimal_Id cuando hay imagenes diagnosticas activas")
    void delega_cuando_hay_imagenes_activas() {
        when(jpaRepository.existsByAnimal_Id(100L)).thenReturn(true);

        assertThat(port.existsActiveByAnimalId(100L)).isTrue();
    }

    @Test
    @DisplayName("delega en existsByAnimal_Id cuando no hay imagenes diagnosticas activas")
    void delega_cuando_no_hay_imagenes_activas() {
        when(jpaRepository.existsByAnimal_Id(100L)).thenReturn(false);

        assertThat(port.existsActiveByAnimalId(100L)).isFalse();
    }
}
