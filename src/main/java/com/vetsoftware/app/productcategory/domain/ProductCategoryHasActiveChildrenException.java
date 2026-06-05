package com.vetsoftware.app.productcategory.domain;

public class ProductCategoryHasActiveChildrenException extends RuntimeException {
    public ProductCategoryHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete productcategory " + id + ": has active " + childType + " children");
    }
}
