package com.vetsoftware.app.company.application.port.out;

public interface DayCareChildrenQueryPort {
    boolean existsActiveByCompanyId(Long parentId);
}
