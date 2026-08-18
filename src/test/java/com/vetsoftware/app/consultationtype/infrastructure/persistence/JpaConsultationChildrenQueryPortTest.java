package com.vetsoftware.app.consultationtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaConsultationChildrenQueryPort")
class JpaConsultationChildrenQueryPortTest {

    @Mock
    private ConsultationJpaRepository jpaRepository;

    @InjectMocks
    private JpaConsultationChildrenQueryPort port;

    @Test
    @DisplayName("delega en existsByConsultationType_Id del repositorio de Spring Data")
    void delega_en_existsByConsultationType_Id() {
        when(jpaRepository.existsByConsultationType_Id(5L)).thenReturn(true);

        assertThat(port.existsActiveByConsultationTypeId(5L)).isTrue();
    }

    @Test
    @DisplayName("sin consultas activas devuelve false")
    void sin_consultas_activas_devuelve_false() {
        when(jpaRepository.existsByConsultationType_Id(5L)).thenReturn(false);

        assertThat(port.existsActiveByConsultationTypeId(5L)).isFalse();
    }
}
