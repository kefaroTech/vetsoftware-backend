package com.vetsoftware.app.specie.application.port.out;

public interface AnimalChildrenQueryPort {
  boolean existsActiveBySpecieId(Long parentId);
}
