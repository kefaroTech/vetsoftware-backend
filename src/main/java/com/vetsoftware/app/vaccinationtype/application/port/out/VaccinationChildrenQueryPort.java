package com.vetsoftware.app.vaccinationtype.application.port.out;

public interface VaccinationChildrenQueryPort {
  boolean existsActiveByVaccinationTypeId(Long parentId);
}
