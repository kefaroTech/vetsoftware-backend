package com.vetsoftware.app.publishadminpermissions.application.port.out;

import java.util.List;

public interface CompanyCatalogQueryPort {
    List<CompanyAdminContext> findAllWithAdminRole();
}
