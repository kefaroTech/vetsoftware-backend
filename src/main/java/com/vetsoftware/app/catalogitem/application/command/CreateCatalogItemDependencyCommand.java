package com.vetsoftware.app.catalogitem.application.command;

import com.vetsoftware.app.catalogitem.domain.RelationType;

public record CreateCatalogItemDependencyCommand(Long catalogItemId, Long relatedItemId,
        RelationType relationType, String note) {
}
