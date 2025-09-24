package com.jp.dataxeed.pm.service;

import java.time.LocalDate;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jp.dataxeed.pm.dto.OrderProductRef;
import com.jp.dataxeed.pm.dto.OrderWithDetailsDto;
import com.jp.dataxeed.pm.dto.PictureDto;
import com.jp.dataxeed.pm.dto.ProductWithDetailsDto;
import com.jp.dataxeed.pm.dto.PartyCodeNameDto;
import com.jp.dataxeed.pm.entity.OrderEntity;
import com.jp.dataxeed.pm.form.order.SearchOrderForm;
import com.jp.dataxeed.pm.form.user.UserForm;
import com.jp.dataxeed.pm.helper.OrderHelper;
import com.jp.dataxeed.pm.helper.UserHelper;
import com.jp.dataxeed.pm.mapper.OrderMapper;
import com.jp.dataxeed.pm.mapper.OrderPicturesMapper;
import com.jp.dataxeed.pm.mapper.OrderProductsMapper;

@Service
public class OrderService {

    private final UserService userService;
    private final UserHelper userHelper;
    private final OrderMapper orderMapper;
    private final OrderHelper orderHelper;
    private final OrderProductsMapper orderProductsMapper;
    private final ProductService productService;
    private final OrderPicturesMapper orderPicturesMapper;
    private final StockService stockService;
    private final PartyService partyService;

    @Autowired
    public OrderService(
            UserService userService,
            UserHelper userHelper,
            OrderMapper orderMapper,
            OrderHelper orderHelper,
            OrderProductsMapper orderProductsMapper,
            ProductService productService,
            OrderPicturesMapper orderPicturesMapper,
            StockService stockService,
            PartyService partyService) {
        this.userService = userService;
        this.userHelper = userHelper;
        this.orderMapper = orderMapper;
        this.orderHelper = orderHelper;
        this.orderProductsMapper = orderProductsMapper;
        this.productService = productService;
        this.orderPicturesMapper = orderPicturesMapper;
        this.stockService = stockService;
        this.partyService = partyService;
    }

    // ===== ユーザー関連 =====
    public List<UserForm> getAdminUsers() {
        return userService.findAll().stream()
                .map(userHelper::EntityToForm)
                .filter(u -> "ADMIN".equals(u.getRole()))
                .toList();
    }

    public List<UserForm> getAllUsers() {
        return userService.findAll().stream()
                .map(userHelper::EntityToForm)
                .toList();
    }

    // ===== 取得・検索 =====
    public OrderWithDetailsDto getOrderWithDetailsById(int orderId) {
        return orderMapper.getOrderWithDetailsById(orderId);
    }

    public List<OrderWithDetailsDto> searchOrderWithDetailsDto(SearchOrderForm form) {
        if (form == null)
            return List.of();
        return orderMapper.searchOrderWithDetailsDto(form);
    }

    public List<ProductWithDetailsDto> getProductWithDetailsDtos(int orderId) {
        List<OrderProductRef> refs = orderProductsMapper.findRefsByOrderId(orderId);
        if (refs == null || refs.isEmpty())
            return List.of();

        Map<Integer, Integer> qtyByProductId = new LinkedHashMap<>();
        for (OrderProductRef r : refs) {
            if (r.getProductId() == null || r.getQuantity() == null)
                continue;
            qtyByProductId.merge(r.getProductId(), r.getQuantity(), Integer::sum);
        }

        List<ProductWithDetailsDto> dtos = productService
                .getProductsWithDetailsByIds(new ArrayList<>(qtyByProductId.keySet()));
        for (ProductWithDetailsDto d : dtos) {
            d.setQuantity(qtyByProductId.get(d.getId()));
        }
        return dtos;
    }

    // ===== 新規表示時：空注文作成 =====
    @Transactional
    public int createEmptyOrder(String mode) {
        List<PartyCodeNameDto> parties = partyService.getPartyCodeNameDtos();
        if (parties == null || parties.isEmpty()) {
            throw new IllegalStateException("party が1件も無いため仮注文を作成できません");
        }
        int partyId = parties.get(0).getId();

        List<UserForm> admins = getAdminUsers();
        if (admins.isEmpty())
            throw new IllegalStateException("ADMINユーザーが存在しません");
        int adminId = admins.get(0).getId();

        List<UserForm> allUsers = getAllUsers();
        if (allUsers.isEmpty())
            throw new IllegalStateException("ユーザーが存在しません");
        int workerId = allUsers.get(0).getId();
        int inspectorId = allUsers.get(0).getId();

        String prefix = "OUT".equalsIgnoreCase(mode) ? "OUT" : "IN";
        String draftCode = "DRAFT-" + prefix + "-" + System.currentTimeMillis();

        OrderEntity e = new OrderEntity();
        e.setOrderCode(draftCode);
        e.setPartyId(partyId);
        e.setDeliveryDate(LocalDate.now());
        e.setAdminId(adminId);
        e.setWarehouseWorkerId(workerId);
        e.setQualityInspectorId(inspectorId);
        e.setSituation("下書き");

        orderMapper.insertOrderToSave(e);
        if (e.getId() == null || e.getId() <= 0) {
            throw new IllegalStateException("仮注文の採番に失敗しました");
        }
        return e.getId();
    }

    // ===== 保存（既存編集） =====
    @Transactional
    public void saveOrderWithDetails(OrderWithDetailsDto dto) {
        final int orderId = dto.getId();

        String beforeSituation = null;
        String beforeOrderCode = null;
        List<OrderProductRef> beforeItems = List.of();
        if (orderId > 0) {
            OrderEntity before = orderMapper.findById(orderId);
            if (before != null) {
                beforeSituation = before.getSituation();
                beforeOrderCode = before.getOrderCode();
                beforeItems = safeRefs(orderProductsMapper.findRefsByOrderId(orderId));
            }
        }

        OrderEntity entity = orderHelper.dtoToEntity(dto);
        if (entity.getOrderCode() == null || entity.getOrderCode().isBlank()) {
            OrderEntity cur = orderMapper.findById(orderId);
            if (cur != null)
                entity.setOrderCode(cur.getOrderCode());
        }
        orderMapper.updateOrderToSave(entity);

        orderProductsMapper.deleteByOrderId(orderId);
        List<ProductWithDetailsDto> items = dto.getProductWithDetailsDtos();
        if (items != null && !items.isEmpty()) {
            Map<Integer, Integer> qtyByProductId = new LinkedHashMap<>();
            for (ProductWithDetailsDto p : items) {
                if (p == null || p.getId() == null || p.getQuantity() == null)
                    continue;
                if (p.getQuantity() <= 0)
                    continue;
                qtyByProductId.merge(p.getId(), p.getQuantity(), Integer::sum);
            }
            List<OrderProductRef> refs = qtyByProductId.entrySet().stream()
                    .map(e -> {
                        OrderProductRef r = new OrderProductRef();
                        r.setProductId(e.getKey());
                        r.setQuantity(e.getValue());
                        return r;
                    })
                    .toList();
            if (!refs.isEmpty())
                orderProductsMapper.insertBatch(orderId, refs);
        }

        if (dto.getPictures() != null) {
            orderPicturesMapper.deleteByOrderId(orderId);
            for (PictureDto pic : dto.getPictures()) {
                orderPicturesMapper.insertOrderPicture(orderId, pic.getId());
            }
        }

        String afterOrderCode = (dto.getOrderCode() != null && !dto.getOrderCode().isBlank())
                ? dto.getOrderCode()
                : orderMapper.findById(orderId).getOrderCode();

        String beforeMode = resolveModeFromCode(beforeOrderCode);
        String afterMode = resolveModeFromCode(afterOrderCode);

        stockService.handleCompletionTransition(
                beforeSituation,
                dto.getSituation(),
                (afterMode != null ? afterMode : beforeMode),
                beforeItems,
                safeRefs(orderProductsMapper.findRefsByOrderId(orderId)),
                afterOrderCode);
    }

    // ===== 新規保存（事前採番対応） =====
    @Transactional
    public void saveNewOrderWithDetails(OrderWithDetailsDto dto, String modeArg) {
        if (dto == null)
            return;
        final String prefix = "OUT".equalsIgnoreCase(modeArg) ? "OUT" : "IN";

        if (dto.getId() != null && dto.getId() > 0) {
            final int orderId = dto.getId();

            OrderEntity entity = orderHelper.dtoToEntity(dto);
            OrderEntity cur = orderMapper.findById(orderId);
            if (cur != null && (entity.getOrderCode() == null || entity.getOrderCode().isBlank())) {
                entity.setOrderCode(cur.getOrderCode());
            }
            orderMapper.updateOrderToSave(entity);

            final String orderCode = prefix + orderId;
            orderMapper.updateOrderCodeById(orderId, orderCode);
            dto.setOrderCode(orderCode);

            orderProductsMapper.deleteByOrderId(orderId);
            List<ProductWithDetailsDto> items = dto.getProductWithDetailsDtos();
            if (items != null && !items.isEmpty()) {
                Map<Integer, Integer> qtyByProductId = new LinkedHashMap<>();
                for (ProductWithDetailsDto p : items) {
                    if (p == null || p.getId() == null || p.getQuantity() == null)
                        continue;
                    if (p.getQuantity() <= 0)
                        continue;
                    qtyByProductId.merge(p.getId(), p.getQuantity(), Integer::sum);
                }
                List<OrderProductRef> refs = qtyByProductId.entrySet().stream()
                        .map(e -> {
                            OrderProductRef r = new OrderProductRef();
                            r.setProductId(e.getKey());
                            r.setQuantity(e.getValue());
                            return r;
                        })
                        .toList();
                if (!refs.isEmpty())
                    orderProductsMapper.insertBatch(orderId, refs);
            }

            if (dto.getPictures() != null) {
                orderPicturesMapper.deleteByOrderId(orderId);
                for (PictureDto pic : dto.getPictures()) {
                    orderPicturesMapper.insertOrderPicture(orderId, pic.getId());
                }
            }

            if ("完了".equals(dto.getSituation())) {
                stockService.handleCompletionTransition(
                        null, "完了", prefix,
                        List.of(),
                        safeRefs(orderProductsMapper.findRefsByOrderId(orderId)),
                        orderCode);
            }
            return;
        }

        // fallback: 完全新規
        OrderEntity entity = orderHelper.dtoToEntity(dto);
        entity.setId(null);
        String draftCode = "DRAFT-" + prefix + "-" + System.currentTimeMillis();
        entity.setOrderCode(draftCode);
        orderMapper.insertOrderToSave(entity);
        final int orderId = entity.getId();
        if (orderId <= 0)
            throw new IllegalStateException("新規注文のID採番に失敗しました");

        final String orderCode = prefix + orderId;
        orderMapper.updateOrderCodeById(orderId, orderCode);
        dto.setId(orderId);
        dto.setOrderCode(orderCode);

        orderProductsMapper.deleteByOrderId(orderId);
        List<ProductWithDetailsDto> items = dto.getProductWithDetailsDtos();
        if (items != null && !items.isEmpty()) {
            Map<Integer, Integer> qtyByProductId = new LinkedHashMap<>();
            for (ProductWithDetailsDto p : items) {
                if (p == null || p.getId() == null || p.getQuantity() == null)
                    continue;
                if (p.getQuantity() <= 0)
                    continue;
                qtyByProductId.merge(p.getId(), p.getQuantity(), Integer::sum);
            }
            List<OrderProductRef> refs = qtyByProductId.entrySet().stream()
                    .map(e -> {
                        OrderProductRef r = new OrderProductRef();
                        r.setProductId(e.getKey());
                        r.setQuantity(e.getValue());
                        return r;
                    })
                    .toList();
            if (!refs.isEmpty())
                orderProductsMapper.insertBatch(orderId, refs);
        }

        if (dto.getPictures() != null && !dto.getPictures().isEmpty()) {
            orderPicturesMapper.deleteByOrderId(orderId);
            for (PictureDto pic : dto.getPictures()) {
                orderPicturesMapper.insertOrderPicture(orderId, pic.getId());
            }
        }

        if ("完了".equals(dto.getSituation())) {
            stockService.handleCompletionTransition(
                    null, "完了",
                    resolveModeFromCode(orderCode),
                    List.of(),
                    safeRefs(orderProductsMapper.findRefsByOrderId(orderId)),
                    orderCode);
        }
    }

    // ===== ユーティリティ =====
    private List<OrderProductRef> safeRefs(List<OrderProductRef> refs) {
        return (refs != null) ? refs : List.of();
    }

    private static String resolveModeFromCode(String code) {
        if (code == null)
            return null;
        String c = code.trim().toUpperCase();
        if (c.startsWith("IN"))
            return "IN";
        if (c.startsWith("OUT"))
            return "OUT";
        if (c.startsWith("DRAFT-IN"))
            return "IN";
        if (c.startsWith("DRAFT-OUT"))
            return "OUT";
        return null;
    }

    // ===== 削除 =====
    public boolean deleteOrder(int orderId) {
        OrderEntity orderEntity = orderMapper.findById(orderId);
        if (orderEntity == null)
            return false;
        if ("完了".equals(orderEntity.getSituation()))
            return false;

        orderMapper.deleteOrderById(orderId);
        orderProductsMapper.deleteByOrderId(orderId);
        return true;
    }

    // ===== 離脱処理 =====
    @Transactional
    public void exitOrder(int orderId) {
        OrderEntity order = orderMapper.findById(orderId);
        if (order == null)
            return;

        // 既に本コードに変わっていたら何もしない
        String code = order.getOrderCode();
        if (code == null || !code.startsWith("DRAFT-"))
            return;

        // 保存確定と競合しないように“下書き&子明細ゼロ”の時だけ削除
        int itemCount = orderProductsMapper.countByOrderId(orderId);
        if (!"下書き".equals(order.getSituation()) || itemCount > 0)
            return;

        orderMapper.deleteOrderById(orderId);
    }
}
