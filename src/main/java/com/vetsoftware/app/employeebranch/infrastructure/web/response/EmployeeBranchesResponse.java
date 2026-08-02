package com.vetsoftware.app.employeebranch.infrastructure.web.response;

import java.util.List;

public record EmployeeBranchesResponse(Long employeeId, List<Long> branchIds) {
}
