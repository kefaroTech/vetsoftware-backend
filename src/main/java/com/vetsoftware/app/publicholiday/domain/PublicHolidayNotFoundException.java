package com.vetsoftware.app.publicholiday.domain;

/** El festivo pedido no existe. Mapea a 404. */
public class PublicHolidayNotFoundException extends RuntimeException {

    public PublicHolidayNotFoundException(Long id) {
        super("Public holiday not found: " + id);
    }
}
