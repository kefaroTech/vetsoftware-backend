package com.vetsoftware.app.company.application.port.out;

public interface DiagnosticImagingChildrenQueryPort {
    boolean existsActiveByCompanyId(Long parentId);
}
