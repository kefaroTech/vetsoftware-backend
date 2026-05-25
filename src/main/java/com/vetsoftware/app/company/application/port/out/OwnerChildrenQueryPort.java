package com.vetsoftware.app.company.application.port.out;

public interface OwnerChildrenQueryPort {
    boolean existsActiveByCompanyId(Long parentId);
}
