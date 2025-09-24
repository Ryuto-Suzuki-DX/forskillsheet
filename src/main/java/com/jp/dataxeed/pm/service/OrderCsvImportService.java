package com.jp.dataxeed.pm.service;

import java.util.function.Function;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jp.dataxeed.pm.dto.OrderProductRef;
import com.jp.dataxeed.pm.dto.csv.CsvOrderRow;
import com.jp.dataxeed.pm.entity.OrderEntity;
import com.jp.dataxeed.pm.form.user.UserForm;
import com.jp.dataxeed.pm.mapper.OrderMapper;
import com.jp.dataxeed.pm.mapper.OrderProductsMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderCsvImportService {

    private final OrderMapper orderMapper;
    private final OrderProductsMapper orderProductsMapper;
    private final ProductService productService;
    private final UserService userService;
    private final com.jp.dataxeed.pm.helper.UserHelper userHelper;

    // 結果レコード
    public record ImportReport(int ordersCreated, int linesInserted, List<String> errors) {
    }

    // ========= エントリポイント：全件成功のみコミット =========
    public ImportReport importCsvAllOrNothing(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        List<CsvOrderRow> rows = parse(file, errors);
        if (!errors.isEmpty())
            return new ImportReport(0, 0, errors);
        if (rows.isEmpty())
            return new ImportReport(0, 0, List.of("CSVに有効な行がありません。"));

        // 1) 検証だけ先に全部やる（ID存在・権限・フォーマット・商品存在など）
        var validated = validateAll(rows);
        if (!validated.errors.isEmpty()) {
            return new ImportReport(0, 0, validated.errors);
        }

        // 2) 問題なければ一括登録（@Transactional）
        return insertAll(validated.planByKey);
    }

    // ========= 検証結果の中間モデル =========
    private static class InsertPlan {
        String mode;
        Integer partyId;
        String trackingNumber;
        LocalDate deliveryDate;
        String situation;
        Integer locationId;
        Integer adminId;
        Integer workerId;
        Integer inspectorId;
        String adminNote;
        String workerNote;
        String inspectorNote;
        // product_id -> quantity 合計
        Map<Integer, Integer> qtyByProductId = new LinkedHashMap<>();
    }

    private static class ValidationResult {
        Map<String, InsertPlan> planByKey = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
    }

    // ========= 検証のみ =========
    private ValidationResult validateAll(List<CsvOrderRow> rows) {
        ValidationResult vr = new ValidationResult();

        // users を一括取得（権限チェック用）
        Map<Integer, UserForm> usersById = userService.findAll().stream()
                .map(userHelper::EntityToForm)
                .collect(Collectors.toMap(UserForm::getId, u -> u, (a, b) -> a, LinkedHashMap::new));

        // import_key ごとに集計
        Map<String, List<CsvOrderRow>> byKey = rows.stream()
                .collect(Collectors.groupingBy(CsvOrderRow::getImportKey, LinkedHashMap::new, Collectors.toList()));

        for (var e : byKey.entrySet()) {
            String key = e.getKey();
            List<CsvOrderRow> list = e.getValue();
            CsvOrderRow head = list.get(0);

            // 必須項目とルール
            if (!"IN".equalsIgnoreCase(head.getMode()) && !"OUT".equalsIgnoreCase(head.getMode())) {
                vr.errors.add(msg(head, "mode は IN / OUT のみ"));
                continue;
            }
            if (head.getPartyId() == null) {
                vr.errors.add(msg(head, "party_id は必須"));
                continue;
            }
            if ("完了".equals(head.getSituation())) {
                vr.errors.add(msg(head, "situation=完了 はCSVで禁止です"));
                continue;
            }
            LocalDate delivery = parseDate(head.getDeliveryDate());
            if (delivery == null) {
                vr.errors.add(msg(head, "delivery_date は yyyy-MM-dd 形式"));
                continue;
            }
            // admin 権限チェック
            if (head.getAdminId() == null) {
                vr.errors.add(msg(head, "admin_id は必須"));
                continue;
            }
            UserForm admin = usersById.get(head.getAdminId());
            if (admin == null) {
                vr.errors.add(msg(head, "admin_id が未登録: " + head.getAdminId()));
                continue;
            }
            if (!"ADMIN".equals(admin.getRole())) {
                vr.errors.add(msg(head, "admin_id は ADMIN 権限のみ可"));
                continue;
            }
            // worker / inspector は未指定OK（存在すればIDの存在チェック）
            if (head.getWorkerId() != null && !usersById.containsKey(head.getWorkerId())) {
                vr.errors.add(msg(head, "worker_id が未登録: " + head.getWorkerId()));
                continue;
            }
            if (head.getInspectorId() != null && !usersById.containsKey(head.getInspectorId())) {
                vr.errors.add(msg(head, "inspector_id が未登録: " + head.getInspectorId()));
                continue;
            }

            // InsertPlan 作成
            InsertPlan plan = new InsertPlan();
            plan.mode = head.getMode().toUpperCase();
            plan.partyId = head.getPartyId();
            plan.trackingNumber = head.getTrackingNumber();
            plan.deliveryDate = delivery;
            plan.situation = head.getSituation();
            plan.locationId = head.getLocationId();
            plan.adminId = head.getAdminId();
            plan.workerId = head.getWorkerId();
            plan.inspectorId = head.getInspectorId();
            plan.adminNote = head.getAdminNote();
            plan.workerNote = head.getWorkerNote();
            plan.inspectorNote = head.getInspectorNote();

            boolean hasValidDetail = false;

            for (CsvOrderRow r : list) {
                // 明細の必須
                if (isBlank(r.getProductCode())) {
                    vr.errors.add(msg(r, "product_code は必須"));
                    continue;
                }
                Integer pid = productService.getProductIdByCode(r.getProductCode());
                if (pid == null) {
                    vr.errors.add(msg(r, "未登録の product_code: " + r.getProductCode()));
                    continue;
                }
                if (r.getQuantity() == null || r.getQuantity() <= 0) {
                    vr.errors.add(msg(r, "quantity は 1 以上"));
                    continue;
                }
                // 合算
                plan.qtyByProductId.merge(pid, r.getQuantity(), Integer::sum);
                hasValidDetail = true;
            }

            if (!hasValidDetail) {
                vr.errors.add("[import_key=" + key + "] 有効な明細が1件もありません");
                continue;
            }

            vr.planByKey.put(key, plan);
        }

        return vr;
    }

    // ========= 実挿入（全件まとめて1トランザクション） =========
    @Transactional
    protected ImportReport insertAll(Map<String, InsertPlan> planByKey) {
        int orders = 0;
        int lines = 0;

        for (var e : planByKey.entrySet()) {
            InsertPlan p = e.getValue();

            // 既存の採番仕様：DRAFT-<IN/OUT>-time → INSERT → <MODE><id> に更新
            OrderEntity entity = new OrderEntity();
            entity.setOrderCode("DRAFT-" + p.mode + "-" + System.currentTimeMillis());
            entity.setPartyId(p.partyId);
            entity.setTrackingNumber(p.trackingNumber);
            entity.setDeliveryDate(p.deliveryDate);
            entity.setAdminNote(p.adminNote);
            entity.setWarehouseWorkerNote(p.workerNote);
            entity.setQualityInspectorNote(p.inspectorNote);
            entity.setAdminId(p.adminId);
            entity.setWarehouseWorkerId(p.workerId);
            entity.setQualityInspectorId(p.inspectorId);
            entity.setSituation(p.situation);
            entity.setLocationId(p.locationId);
            entity.setHowCsv(true); // CSV識別

            orderMapper.insertOrderToSave(entity);
            int orderId = entity.getId();
            String orderCode = p.mode + orderId;
            orderMapper.updateOrderCodeById(orderId, orderCode);

            // 明細
            if (!p.qtyByProductId.isEmpty()) {
                List<OrderProductRef> refs = p.qtyByProductId.entrySet().stream()
                        .map(en -> {
                            OrderProductRef r = new OrderProductRef();
                            r.setProductId(en.getKey());
                            r.setQuantity(en.getValue());
                            return r;
                        }).toList();
                orderProductsMapper.insertBatch(orderId, refs);
                lines += refs.size();
            }
            orders++;
        }

        // ここまで例外が無ければコミット（=全件成功）
        return new ImportReport(orders, lines, List.of());
    }

    // ========= CSV読み込み =========
    private List<CsvOrderRow> parse(MultipartFile file, List<String> errors) {
        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                var parser = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim())) {
            List<CsvOrderRow> result = new ArrayList<>();
            for (CSVRecord rec : parser) {
                CsvOrderRow r = new CsvOrderRow();
                r.setRowNum(rec.getRecordNumber());
                r.setImportKey(get(rec, "import_key"));
                r.setMode(get(rec, "mode"));
                r.setPartyId(parseInt(get(rec, "party_id")));
                r.setTrackingNumber(get(rec, "tracking_number"));
                r.setDeliveryDate(get(rec, "delivery_date"));
                r.setSituation(get(rec, "situation"));
                r.setLocationId(parseInt(get(rec, "location_id")));
                r.setAdminId(parseInt(get(rec, "admin_id")));
                r.setWorkerId(parseInt(get(rec, "worker_id")));
                r.setInspectorId(parseInt(get(rec, "inspector_id")));
                r.setAdminNote(get(rec, "admin_note"));
                r.setWorkerNote(get(rec, "worker_note"));
                r.setInspectorNote(get(rec, "inspector_note"));
                r.setProductCode(get(rec, "product_code"));
                r.setQuantity(parseInt(get(rec, "quantity")));

                if (isBlank(r.getImportKey())) {
                    errors.add("[" + rec.getRecordNumber() + "] import_key は必須です");
                    continue;
                }
                result.add(r);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            errors.add("CSVの読み込みでエラー: " + e.getMessage());
            return List.of();
        }
    }

    // ========= util =========
    private static String get(CSVRecord rec, String name) {
        return rec.isMapped(name) ? rec.get(name) : null;
    }

    private static Integer parseInt(String s) {
        try {
            return (s == null || s.isBlank()) ? null : Integer.valueOf(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String msg(CsvOrderRow r, String reason) {
        return "[row=" + r.getRowNum() + ", import_key=" + r.getImportKey() + "] " + reason;
    }
}
