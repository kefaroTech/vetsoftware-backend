package com.vetsoftware.app.laboratorytestfile.domain;

public record LaboratoryTestStoragePathRef(Long companyId, Long ownerId, Long animalId,
        String animalName) {
    public LaboratoryTestStoragePathRef {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (ownerId == null)
            throw new IllegalArgumentException("ownerId is required");
        if (animalId == null)
            throw new IllegalArgumentException("animalId is required");
        if (animalName == null || animalName.isBlank())
            throw new IllegalArgumentException("animalName is required");
    }
}
