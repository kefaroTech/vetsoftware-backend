package com.vetsoftware.app.membership.application.port.out;

import java.util.List;

public interface ModuleValidationPort {
    void validateAllExist(List<Long> moduleIds);
}
