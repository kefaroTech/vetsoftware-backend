package com.vetsoftware.app.branch.application.command;

public record CreateBranchCommand(
    String name, String code, String address, String phone, Long cityId, Long companyId) {}
