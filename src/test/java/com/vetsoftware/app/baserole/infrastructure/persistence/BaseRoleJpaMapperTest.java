package com.vetsoftware.app.baserole.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.baserole.domain.BaseRole;
import com.vetsoftware.app.baserole.testsupport.BaseRoleMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BaseRoleJpaMapper")
class BaseRoleJpaMapperTest {

    private final BaseRoleJpaMapper mapper = new BaseRoleJpaMapper();

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            BaseRole baseRole = BaseRoleMother.veterinario();

            BaseRoleJpaEntity entity = mapper.toJpa(baseRole);

            assertThat(entity.getId()).isEqualTo(BaseRoleMother.BASE_ROLE_ID);
            assertThat(entity.getName()).isEqualTo("Veterinario");
            assertThat(entity.getCode()).isEqualTo("VET");
            assertThat(entity.getMandatory()).isFalse();
            assertThat(entity.getCreatedDate()).isEqualTo(BaseRoleMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("dominio a entidad a dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            BaseRole original = BaseRoleMother.administrador();

            BaseRoleJpaEntity entity = mapper.toJpa(original);
            BaseRole vuelta = mapper.toDomain(entity);

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }
}
