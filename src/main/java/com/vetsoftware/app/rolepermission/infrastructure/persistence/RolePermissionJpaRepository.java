package com.vetsoftware.app.rolepermission.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermissionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"role", "permission"})
    Optional<RolePermissionJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"role", "permission"})
    Optional<RolePermissionJpaEntity> findByIdAndRole_Company_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = "permission")
    List<RolePermissionJpaEntity> findByRoleIdIn(List<Long> roleIds);

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermissionJpaEntity> findAllByRoleId(Long roleId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE role_permissions
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE role_permissions rp
            JOIN roles r ON r.id = rp.role_id
            SET rp.enabled = true
            WHERE rp.id = :id
              AND r.company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    /**
     * Baja en cascada de todos los permisos vigentes de un rol, acotada a la
     * empresa. No existe la variante sin {@code companyId}: el {@code role_id} es
     * una FK ajena y no acota nada por si mismo — el rol es de alguien—, asi que un
     * {@code WHERE role_id = :roleId} a secas deshabilitaba en bloque los permisos
     * del rol de cualquier tenant para quien conociera el id.
     *
     * <p>
     * El {@code DeleteRoleService} que la invoca ya valida la propiedad del rol
     * antes de llegar aqui, pero esa comprobacion vive en Java y esta en el
     * {@code WHERE}: es una mutacion en bloque, la clase de operacion donde una
     * lectura previa que se caiga de un refactor no deja rastro.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE role_permissions rp
            JOIN roles r ON r.id = rp.role_id
            SET rp.enabled = false
            WHERE rp.role_id = :roleId
              AND rp.enabled = true
              AND r.company_id = :companyId
            """, nativeQuery = true)
    int disableAllByRoleId(@org.springframework.data.repository.query.Param("roleId") Long roleId,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT id
            FROM role_permissions
            WHERE role_id = :roleId
              AND permission_id = :permissionId
              AND enabled = false
            LIMIT 1
            """, nativeQuery = true)
    java.util.Optional<Long> findDisabledIdByRoleAndPermission(
            @org.springframework.data.repository.query.Param("roleId") Long roleId,
            @org.springframework.data.repository.query.Param("permissionId") Long permissionId);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT id AS id, permission_id AS permissionId
            FROM role_permissions
            WHERE role_id = :roleId
              AND permission_id IN (:permissionIds)
              AND enabled = false
            """, nativeQuery = true)
    List<DisabledRolePermissionRow> findDisabledByRoleAndPermissions(
            @org.springframework.data.repository.query.Param("roleId") Long roleId,
            @org.springframework.data.repository.query.Param("permissionIds") java.util.Collection<Long> permissionIds);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE role_permissions
            SET enabled = true
            WHERE id IN (:ids)
            """, nativeQuery = true)
    int reactivateAllByIds(
            @org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);

    /**
     * Reactivacion en bloque acotada al tenant. Es la peor variante de la familia y
     * por partida doble: en una reactivacion no hay lectura previa que valide la
     * propiedad —el servicio decide si la fila existe mirando cuantas actualizo— y
     * ademas entra una <em>lista</em> de ids, asi que un solo id ajeno colado en el
     * lote devolvia un permiso revocado al rol de otra empresa. El {@code JOIN
     * roles} es toda la barrera que hay.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE role_permissions rp
            JOIN roles r ON r.id = rp.role_id
            SET rp.enabled = true
            WHERE rp.id IN (:ids)
              AND r.company_id = :companyId
            """, nativeQuery = true)
    int reactivateAllByIds(
            @org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    /**
     * Baja en bloque, simetrica de {@link #reactivateAllByIds}. No se usa
     * {@code deleteAllByIdInBatch}: ese emite un DELETE en bloque que Hibernate NO
     * pasa por el {@code @SQLDelete} de la entidad, asi que borraba fisicamente
     * filas que la via individual ({@code delete(id)}) solo deshabilita — y con la
     * fila fisica se perdia el id que
     * {@code findDisabledByRoleAndPermissions}/{@code reactivateAllByIds} usan para
     * reactivar el permiso si vuelve a asignarse.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE role_permissions
            SET enabled = false
            WHERE id IN (:ids)
            """, nativeQuery = true)
    int disableAllByIds(
            @org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);

    /**
     * Baja en bloque acotada al tenant, simetrica de {@link #reactivateAllByIds}.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE role_permissions rp
            JOIN roles r ON r.id = rp.role_id
            SET rp.enabled = false
            WHERE rp.id IN (:ids)
              AND r.company_id = :companyId
            """, nativeQuery = true)
    int disableAllByIds(
            @org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByRole_Id(Long roleId);

    boolean existsByPermission_Id(Long permissionId);
}
