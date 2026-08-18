package com.vetsoftware.app.role.application.port.out;

public interface RolePermissionChildrenCascadePort {
    /**
     * Baja en cascada de los permisos del rol, acotada a la empresa. El
     * {@code companyId} viaja hasta el {@code WHERE} a proposito: el
     * {@code DeleteRoleService} ya valido que el rol es del tenant, pero esa
     * comprobacion vive en Java y esta va en el SQL de una mutacion en bloque.
     */
    int deactivateAllByRoleId(Long roleId, Long companyId);
}
