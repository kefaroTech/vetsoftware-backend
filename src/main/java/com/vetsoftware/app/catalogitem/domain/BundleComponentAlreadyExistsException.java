package com.vetsoftware.app.catalogitem.domain;

public class BundleComponentAlreadyExistsException extends RuntimeException {
    public BundleComponentAlreadyExistsException(Long bundleItemId, Long componentItemId) {
        super("Bundle " + bundleItemId + " already contains component " + componentItemId);
    }
}
