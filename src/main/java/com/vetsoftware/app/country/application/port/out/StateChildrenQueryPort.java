package com.vetsoftware.app.country.application.port.out;

public interface StateChildrenQueryPort {
  boolean existsActiveByCountryId(Long parentId);
}
