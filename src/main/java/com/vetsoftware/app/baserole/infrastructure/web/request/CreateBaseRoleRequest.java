package com.vetsoftware.app.baserole.infrastructure.web.request;

public record CreateBaseRoleRequest(String name, String code, Boolean mandatory) {}
