package com.vetsoftware.app.systemuser.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.systemuser.domain.SystemUser;
import com.vetsoftware.app.systemuser.testsupport.SystemUserMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 */
@DisplayName("SystemUserJpaMapper")
class SystemUserJpaMapperTest {

    private final SystemUserJpaMapper mapper = new SystemUserJpaMapper();

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            SystemUser systemUser = SystemUserMother.activo();

            SystemUserJpaEntity entity = mapper.toJpa(systemUser);

            assertThat(entity.getId()).isEqualTo(SystemUserMother.SYSTEM_USER_ID);
            assertThat(entity.getCode()).isEqualTo(SystemUserMother.CODE);
            assertThat(entity.getHashPassword()).isEqualTo(SystemUserMother.HASH_PASSWORD);
            assertThat(entity.getCreatedDate()).isEqualTo(SystemUserMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.getAuthVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("propaga authVersion tal cual, sin reiniciarla")
        void propaga_auth_version_tal_cual() {
            SystemUserJpaEntity entity = mapper.toJpa(SystemUserMother.conAuthVersion(7L));

            assertThat(entity.getAuthVersion()).isEqualTo(7L);
        }

        @Test
        @DisplayName("propaga el usuario deshabilitado")
        void propaga_el_usuario_deshabilitado() {
            SystemUserJpaEntity entity = mapper.toJpa(SystemUserMother.deshabilitado());

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain — entidad a dominio")
    class ToDomain {

        @Test
        @DisplayName("reconstruye el agregado con cada campo en su sitio")
        void reconstruye_el_agregado_con_cada_campo_en_su_sitio() {
            SystemUserJpaEntity entity = new SystemUserJpaEntity();
            entity.setId(SystemUserMother.SYSTEM_USER_ID);
            entity.setCode(SystemUserMother.CODE);
            entity.setHashPassword(SystemUserMother.HASH_PASSWORD);
            entity.setCreatedDate(SystemUserMother.CREADO);
            entity.setEnabled(true);
            entity.setAuthVersion(3L);

            SystemUser systemUser = mapper.toDomain(entity);

            assertThat(systemUser.getId()).isEqualTo(SystemUserMother.SYSTEM_USER_ID);
            assertThat(systemUser.getCode()).isEqualTo(SystemUserMother.CODE);
            assertThat(systemUser.getHashPassword()).isEqualTo(SystemUserMother.HASH_PASSWORD);
            assertThat(systemUser.getCreatedDate()).isEqualTo(SystemUserMother.CREADO);
            assertThat(systemUser.isEnabled()).isTrue();
            assertThat(systemUser.getAuthVersion()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("dominio -> entidad -> dominio no pierde nada")
        void dominio_entidad_dominio_no_pierde_nada() {
            SystemUser original = SystemUserMother.conAuthVersion(5L);

            SystemUserJpaEntity entity = mapper.toJpa(original);
            SystemUser vuelta = mapper.toDomain(entity);

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }
}
