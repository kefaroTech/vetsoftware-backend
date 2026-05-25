package com.vetsoftware.app.diagnosticimagingtype.application.port.out;

public interface DiagnosticImagingChildrenQueryPort {
    boolean existsActiveByDiagnosticImagingTypeId(Long parentId);
}
