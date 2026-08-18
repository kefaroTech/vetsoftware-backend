package com.vetsoftware.app.registration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeCodeChecker")
class JpaEmployeeCodeCheckerTest {

    @Mock
    private EmployeeJpaRepository jpaRepository;
    @InjectMocks
    private JpaEmployeeCodeChecker checker;

    @Test
    @DisplayName("un código con filas existentes (incluidas las desactivadas) está en uso")
    void un_codigo_con_filas_existentes_esta_en_uso() {
        when(jpaRepository.countByEmployeeCodeAllRows("orlando@vetrina.co")).thenReturn(1L);

        assertThat(checker.exists("orlando@vetrina.co")).isTrue();
    }

    @Test
    @DisplayName("un código sin ninguna fila está disponible")
    void un_codigo_sin_filas_esta_disponible() {
        when(jpaRepository.countByEmployeeCodeAllRows("nuevo@vetrina.co")).thenReturn(0L);

        assertThat(checker.exists("nuevo@vetrina.co")).isFalse();
    }
}
