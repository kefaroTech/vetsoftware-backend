package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.SystemUserCredentialsRepository;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSystemUserCredentialsRepository implements SystemUserCredentialsRepository {

    private final SystemUserJpaRepository systemUserJpaRepository;

    public JpaSystemUserCredentialsRepository(SystemUserJpaRepository systemUserJpaRepository) {
        this.systemUserJpaRepository = systemUserJpaRepository;
    }

    /**
     * Devuelve credenciales solo de una cuenta HABILITADA.
     *
     * <p>
     * El filtro no esta aqui: {@code SystemUserJpaEntity} lleva
     * {@code @SQLRestriction("enabled = true")}, asi que toda consulta por este
     * repositorio ya excluye las desactivadas. Se documenta porque no se ve leyendo
     * este metodo, y porque de esa anotacion depende que desactivar un usuario de
     * sistema le corte de verdad el acceso. Lo fija
     * {@code SystemUserCredentialsPersistenceIT}.
     */
    @Override
    public Optional<SystemUserCredentials> findByCode(String code) {
        return systemUserJpaRepository.findByCode(code)
                .map(u -> new SystemUserCredentials(u.getId(), u.getHashPassword()));
    }
}
