package com.vetsoftware.app.owner.application.command;

public record UpdateOwnerCommand(
        Long id, String name, String email, String document, String address,
        String phone, Long cityId, Long companyId
) {}
