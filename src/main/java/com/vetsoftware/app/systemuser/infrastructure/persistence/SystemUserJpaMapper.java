package com.vetsoftware.app.systemuser.infrastructure.persistence;

import com.vetsoftware.app.systemuser.domain.SystemUser;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class SystemUserJpaMapper {

    /**
     * Entidad JPA de una cuenta de sistema recién dada de alta, construida a partir
     * de valores primitivos.
     *
     * <p>
     * <b>Existe para que el alta desde otra rodaja no tenga que importar
     * {@link SystemUser}.</b> El alta de superadministradores
     * ({@code platformaccess}) necesita crear la cuenta, y su adaptador vive en
     * {@code infrastructure/persistence} — el único cruce de vertical slicing que
     * el proyecto permite, pero acotado a la <i>entidad JPA</i> y al repositorio,
     * nunca al dominio. Importar {@code systemuser.domain.SystemUser} desde allí lo
     * rompía, y además <b>ArchUnit no lo veía</b>: {@code SIN_CRUCE_DE_DOMINIOS}
     * casa por {@code ..(*)..domain..} y el origen no está en un paquete
     * {@code domain}.
     *
     * <p>
     * Pasa por {@link SystemUser#provision} y no construye la entidad a mano a
     * propósito: las invariantes —código obligatorio y de 50 caracteres o menos,
     * contraseña, correo y nombre presentes, topes de longitud— se siguen
     * evaluando, y se evalúan aquí, dentro de la rodaja que las posee.
     */
    public SystemUserJpaEntity toJpaProvisioned(String code, String hashPassword, String email,
            String fullName, LocalDateTime createdDate) {
        return toJpa(SystemUser.provision(code, hashPassword, email, fullName, createdDate));
    }

    public SystemUserJpaEntity toJpa(SystemUser systemUser) {
        SystemUserJpaEntity entity = new SystemUserJpaEntity();
        entity.setId(systemUser.getId());
        entity.setCode(systemUser.getCode());
        entity.setHashPassword(systemUser.getHashPassword());
        entity.setCreatedDate(systemUser.getCreatedDate());
        entity.setVersion(systemUser.getVersion());
        entity.setEnabled(systemUser.isEnabled());
        entity.setAuthVersion(systemUser.getAuthVersion());
        entity.setEmail(systemUser.getEmail());
        entity.setFullName(systemUser.getFullName());
        return entity;
    }

    public SystemUser toDomain(SystemUserJpaEntity entity) {
        return new SystemUser(entity.getId(), entity.getCode(), entity.getHashPassword(),
                entity.getCreatedDate(), entity.getVersion(), entity.isEnabled(),
                entity.getAuthVersion(), entity.getEmail(), entity.getFullName());
    }
}
