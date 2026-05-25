package com.vetsoftware.app.company.application.port.out;

public interface DewormingChildrenQueryPort {
    boolean existsActiveByCompanyId(Long parentId);
}
