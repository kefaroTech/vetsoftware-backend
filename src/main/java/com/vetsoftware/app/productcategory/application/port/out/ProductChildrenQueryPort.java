package com.vetsoftware.app.productcategory.application.port.out;

public interface ProductChildrenQueryPort {
    boolean existsActiveByProductCategoryId(Long categoryId);
}
