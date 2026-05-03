package com.vetsoftware.app.auth.application.dto;

import java.util.Set;

public sealed interface AuthContext
    permits EmployeeContext, SystemUserContext, SystemContext {

    Set<String> permissions();
}
