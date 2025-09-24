package com.jp.dataxeed.pm.controller;

import java.security.Principal;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.jp.dataxeed.pm.dto.OrderWithDetailsDto;
import com.jp.dataxeed.pm.service.CategoryService;
import com.jp.dataxeed.pm.service.LocationService;
import com.jp.dataxeed.pm.service.OrderService;
import com.jp.dataxeed.pm.service.PartyService;
import com.jp.dataxeed.pm.service.UserService;

/**
 * 新規入出庫（作成専用）
 * /order-in → mode=IN
 * /order-out → mode=OUT
 *
 * 画面表示時に“空注文”を1件作ってIDを採番→ hidden name="id" にセット。
 * これで新規でも /order/api/{orderId}/pictures が即使える。
 */
@Controller
public class InOutController {

    private final UserService userService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final PartyService partyService;

    @Autowired
    public InOutController(
            UserService userService,
            OrderService orderService,
            CategoryService categoryService,
            LocationService locationService,
            PartyService partyService) {
        this.userService = userService;
        this.orderService = orderService;
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.partyService = partyService;
    }

    // ---------------------------
    // 画面表示（IN / OUT）
    // ---------------------------

    @GetMapping("/order-in")
    public String newIn(Model model, Principal principal) {
        return setupNewForm(model, principal, "IN");
    }

    @GetMapping("/order-out")
    public String newOut(Model model, Principal principal) {
        return setupNewForm(model, principal, "OUT");
    }

    private String setupNewForm(Model model, Principal principal, String mode) {
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));

        // 事前に選択肢リストを詰める（例外時もメッセージ表示に使える）
        prepareLists(model, principal);

        // 空の注文レコードを1件作成して採番ID取得
        int newId = orderService.createEmptyOrder(mode);

        // DTOへ反映（hidden の id に入る）
        OrderWithDetailsDto dto = new OrderWithDetailsDto();
        dto.setId(newId);
        dto.setSituation(""); // 初期値（必要に応じて調整）
        model.addAttribute("orderWithDetailsDto", dto);

        model.addAttribute("mode", mode); // "IN" or "OUT"
        return "order/new-inout";
    }

    // ---------------------------
    // 保存（IN / OUT）
    // ---------------------------

    @PostMapping("/order-in/save")
    public String saveIn(@ModelAttribute("orderWithDetailsDto") OrderWithDetailsDto dto,
            BindingResult br, Model model, Principal principal) {
        populatePartyIdIfMissing(dto);
        if (dto.getPartyId() == null) {
            br.addError(new ObjectError("orderWithDetailsDto", "企業を選択してください（コード/名称のいずれかから一致が必要）"));
        }
        if (br.hasErrors()) {
            prepareLists(model, principal);
            model.addAttribute("mode", "IN");
            return "order/new-inout";
        }

        // 事前採番済みのIDを持つ下書きを確定保存
        orderService.saveNewOrderWithDetails(dto, "IN");
        return "redirect:/order/search";
    }

    @PostMapping("/order-out/save")
    public String saveOut(@ModelAttribute("orderWithDetailsDto") OrderWithDetailsDto dto,
            BindingResult br, Model model, Principal principal) {
        populatePartyIdIfMissing(dto);
        if (dto.getPartyId() == null) {
            br.addError(new ObjectError("orderWithDetailsDto", "企業を選択してください（コード/名称のいずれかから一致が必要）"));
        }
        if (br.hasErrors()) {
            prepareLists(model, principal);
            model.addAttribute("mode", "OUT");
            return "order/new-inout";
        }

        // 事前採番済みのIDを持つ下書きを確定保存
        orderService.saveNewOrderWithDetails(dto, "OUT");
        return "redirect:/order/search";
    }

    // ---------------------------
    // 内部ユーティリティ
    // ---------------------------

    /** partyId 未指定なら code/name から補完（見つからなければ null のまま） */
    private void populatePartyIdIfMissing(OrderWithDetailsDto dto) {
        if (dto == null || dto.getPartyId() != null)
            return;

        Integer id = null;
        if (dto.getPartyCode() != null && !dto.getPartyCode().isBlank()) {
            id = partyService.findIdByCode(dto.getPartyCode().trim());
        }
        if (id == null && dto.getPartyName() != null && !dto.getPartyName().isBlank()) {
            id = partyService.findIdByName(dto.getPartyName().trim());
        }
        dto.setPartyId(id);
    }

    /** 画面再表示に必要な各種リストを詰める */
    private void prepareLists(Model model, Principal principal) {
        model.addAttribute("partyList", partyService.getPartyCodeNameDtos());
        model.addAttribute("adminList", orderService.getAdminUsers());
        model.addAttribute("qualityInspectorList", orderService.getAllUsers());
        model.addAttribute("warehouseWorkerList", orderService.getAllUsers());
        model.addAttribute("categoryList", categoryService.findAll());
        model.addAttribute("locationList", locationService.findAll());
        if (principal != null) {
            model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));
        }
    }

    @SuppressWarnings("unused")
    private static String normalizeMode(String mode) {
        String m = Objects.toString(mode, "").trim().toUpperCase();
        if ("IN".equals(m) || "OUT".equals(m))
            return m;
        throw new IllegalArgumentException("mode must be IN or OUT");
    }
}
