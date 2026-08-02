package com.vetsoftware.app.animal.application.port.out;

public interface SurgeryChildrenQueryPort {
  boolean existsActiveByAnimalId(Long parentId);
}
