package com.vetsoftware.app.auth.application.command;

public record LoginSystemUserCommand(String code, String password) {
}
