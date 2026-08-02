package com.vetsoftware.app.state.application.port.out;

public interface CityChildrenQueryPort {
  boolean existsActiveByStateId(Long parentId);
}
