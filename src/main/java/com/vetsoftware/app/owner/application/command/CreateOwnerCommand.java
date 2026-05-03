package com.vetsoftware.app.owner.application.command;

public record CreateOwnerCommand(
        String name, String email, String document, String address,
        String phone, Long cityId, Long companyId
) {}
