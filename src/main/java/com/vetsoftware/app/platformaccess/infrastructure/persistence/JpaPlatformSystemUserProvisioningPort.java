package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import com.vetsoftware.app.platformaccess.application.port.out.PlatformSystemUserProvisioningPort;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaEntity;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaMapper;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Crea la cuenta de sistema que resulta de aceptar la invitación.
 *
 * <p>
 * Vive en {@code infrastructure/persistence} porque es el <b>único</b> cruce de
 * vertical slicing que el proyecto permite: persistencia contra persistencia de
 * otra feature. Ni el dominio ni los casos de uso de {@code platformaccess}
 * conocen {@code SystemUser}; lo que cruza la frontera es este adaptador y nada
 * más.
 *
 * <p>
 * <b>Y ese cruce llega hasta la entidad JPA, no hasta el dominio.</b> Esta
 * clase importaba {@code systemuser.domain.SystemUser} para llamar a
 * {@code SystemUser.provision(...)}, que es exactamente lo que la regla de
 * vertical slicing prohíbe — y lo hacía en el punto ciego de
 * {@code SIN_CRUCE_DE_DOMINIOS}, que casa por {@code ..(*)..domain..} y no ve
 * un origen en {@code infrastructure.persistence}. Hoy la construcción la hace
 * {@code SystemUserJpaMapper.toJpaProvisioned(...)}, dentro de la rodaja dueña
 * del modelo, que sigue pasando por {@code SystemUser.provision} y conserva sus
 * invariantes.
 *
 * <p>
 * <b>No concede un solo permiso, y eso es el control de seguridad, no un
 * descuido.</b> El filtro de autenticación otorga {@code ROLE_SYSTEM} a toda
 * cuenta de sistema sin mirar permisos, así que crear la fila ya es conceder
 * acceso total. Añadir aquí una llamada a cualquier caso de uso de permisos, o
 * copiar los de otra cuenta, sería duplicar el privilegio sin ganar nada y
 * abrir un camino para que el cliente influya en él.
 */
@Component
public class JpaPlatformSystemUserProvisioningPort implements PlatformSystemUserProvisioningPort {

    private final SystemUserJpaRepository systemUserJpaRepository;
    private final SystemUserJpaMapper systemUserJpaMapper;

    public JpaPlatformSystemUserProvisioningPort(SystemUserJpaRepository systemUserJpaRepository,
            SystemUserJpaMapper systemUserJpaMapper) {
        this.systemUserJpaRepository = systemUserJpaRepository;
        this.systemUserJpaMapper = systemUserJpaMapper;
    }

    @Override
    public boolean emailTaken(String email) {
        return systemUserJpaRepository.countByEmailIncludingDisabled(email) > 0;
    }

    @Override
    public boolean codeTaken(String code) {
        return systemUserJpaRepository.countByCodeIncludingDisabled(code) > 0;
    }

    /**
     * El {@code UNIQUE} de la base es la última línea, no esta comprobación: si dos
     * aceptaciones concurrentes pasan las dos por {@link #emailTaken} o por
     * {@link #codeTaken}, la segunda revienta aquí con violación de unicidad y su
     * transacción entera se deshace. Es lo correcto —no puede haber dos
     * superadministradores con la misma identidad— y es lo único que la
     * concurrencia no se puede comer.
     */
    @Override
    public Long provision(String code, String email, String fullName, String hashedPassword,
            LocalDateTime createdDate) {
        SystemUserJpaEntity saved = systemUserJpaRepository.save(systemUserJpaMapper
                .toJpaProvisioned(code, hashedPassword, email, fullName, createdDate));
        return saved.getId();
    }
}
