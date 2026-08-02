package com.vetsoftware.app.animal.application.port.out;

public interface DayCareChildrenQueryPort {
  boolean existsActiveByAnimalId(Long parentId);
}
