package com.vetsoftware.app.company.application.port.out;

public interface AnimalChildrenQueryPort {
    boolean existsActiveByCompanyId(Long parentId);
}
