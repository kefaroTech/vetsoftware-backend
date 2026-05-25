package com.vetsoftware.app.city.application.port.out;

public interface OwnerChildrenQueryPort {
    boolean existsActiveByCityId(Long parentId);
}
