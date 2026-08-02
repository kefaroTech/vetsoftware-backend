package com.vetsoftware.app.servicecategory.domain;

public class ServiceCategoryNameAlreadyExistsException extends RuntimeException {
    public ServiceCategoryNameAlreadyExistsException(String name) {
        super("Ya existe una categoría de servicio activa con el nombre '" + name
                + "' en esta empresa.");
    }
}
