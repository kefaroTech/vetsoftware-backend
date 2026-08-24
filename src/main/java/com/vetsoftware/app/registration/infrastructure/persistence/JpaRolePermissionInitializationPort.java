package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.entitlement.application.port.out.AdminPermissionReconciliationPort;
import com.vetsoftware.app.entitlement.infrastructure.persistence.CompanyEntitlementJpaRepository;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import com.vetsoftware.app.registration.application.port.out.RolePermissionInitializationPort;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionCachePort;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaEntity;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reparte a la empresa recien creada los permisos base de un rol base,
 * filtrados por lo que su <b>contrato</b> le concede.
 *
 * <p>
 * Antes el filtro salia de {@code membership_sub_modules} —«que submodulos abre
 * el plan de esta empresa»—. Ahora sale de {@code company_entitlements}, que es
 * la tabla derivada del contrato vigente, y solo cuentan los niveles
 * {@code FULL} y {@code READ_ONLY}: un {@code NONE} es explicitamente «este
 * submodulo no existe para esta empresa».
 */
@Component
public class JpaRolePermissionInitializationPort
        implements
            RolePermissionInitializationPort,
            AdminPermissionReconciliationPort {

    private static final Logger log = LoggerFactory
            .getLogger(JpaRolePermissionInitializationPort.class);

    /**
     * {@code NONE} queda fuera a proposito: es la baja de un modulo, y dar de baja
     * jamas borra datos, solo baja el nivel de acceso. Un permiso derivado de un
     * {@code NONE} volveria a abrir la pantalla que la baja acaba de cerrar.
     */
    private static final List<String> NIVELES_CONCEDIDOS = List.of("FULL", "READ_ONLY");
    private static final String ADMIN_CODE = "ADMIN";
    private static final String FULL = "FULL";
    private static final String READ_ONLY = "READ_ONLY";
    private static final String READ_SUFFIX = ".read";

    private final CompanyEntitlementJpaRepository companyEntitlementJpaRepository;
    private final BaseRolePermissionJpaRepository baseRolePermissionJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;
    private final RolePermissionJpaRepository rolePermissionJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final BaseRoleJpaRepository baseRoleJpaRepository;
    private final PermissionCachePort permissionCachePort;
    private final Clock clock;

    public JpaRolePermissionInitializationPort(
            CompanyEntitlementJpaRepository companyEntitlementJpaRepository,
            BaseRolePermissionJpaRepository baseRolePermissionJpaRepository,
            PermissionJpaRepository permissionJpaRepository,
            RolePermissionJpaRepository rolePermissionJpaRepository,
            CompanyJpaRepository companyJpaRepository, RoleJpaRepository roleJpaRepository,
            BaseRoleJpaRepository baseRoleJpaRepository, PermissionCachePort permissionCachePort,
            Clock clock) {
        this.companyEntitlementJpaRepository = companyEntitlementJpaRepository;
        this.baseRolePermissionJpaRepository = baseRolePermissionJpaRepository;
        this.permissionJpaRepository = permissionJpaRepository;
        this.rolePermissionJpaRepository = rolePermissionJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
        this.baseRoleJpaRepository = baseRoleJpaRepository;
        this.permissionCachePort = permissionCachePort;
        this.clock = clock;
    }

    @Override
    public void initializeForRole(Long roleId, Long companyId, Long baseRoleId) {
        Set<Long> subModuleIds = new HashSet<>(companyEntitlementJpaRepository
                .findGrantedSubModuleIdsByCompanyId(companyId, NIVELES_CONCEDIDOS));
        if (subModuleIds.isEmpty()) {
            // Antes esto era un `return` mudo. Con el modelo nuevo es imposible por
            // construccion —toda empresa nace con contrato, y el contrato deriva sus
            // entitlements en la misma transaccion—, asi que llegar aqui significa que
            // algo de esa cadena se rompio. Si se deja pasar, la empresa nace sin un
            // solo permiso, entra al sistema y no puede hacer nada; el sintoma aparece
            // dias despues, disfrazado de problema de permisos del usuario, y no hay
            // rastro que lleve hasta aqui. Un ticket de soporte irresoluble.
            throw new IllegalStateException(("La empresa %d no tiene ningun submodulo concedido "
                    + "(company_entitlements con access_level FULL o READ_ONLY), asi que sus roles "
                    + "nacerian sin un solo permiso. Toda empresa nace con un contrato: revisa que "
                    + "el alta haya creado su subscriptions y que el recalculo de "
                    + "company_entitlements haya corrido en la misma transaccion.")
                    .formatted(companyId));
        }

        List<BasePermissionJpaEntity> applicableBasePermissions = baseRolePermissionJpaRepository
                .findByBaseRoleId(baseRoleId).stream()
                .map(BaseRolePermissionJpaEntity::getBasePermission)
                .filter(bp -> subModuleIds.contains(bp.getSubModule().getId())).toList();
        if (applicableBasePermissions.isEmpty()) {
            // Esta si es legitima: un rol base cuyos permisos caen todos fuera de lo que
            // la empresa tiene concedido queda como plantilla vacia. No es un fallo, pero
            // tampoco es invisible.
            log.warn("El rol {} de la empresa {} nace sin permisos: ninguno de los permisos base "
                    + "del rol base {} cae dentro de los {} submodulos concedidos por su "
                    + "contrato.", roleId, companyId, baseRoleId, subModuleIds.size());
            return;
        }

        CompanyJpaEntity companyRef = companyJpaRepository.getReferenceById(companyId);
        List<PermissionJpaEntity> resolvedPermissions = new ArrayList<>(
                applicableBasePermissions.size());
        List<PermissionJpaEntity> permissionsToCreate = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);

        for (BasePermissionJpaEntity bp : applicableBasePermissions) {
            PermissionJpaEntity permission = permissionJpaRepository
                    .findByCompanyIdAndCode(companyId, bp.getCode()).orElseGet(() -> {
                        PermissionJpaEntity entity = new PermissionJpaEntity();
                        entity.setName(bp.getName());
                        entity.setCode(bp.getCode());
                        entity.setCompany(companyRef);
                        entity.setSubModule(bp.getSubModule());
                        entity.setCreatedDate(now);
                        permissionsToCreate.add(entity);
                        return entity;
                    });
            resolvedPermissions.add(permission);
        }

        if (!permissionsToCreate.isEmpty()) {
            permissionJpaRepository.saveAll(permissionsToCreate);
        }

        RoleJpaEntity roleRef = roleJpaRepository.getReferenceById(roleId);
        List<RolePermissionJpaEntity> rolePermissions = resolvedPermissions.stream().map(p -> {
            RolePermissionJpaEntity rp = new RolePermissionJpaEntity();
            rp.setRole(roleRef);
            rp.setPermission(p);
            rp.setCreatedDate(now);
            return rp;
        }).toList();
        rolePermissionJpaRepository.saveAll(rolePermissions);
    }

    /**
     * Reconcilia solo la proyeccion ADMIN que procede de la plantilla base. Los
     * permisos personalizados del rol no se tocan. La baja es logica para conservar
     * la identidad de los vinculos y poder reactivarlos en un upgrade posterior.
     */
    @Override
    public void reconcile(Long companyId, LocalDateTime recalculatedAt) {
        RoleJpaEntity adminRole = roleJpaRepository.findByCompanyIdAndCode(companyId, ADMIN_CODE)
                .orElse(null);
        if (adminRole == null) {
            // Durante el alta el contrato se deriva antes de crear los roles. La
            // primitiva de registro materializa el rol cuando ya existe.
            return;
        }

        Long adminBaseRoleId = baseRoleJpaRepository.findByCode(ADMIN_CODE)
                .orElseThrow(() -> new IllegalStateException("BaseRole 'ADMIN' not configured"))
                .getId();
        List<BasePermissionJpaEntity> templates = baseRolePermissionJpaRepository
                .findByBaseRoleId(adminBaseRoleId).stream()
                .map(BaseRolePermissionJpaEntity::getBasePermission).toList();

        Map<Long, String> accessBySubModule = companyEntitlementJpaRepository
                .findAllByCompany_Id(companyId).stream()
                .filter(e -> !e.getValidFrom().isAfter(recalculatedAt))
                .filter(e -> e.getValidUntil() == null || e.getValidUntil().isAfter(recalculatedAt))
                .collect(Collectors.toMap(e -> e.getSubModule().getId(), e -> e.getAccessLevel(),
                        (left, right) -> right));

        Set<String> baseCodes = templates.stream().map(BasePermissionJpaEntity::getCode)
                .collect(Collectors.toSet());
        List<BasePermissionJpaEntity> allowedTemplates = templates.stream()
                .filter(template -> grants(accessBySubModule.get(template.getSubModule().getId()),
                        template.getCode()))
                .toList();

        Map<String, PermissionJpaEntity> activePermissions = permissionJpaRepository
                .findAllByCompanyId(companyId).stream().collect(Collectors.toMap(
                        PermissionJpaEntity::getCode, Function.identity(), (left, right) -> left));
        Map<String, Long> desiredPermissionIds = new HashMap<>();
        boolean changed = false;

        for (BasePermissionJpaEntity template : allowedTemplates) {
            PermissionJpaEntity permission = activePermissions.get(template.getCode());
            if (permission == null) {
                var disabledId = permissionJpaRepository.findDisabledIdByCompanyIdAndCode(companyId,
                        template.getCode());
                if (disabledId.isPresent()) {
                    if (permissionJpaRepository.reactivate(disabledId.get(), companyId) != 1) {
                        throw new IllegalStateException(
                                "Could not reactivate permission " + disabledId.get());
                    }
                    permission = permissionJpaRepository
                            .findByIdAndCompany_Id(disabledId.get(), companyId)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Reactivated permission is not visible: " + disabledId.get()));
                } else {
                    permission = new PermissionJpaEntity();
                    permission.setName(template.getName());
                    permission.setCode(template.getCode());
                    permission.setCompany(companyJpaRepository.getReferenceById(companyId));
                    permission.setSubModule(template.getSubModule());
                    permission.setCreatedDate(recalculatedAt);
                    permission = permissionJpaRepository.save(permission);
                }
                changed = true;
            }
            desiredPermissionIds.put(template.getCode(), permission.getId());
        }

        List<RolePermissionJpaEntity> activeLinks = rolePermissionJpaRepository
                .findAllByRoleId(adminRole.getId());
        Set<Long> linkedPermissionIds = activeLinks.stream()
                .map(link -> link.getPermission().getId()).collect(Collectors.toSet());
        RoleJpaEntity roleRef = roleJpaRepository.getReferenceById(adminRole.getId());

        for (Long permissionId : desiredPermissionIds.values()) {
            if (linkedPermissionIds.contains(permissionId)) {
                continue;
            }
            var disabledLinkId = rolePermissionJpaRepository
                    .findDisabledIdByRoleAndPermission(adminRole.getId(), permissionId);
            if (disabledLinkId.isPresent()) {
                if (rolePermissionJpaRepository.reactivate(disabledLinkId.get(), companyId) != 1) {
                    throw new IllegalStateException(
                            "Could not reactivate role permission " + disabledLinkId.get());
                }
            } else {
                RolePermissionJpaEntity link = new RolePermissionJpaEntity();
                link.setRole(roleRef);
                link.setPermission(permissionJpaRepository.getReferenceById(permissionId));
                link.setCreatedDate(recalculatedAt);
                rolePermissionJpaRepository.save(link);
            }
            changed = true;
        }

        Set<String> desiredCodes = desiredPermissionIds.keySet();
        List<Long> obsoleteLinkIds = activeLinks.stream()
                .filter(link -> baseCodes.contains(link.getPermission().getCode()))
                .filter(link -> !desiredCodes.contains(link.getPermission().getCode()))
                .map(RolePermissionJpaEntity::getId).toList();
        if (!obsoleteLinkIds.isEmpty()) {
            rolePermissionJpaRepository.disableAllByIds(obsoleteLinkIds, companyId);
            changed = true;
        }

        if (changed) {
            // Es una optimizacion de la proxima request, no la barrera de seguridad:
            // el resolver efectivo vuelve a consultar company_entitlements siempre.
            permissionCachePort.evictByRoleId(adminRole.getId());
        }
    }

    private static boolean grants(String accessLevel, String permissionCode) {
        return FULL.equals(accessLevel)
                || READ_ONLY.equals(accessLevel) && permissionCode.endsWith(READ_SUFFIX);
    }
}
