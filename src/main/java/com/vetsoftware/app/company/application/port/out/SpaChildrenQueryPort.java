package com.vetsoftware.app.company.application.port.out;

public interface SpaChildrenQueryPort {
    boolean existsActiveByCompanyId(Long parentId);
}
