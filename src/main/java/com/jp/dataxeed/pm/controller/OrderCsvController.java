package com.jp.dataxeed.pm.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import com.jp.dataxeed.pm.service.OrderCsvImportService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/csv")
@RequiredArgsConstructor
public class OrderCsvController {

    private final OrderCsvImportService csvImportService;

    @GetMapping("")
    public String showCsvPage() {
        return "csv/csv";
    }

    // サンプルCSV ダウンロード
    @GetMapping("/sample")
    public ResponseEntity<byte[]> downloadSample() {
        String sample = """
                import_key,mode,party_id,tracking_number,delivery_date,situation,location_id,admin_id,worker_id,inspector_id,admin_note,worker_note,inspector_note,product_code,quantity
                JOB-20250818-001,IN,1,TNK-001,2025-08-25,作業中,2,10,11,12,入庫分です,,,"PRD-AAA",10
                JOB-20250818-001,IN,1,TNK-001,2025-08-25,作業中,2,10,11,12,入庫分です,,,"PRD-BBB",5
                JOB-20250818-002,OUT,3,SLP-999,2025-08-26,検品待ち,4,10,,,"至急",注意,,PRD-CCC,2
                """
                .stripIndent();

        byte[] bytes = sample.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import_sample.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    // 取り込み（全件成功のみコミット）
    @PostMapping("/import")
    public String importCsv(MultipartFile file, Model model) {
        if (file == null || file.isEmpty()) {
            model.addAttribute("errors", List.of("CSVファイルを選択してください。"));
            return "csv/csv";
        }
        var report = csvImportService.importCsvAllOrNothing(file);
        if (!report.errors().isEmpty()) {
            model.addAttribute("errors", report.errors());
        } else {
            model.addAttribute("result",
                    String.format("注文 %d 件 / 明細 %d 行を登録しました。", report.ordersCreated(), report.linesInserted()));
        }
        return "csv/csv";
    }
}
