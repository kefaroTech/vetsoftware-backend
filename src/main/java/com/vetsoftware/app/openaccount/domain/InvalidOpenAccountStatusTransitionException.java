package com.vetsoftware.app.openaccount.domain;

public class InvalidOpenAccountStatusTransitionException extends RuntimeException {
    public InvalidOpenAccountStatusTransitionException(OpenAccountStatus from, OpenAccountStatus to) {
        super("cannot change open account status from " + from + " to " + to);
    }
}
