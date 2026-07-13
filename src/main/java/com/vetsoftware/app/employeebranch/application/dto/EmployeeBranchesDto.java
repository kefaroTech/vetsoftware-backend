package com.vetsoftware.app.employeebranch.application.dto;

import java.util.List;

public record EmployeeBranchesDto(Long employeeId, List<Long> branchIds) {}
