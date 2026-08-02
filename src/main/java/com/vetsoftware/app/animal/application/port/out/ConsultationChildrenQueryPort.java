package com.vetsoftware.app.animal.application.port.out;

public interface ConsultationChildrenQueryPort {
  boolean existsActiveByAnimalId(Long parentId);
}
