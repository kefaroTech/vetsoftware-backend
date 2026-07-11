package com.vetsoftware.app.branch.application.command;

public record UpdateBranchCommand(
        Long id, String name, String code, String address, String phone, Long cityId, Long companyId
) {}
