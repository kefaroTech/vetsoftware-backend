package com.vetsoftware.app.inventory.application.command;

import java.time.LocalDate;

/** Kardex paginado de un producto en una sede, con rango de fechas opcional (auditable). */
public record SearchKardexCommand(Long companyId, Long branchId, Long productId, LocalDate from, LocalDate to,
                                  int page, int pageSize) {}
