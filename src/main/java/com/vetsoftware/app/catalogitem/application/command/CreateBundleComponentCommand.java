package com.vetsoftware.app.catalogitem.application.command;

public record CreateBundleComponentCommand(Long bundleItemId, Long componentItemId, int quantity) {
}
