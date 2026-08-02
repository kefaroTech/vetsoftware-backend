package com.vetsoftware.app.animal.application.port.out;

public interface DewormingChildrenQueryPort {
  boolean existsActiveByAnimalId(Long parentId);
}
