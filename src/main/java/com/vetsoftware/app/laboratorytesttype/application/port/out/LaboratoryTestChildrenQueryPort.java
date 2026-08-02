package com.vetsoftware.app.laboratorytesttype.application.port.out;

public interface LaboratoryTestChildrenQueryPort {
  boolean existsActiveByLaboratoryTestTypeId(Long parentId);
}
