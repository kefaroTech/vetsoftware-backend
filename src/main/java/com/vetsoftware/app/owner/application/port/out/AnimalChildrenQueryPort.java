package com.vetsoftware.app.owner.application.port.out;

public interface AnimalChildrenQueryPort {
  boolean existsActiveByOwnerId(Long parentId);
}
