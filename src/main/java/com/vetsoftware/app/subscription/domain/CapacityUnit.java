package com.vetsoftware.app.subscription.domain;

/**
 * Unidad de la capacidad contratada. Solo tiene sentido cuando el tipo de
 * articulo es {@code CAPACITY}: {@code chk_subscription_items_capacity_unit} lo
 * exige en los dos sentidos, y {@link SubscriptionItem} lo revalida.
 */
public enum CapacityUnit {
    USER, BRANCH, TERMINAL, STORAGE_GB
}
