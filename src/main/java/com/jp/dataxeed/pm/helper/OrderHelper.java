package com.jp.dataxeed.pm.helper;

import org.springframework.stereotype.Component;

import com.jp.dataxeed.pm.dto.OrderWithDetailsDto;
import com.jp.dataxeed.pm.entity.OrderEntity;

@Component
public class OrderHelper {

    // OrderWithDetailsDto → OrderEntity
    public OrderEntity dtoToEntity(OrderWithDetailsDto orderWithDetailsDto) {
        if (orderWithDetailsDto == null) {
            return null;
        }
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(orderWithDetailsDto.getId());
        orderEntity.setOrderCode(orderWithDetailsDto.getOrderCode());
        orderEntity.setPartyId(orderWithDetailsDto.getPartyId());
        orderEntity.setTrackingNumber(orderWithDetailsDto.getTrackingNumber());
        orderEntity.setDeliveryDate(orderWithDetailsDto.getDeliveryDate()); // ★追加: 配送日
        orderEntity.setAdminNote(orderWithDetailsDto.getAdminNote());
        orderEntity.setWarehouseWorkerNote(orderWithDetailsDto.getWarehouseWorkerNote());
        orderEntity.setQualityInspectorNote(orderWithDetailsDto.getQualityInspectorNote());
        orderEntity.setAdminId(orderWithDetailsDto.getAdminId());
        orderEntity.setWarehouseWorkerId(orderWithDetailsDto.getWarehouseWorkerId());
        orderEntity.setQualityInspectorId(orderWithDetailsDto.getQualityInspectorId());
        orderEntity.setSituation(orderWithDetailsDto.getSituation());
        orderEntity.setLocationId(orderWithDetailsDto.getLocationId()); // ★追加
        orderEntity.setCreatedAt(orderWithDetailsDto.getCreatedAt());
        orderEntity.setUpdatedAt(orderWithDetailsDto.getUpdatedAt());
        return orderEntity;
    }

    public OrderWithDetailsDto entityToDto(OrderEntity orderEntity) {
        if (orderEntity == null) {
            return null;
        }
        OrderWithDetailsDto dto = new OrderWithDetailsDto();
        dto.setId(orderEntity.getId());
        dto.setOrderCode(orderEntity.getOrderCode());
        dto.setPartyId(orderEntity.getPartyId());
        dto.setTrackingNumber(orderEntity.getTrackingNumber());
        dto.setDeliveryDate(orderEntity.getDeliveryDate()); // ★これが無いと表示されない
        dto.setAdminNote(orderEntity.getAdminNote());
        dto.setWarehouseWorkerNote(orderEntity.getWarehouseWorkerNote());
        dto.setQualityInspectorNote(orderEntity.getQualityInspectorNote());
        dto.setAdminId(orderEntity.getAdminId());
        dto.setWarehouseWorkerId(orderEntity.getWarehouseWorkerId());
        dto.setQualityInspectorId(orderEntity.getQualityInspectorId());
        dto.setSituation(orderEntity.getSituation());
        dto.setLocationId(orderEntity.getLocationId());
        dto.setCreatedAt(orderEntity.getCreatedAt());
        dto.setUpdatedAt(orderEntity.getUpdatedAt());
        return dto;
    }
}
