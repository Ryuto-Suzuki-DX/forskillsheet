package com.jp.dataxeed.pm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.jp.dataxeed.pm.dto.OrderWithDetailsDto;
import com.jp.dataxeed.pm.dto.PictureDto;
import com.jp.dataxeed.pm.entity.OrderEntity;
import com.jp.dataxeed.pm.form.order.SearchOrderForm;

public interface OrderMapper {
    // findById
    OrderEntity findById(int id);

    // updateOrderToSave
    void updateOrderToSave(OrderEntity orderEntity);

    // insertOrderToSave
    void insertOrderToSave(OrderEntity orderEntity);

    // getOrderWithDetailsById
    OrderWithDetailsDto getOrderWithDetailsById(int orderId);

    // searchOrderWithDetailsDto
    List<OrderWithDetailsDto> searchOrderWithDetailsDto(SearchOrderForm searchOrderForm);

    List<PictureDto> findPicturesByOrderId(int orderId);
    // Other methods can be added as needed

    // deleteOrderById
    void deleteOrderById(int orderId);

    // updateOrderCodeById
    int updateOrderCodeById(@Param("id") int orderId, @Param("orderCode") String orderCode);

    // ★追加：ドラフトINSERT（useGeneratedKeys で id 採番）
    void insertEmptyOrder(OrderEntity e);

    void deleteUnfinishedOrders();
}
