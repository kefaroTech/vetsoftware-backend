package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.out.EmployeeCapacityPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employee.testsupport.EmployeeMother;
import com.vetsoftware.app.entitlement.domain.CapacityUnit;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityLimitExceededException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reactivación de un empleado desactivado. El UPDATE nativo es la fuente de
 * verdad de si existía la fila: {@code rows == 0} es el único camino de fallo,
 * no un {@code findById} previo. Por eso la empresa tiene que viajar hasta el
 * UPDATE — es la única barrera que separa a un tenant de devolverle el acceso a
 * alguien a quien otra empresa despidió.
 */
@ExtendWith(MockitoExtension.class)
class ReactivateEmployeeServiceTest {

    private static final Long EMPRESA = EmployeeMother.COMPANY_ID;
    private static final Long ID = EmployeeMother.EMPLOYEE_ID;

    @Mock
    private EmployeeRepository repository;
    @Mock
    private EmployeeCapacityPort employeeCapacityPort;
    @InjectMocks
    private ReactivateEmployeeService service;

    @Nested
    class ReactivacionCorrecta {

        @Test
        @DisplayName("reactiva y devuelve el empleado releido tras el UPDATE")
        void reactiva_y_devuelve_el_empleado_releido() {
            when(repository.findByIdIncludingDisabledAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeMother.deshabilitado()));
            when(repository.reactivate(ID, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeMother.activo()));

            EmployeeDto dto = service.execute(ID, EMPRESA);

            assertThat(dto.id()).isEqualTo(ID);
            assertThat(dto.enabled()).isTrue();
            verify(employeeCapacityPort).reserve(EMPRESA);
        }

        @Test
        @DisplayName("sin empresa en el contexto (SYSTEM) reactiva sin acotar")
        void sin_empresa_reactiva_sin_acotar() {
            when(repository.findByIdIncludingDisabled(ID))
                    .thenReturn(Optional.of(EmployeeMother.deshabilitado()));
            when(repository.reactivate(ID)).thenReturn(1);
            when(repository.findById(ID)).thenReturn(Optional.of(EmployeeMother.activo()));

            assertThat(service.execute(ID, null).id()).isEqualTo(ID);

            verify(repository, never()).reactivate(anyLong(), anyLong());
        }

        @Test
        @DisplayName("reactivar un empleado ya activo es idempotente y no reserva otra plaza")
        void reactivar_un_empleado_activo_es_idempotente() {
            when(repository.findByIdIncludingDisabledAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeMother.activo()));

            assertThat(service.execute(ID, EMPRESA).enabled()).isTrue();

            verify(repository, never()).reactivate(anyLong(), anyLong());
            verifyNoInteractions(employeeCapacityPort);
        }
    }

    @Nested
    class Tenancy {

        /**
         * El defecto que cierra este test: sin el {@code AND company_id}, reactivar por
         * id devolvía el login —y subía la {@code auth_version}— de un empleado
         * despedido por otra empresa.
         */
        @Test
        @DisplayName("un empleado de otra empresa no se reactiva ni se relee")
        void un_empleado_de_otra_empresa_no_se_reactiva() {
            when(repository.findByIdIncludingDisabledAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ID, EMPRESA))
                    .isInstanceOf(EmployeeNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).reactivate(anyLong());
            verify(repository, never()).reactivate(anyLong(), anyLong());
            verify(repository, never()).findById(any());
            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verifyNoInteractions(employeeCapacityPort);
        }
    }

    @Nested
    class Rechazos {

        @Test
        @DisplayName("el limite USER aborta antes del UPDATE de reactivacion")
        void el_limite_user_aborta_antes_del_update() {
            when(repository.findByIdIncludingDisabledAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeMother.deshabilitado()));
            doThrow(new CompanyCapacityLimitExceededException(EMPRESA, CapacityUnit.USER, 3, 3, 1))
                    .when(employeeCapacityPort).reserve(EMPRESA);

            assertThatThrownBy(() -> service.execute(ID, EMPRESA))
                    .isInstanceOf(CompanyCapacityLimitExceededException.class)
                    .hasMessageContaining("USER");

            verify(repository, never()).reactivate(anyLong(), anyLong());
            verify(repository, never()).findByIdAndCompanyId(ID, EMPRESA);
        }
    }
}
