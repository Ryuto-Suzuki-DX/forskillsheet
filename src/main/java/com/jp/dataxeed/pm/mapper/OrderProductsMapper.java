package com.jp.dataxeed.pm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.jp.dataxeed.pm.dto.OrderProductRef;

public interface OrderProductsMapper {

    /** 表示用：注文に紐づく (product_id, quantity) 一覧 */
    List<OrderProductRef> findRefsByOrderId(int orderId);

    /** 全置換前の削除 */
    void deleteByOrderId(int orderId);

    /** 数量つき一括INSERT（全置換） */
    void insertBatch(int orderId, List<OrderProductRef> items);

    int countByOrderId(@Param("orderId") int orderId);
}
