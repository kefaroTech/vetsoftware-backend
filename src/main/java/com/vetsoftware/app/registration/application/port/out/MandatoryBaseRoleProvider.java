package com.vetsoftware.app.registration.application.port.out;

import java.util.List;

public interface MandatoryBaseRoleProvider {
    List<BaseRoleData> findMandatory();

    record BaseRoleData(String name, String code) {}
}
