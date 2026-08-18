package com.vetsoftware.app.employee.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeStatus;
import com.vetsoftware.app.employee.testsupport.EmployeeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa.
 *
 * <p>
 * {@code CompanyJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeJpaMapper")
class EmployeeJpaMapperTest {

    private final EmployeeJpaMapper mapper = new EmployeeJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            Employee employee = EmployeeMother.activo();

            EmployeeJpaEntity entity = mapper.toJpa(employee, companyEntity);

            assertThat(entity.getId()).isEqualTo(employee.getId());
            assertThat(entity.getEmployeeCode()).isEqualTo("VV-MARIANA");
            assertThat(entity.getHashPassword()).isEqualTo(EmployeeMother.HASH);
            assertThat(entity.getName()).isEqualTo("Mariana Rojas");
            assertThat(entity.getEmail()).isEqualTo("mariana@vetrina.co");
            assertThat(entity.getCreatedDate()).isEqualTo(EmployeeMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.isEmailVerified()).isTrue();
            assertThat(entity.isMustChangePassword()).isFalse();
            assertThat(entity.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
            assertThat(entity.getAuthVersion()).isZero();
        }

        @Test
        @DisplayName("engancha la empresa en su slot")
        void engancha_la_empresa_en_su_slot() {
            EmployeeJpaEntity entity = mapper.toJpa(EmployeeMother.activo(), companyEntity);

            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }

        @Test
        @DisplayName("un invitado conserva mustChangePassword y status INVITED")
        void un_invitado_conserva_must_change_password_y_status() {
            EmployeeJpaEntity entity = mapper.toJpa(EmployeeMother.invitado(), companyEntity);

            assertThat(entity.isMustChangePassword()).isTrue();
            assertThat(entity.getStatus()).isEqualTo(EmployeeStatus.INVITED);
        }
    }

    @Nested
    @DisplayName("toDomain con ref precargado — camino de escritura")
    class ToDomainConRef {

        @Test
        @DisplayName("reconstruye el agregado sin tocar la asociacion JPA")
        void reconstruye_el_agregado_sin_tocar_la_asociacion() {
            // Este overload existe para no inicializar el proxy de getReferenceById: si
            // leyera
            // entity.getCompany(), Hibernate lanzaria un SELECT extra en save.
            EmployeeJpaEntity entity = new EmployeeJpaEntity();
            entity.setId(EmployeeMother.EMPLOYEE_ID);
            entity.setEmployeeCode("VV-MARIANA");
            entity.setHashPassword(EmployeeMother.HASH);
            entity.setName("Mariana Rojas");
            entity.setEmail("mariana@vetrina.co");
            entity.setCreatedDate(EmployeeMother.CREADO);
            entity.setEnabled(true);
            entity.setEmailVerified(true);
            entity.setMustChangePassword(false);
            entity.setStatus(EmployeeStatus.ACTIVE);
            entity.setAuthVersion(0L);

            Employee employee = mapper.toDomain(entity, EmployeeMother.VETRINA);

            assertThat(employee.getId()).isEqualTo(EmployeeMother.EMPLOYEE_ID);
            assertThat(employee.getCompany()).isEqualTo(EmployeeMother.VETRINA);
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            Employee original = EmployeeMother.activo();

            EmployeeJpaEntity entity = mapper.toJpa(original, companyEntity);
            Employee vuelta = mapper.toDomain(entity, original.getCompany());

            assertThat(vuelta.getEmployeeCode()).isEqualTo(original.getEmployeeCode());
            assertThat(vuelta.getName()).isEqualTo(original.getName());
            assertThat(vuelta.getEmail()).isEqualTo(original.getEmail());
            assertThat(vuelta.getCompany()).isEqualTo(original.getCompany());
            assertThat(vuelta.getStatus()).isEqualTo(original.getStatus());
            assertThat(vuelta.getAuthVersion()).isEqualTo(original.getAuthVersion());
        }
    }

    @Nested
    @DisplayName("toDomain desde la asociacion — camino de lectura")
    class ToDomainDesdeAsociacion {

        @Test
        @DisplayName("construye el companion CompanyRef desde la asociacion JPA")
        void construye_el_company_ref_desde_la_asociacion() {
            when(companyEntity.getId()).thenReturn(EmployeeMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn(EmployeeMother.VETRINA.name());
            when(companyEntity.getIdentifier()).thenReturn(EmployeeMother.VETRINA.identifier());

            EmployeeJpaEntity entity = mapper.toJpa(EmployeeMother.activo(), companyEntity);
            entity.setCompany(companyEntity);

            Employee employee = mapper.toDomain(entity);

            assertThat(employee.getCompany()).isEqualTo(EmployeeMother.VETRINA);
        }
    }
}
