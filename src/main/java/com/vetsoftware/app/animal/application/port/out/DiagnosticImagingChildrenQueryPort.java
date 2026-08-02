package com.vetsoftware.app.animal.application.port.out;

public interface DiagnosticImagingChildrenQueryPort {
  boolean existsActiveByAnimalId(Long parentId);
}
