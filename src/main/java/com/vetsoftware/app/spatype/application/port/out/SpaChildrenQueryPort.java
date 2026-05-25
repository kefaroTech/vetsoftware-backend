package com.vetsoftware.app.spatype.application.port.out;

public interface SpaChildrenQueryPort {
    boolean existsActiveBySpaTypeId(Long parentId);
}
