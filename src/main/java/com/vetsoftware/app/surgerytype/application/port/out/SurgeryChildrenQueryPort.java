package com.vetsoftware.app.surgerytype.application.port.out;

public interface SurgeryChildrenQueryPort {
    boolean existsActiveBySurgeryTypeId(Long parentId);
}
