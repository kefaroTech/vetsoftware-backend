package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.testsupport.ReflectionEntities;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeNameQueryPort — nombre del cajero para el tiquete POS")
class JpaEmployeeNameQueryPortTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;

    private JpaEmployeeNameQueryPort port;

    @BeforeEach
    void montar() {
        port = new JpaEmployeeNameQueryPort(employeeJpaRepository);
    }

    @Test
    @DisplayName("un employeeId null no consulta el repositorio")
    void employee_id_null_no_consulta_el_repositorio() {
        assertThat(port.findName(null)).isEmpty();

        verifyNoInteractions(employeeJpaRepository);
    }

    @Test
    @DisplayName("un empleado existente con nombre devuelve su nombre")
    void empleado_existente_devuelve_su_nombre() throws Exception {
        EmployeeJpaEntity entity = ReflectionEntities.newInstance(EmployeeJpaEntity.class);
        entity.setName("Ana Cajera");
        when(employeeJpaRepository.findById(4L)).thenReturn(Optional.of(entity));

        assertThat(port.findName(4L)).contains("Ana Cajera");
    }

    @Test
    @DisplayName("un empleado con nombre en blanco se trata como ausente")
    void empleado_con_nombre_en_blanco_se_trata_como_ausente() throws Exception {
        EmployeeJpaEntity entity = ReflectionEntities.newInstance(EmployeeJpaEntity.class);
        entity.setName("   ");
        when(employeeJpaRepository.findById(5L)).thenReturn(Optional.of(entity));

        assertThat(port.findName(5L)).isEmpty();
    }

    @Test
    @DisplayName("un empleado inexistente devuelve vacio")
    void empleado_inexistente_devuelve_vacio() {
        when(employeeJpaRepository.findById(6L)).thenReturn(Optional.empty());

        assertThat(port.findName(6L)).isEmpty();
    }
}
