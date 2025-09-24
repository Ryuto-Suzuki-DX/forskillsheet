package com.jp.dataxeed.pm.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jp.dataxeed.pm.dto.OrderProductRef;
import com.jp.dataxeed.pm.dto.StockUpdateParam;
import com.jp.dataxeed.pm.mapper.StockMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockMapper stockMapper;

    /**
     * 在庫更新の共通入口。
     * - 未完了 -> 完了 : afterItems を使って反映（IN=加算, OUT=減算）
     * - 完了 -> 未完了/他 : beforeItems を使って巻き戻し（IN=減算, OUT=加算）
     *
     * @param previousSituation 変更前の状況（null可）
     * @param currentSituation  変更後の状況（null可）
     * @param modeOrNull        "IN" / "OUT"（null 可。null の場合は orderCodePrefix から判定）
     * @param beforeItems       変更前の明細（productId, quantity）合算済み
     * @param afterItems        変更後の明細（productId, quantity）合算済み
     * @param orderCodePrefix   例: "IN123", "OUT45"（mode が null のときの判定用。null可）
     */
    @Transactional
    public void handleCompletionTransition(
            String previousSituation,
            String currentSituation,
            String modeOrNull,
            List<OrderProductRef> beforeItems,
            List<OrderProductRef> afterItems,
            String orderCodePrefix /* 追加：呼び出し側が渡せるなら渡す */) {

        final String mode = (modeOrNull != null) ? modeOrNull : resolveModeFromCode(orderCodePrefix);
        if (mode == null) {
            // 判定不能なら何もしない or 例外。ここでは安全側で何もしない。
            return;
        }

        boolean wasCompleted = "完了".equals(previousSituation);
        boolean willCompleted = "完了".equals(currentSituation);

        if (!wasCompleted && willCompleted) {
            apply(mode, safe(afterItems));
        } else if (wasCompleted && !willCompleted) {
            revert(mode, safe(beforeItems));
        }
        // 完了->完了 / 未完了->未完了 は何もしない
    }

    // ===== 内部実装 =====

    private List<OrderProductRef> safe(List<OrderProductRef> list) {
        return (list != null) ? list : List.of();
    }

    private void apply(String mode, List<OrderProductRef> items) {
        final boolean isOut = "OUT".equalsIgnoreCase(mode);
        for (OrderProductRef it : items) {
            final Integer productId = it.getProductId();
            final long delta = it.getQuantity() == null ? 0L : it.getQuantity();
            if (productId == null || delta <= 0)
                continue;

            if (isOut) {
                // 在庫割れガード
                Long current = stockMapper.selectForUpdate(productId);
                if (current != null && current < delta) {
                    throw new IllegalStateException("在庫不足: productId=" + productId);
                }
                StockUpdateParam p = new StockUpdateParam();
                p.setProductId(productId);
                p.setDelta(delta);
                stockMapper.upsertSub(p);
            } else {
                StockUpdateParam p = new StockUpdateParam();
                p.setProductId(productId);
                p.setDelta(delta);
                stockMapper.upsertAdd(p);
            }
        }
    }

    private void revert(String mode, List<OrderProductRef> items) {
        final boolean wasOut = "OUT".equalsIgnoreCase(mode);
        for (OrderProductRef it : items) {
            final Integer productId = it.getProductId();
            final long delta = it.getQuantity() == null ? 0L : it.getQuantity();
            if (productId == null || delta <= 0)
                continue;

            if (wasOut) {
                // 元が出庫 → 巻き戻しは加算
                StockUpdateParam p = new StockUpdateParam();
                p.setProductId(productId);
                p.setDelta(delta);
                stockMapper.upsertAdd(p);
            } else {
                // 元が入庫 → 巻き戻しは減算（割れガード）
                Long current = stockMapper.selectForUpdate(productId);
                if (current != null && current < delta) {
                    throw new IllegalStateException("在庫不足(巻き戻し不可): productId=" + productId);
                }
                StockUpdateParam p = new StockUpdateParam();
                p.setProductId(productId);
                p.setDelta(delta);
                stockMapper.upsertSub(p);
            }
        }
    }

    /** "INxxxx" / "OUTxxxx" の接頭辞から判定（判定不可なら null） */
    private static String resolveModeFromCode(String code) {
        if (code == null)
            return null;
        String c = code.trim().toUpperCase();
        if (c.startsWith("IN"))
            return "IN";
        if (c.startsWith("OUT"))
            return "OUT";
        return null;
    }

}
