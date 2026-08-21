package vn.naitei.nhom3.expensemanagement.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.naitei.nhom3.expensemanagement.common.response.ApiResponse;
import vn.naitei.nhom3.expensemanagement.dto.importexport.ExportEntityType;
import vn.naitei.nhom3.expensemanagement.dto.importexport.ImportEntityType;
import vn.naitei.nhom3.expensemanagement.dto.importexport.ImportResultResponse;
import vn.naitei.nhom3.expensemanagement.exception.BadRequestException;
import vn.naitei.nhom3.expensemanagement.service.ExportService;
import vn.naitei.nhom3.expensemanagement.service.ImportService;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ImportExportAdminController {

    private final ImportService importService;
    private final ExportService exportService;

    @PostMapping(value = "/import/{entity}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportResultResponse>> importCsv(
            @PathVariable("entity") String entity,
            @RequestPart("file") MultipartFile file) {
        ImportEntityType entityType = parseEntityType(ImportEntityType.class, entity);
        ImportResultResponse response = importService.importCsv(entityType, file);
        return ResponseEntity.ok(ApiResponse.success("Import CSV hoàn tất", response));
    }

    @GetMapping("/export/{entity}")
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable("entity") String entity,
            @RequestParam(value = "format", defaultValue = "csv") String format) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw new BadRequestException("Chỉ hỗ trợ format=csv");
        }
        ExportEntityType entityType = parseEntityType(ExportEntityType.class, entity);
        byte[] content = exportService.exportCsv(entityType);
        String fileName = exportService.suggestedFileName(entityType);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content);
    }

    private <E extends Enum<E>> E parseEntityType(Class<E> type, String raw) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("entity không hợp lệ: " + raw);
        }
    }
}
