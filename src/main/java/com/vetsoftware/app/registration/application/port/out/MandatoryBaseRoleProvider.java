package com.vetsoftware.app.registration.application.port.out;

import java.util.List;

public interface MandatoryBaseRoleProvider {
    List<BaseRoleData> findMandatory();

    record BaseRoleData(Long id, String name, String code) {}
}
