package com.vetsoftware.app.consultation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultation.domain.ConsultationTypeRef;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaEntity;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaConsultationTypeQueryPortTest {

    @Mock
    private ConsultationTypeJpaRepository consultationTypeJpaRepository;
    @InjectMocks
    private JpaConsultationTypeQueryPort port;

    private static ConsultationTypeJpaEntity tipoEncontrado(long id, String name) {
        ConsultationTypeJpaEntity entity = mock(ConsultationTypeJpaEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getName()).thenReturn(name);
        return entity;
    }

    @Test
    @DisplayName("mapea el tipo de consulta encontrado a su companion VO")
    void mapea_el_tipo_de_consulta_encontrado_a_su_companion_vo() {
        ConsultationTypeJpaEntity tipo = tipoEncontrado(5L, "Control");
        when(consultationTypeJpaRepository.findById(5L)).thenReturn(Optional.of(tipo));

        Optional<ConsultationTypeRef> ref = port.findById(5L);

        assertThat(ref).contains(new ConsultationTypeRef(5L, "Control"));
    }

    @Test
    @DisplayName("devuelve vacio si el tipo de consulta no existe")
    void devuelve_vacio_si_el_tipo_de_consulta_no_existe() {
        when(consultationTypeJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
