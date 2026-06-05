package com.vetsoftware.app.servicecategory.application.port.out;

public interface ServiceChildrenQueryPort {
    boolean existsActiveByServiceCategoryId(Long categoryId);
}
