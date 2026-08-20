package vn.naitei.nhom3.expensemanagement.service;

import vn.naitei.nhom3.expensemanagement.dto.importexport.ExportEntityType;

public interface ExportService {

    byte[] exportCsv(ExportEntityType entityType);

    String suggestedFileName(ExportEntityType entityType);
}
