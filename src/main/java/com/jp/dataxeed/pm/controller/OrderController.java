package com.jp.dataxeed.pm.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jp.dataxeed.pm.dto.OrderWithDetailsDto;
import com.jp.dataxeed.pm.dto.PartyCodeNameDto;
import com.jp.dataxeed.pm.form.order.SearchOrderForm;
import com.jp.dataxeed.pm.service.CategoryService;
import com.jp.dataxeed.pm.service.LocationService;
import com.jp.dataxeed.pm.service.OrderService;
import com.jp.dataxeed.pm.service.PartyService;
import com.jp.dataxeed.pm.service.UserService;

@SessionAttributes("searchOrderForm")
@RequestMapping("/order")
@Controller
public class OrderController {

    private final UserService userService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final PartyService partyService;

    @Autowired
    public OrderController(
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

    @ModelAttribute("searchOrderForm")
    public SearchOrderForm iniPartyForm() {
        return new SearchOrderForm();
    }

    @GetMapping("")
    public String showOrder(Model model, Principal principal) {
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));
        model.addAttribute("orderMakerList", orderService.getAdminUsers());
        model.addAttribute("qualityInspectorList", orderService.getAllUsers());
        model.addAttribute("warehouseWorkerList", orderService.getAllUsers());
        model.addAttribute("orders", List.of());
        return "order/order";
    }

    @PostMapping("/reset")
    public String reset(SessionStatus sessionStatus) {
        sessionStatus.setComplete();
        return "redirect:/order";
    }

    @PostMapping("/search")
    public String searchOrderPost(@ModelAttribute("searchOrderForm") SearchOrderForm searchOrderForm, Model model) {
        model.addAttribute("searchOrderForm", searchOrderForm);
        return "redirect:/order/search";
    }

    @GetMapping("/search")
    public String searchOrderGet(@ModelAttribute("searchOrderForm") SearchOrderForm searchOrderForm, Model model,
            Principal principal) {
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));
        List<OrderWithDetailsDto> orderWithDetailsDtos = orderService.searchOrderWithDetailsDto(searchOrderForm);
        model.addAttribute("orders", orderWithDetailsDtos);
        model.addAttribute("orderMakerList", orderService.getAdminUsers());
        model.addAttribute("qualityInspectorList", orderService.getAllUsers());
        model.addAttribute("warehouseWorkerList", orderService.getAllUsers());
        return "order/order";
    }

    @GetMapping("/edit")
    public String editOrder(@RequestParam("orderId") int orderId, Model model, Principal principal) {
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));

        OrderWithDetailsDto dto = orderService.getOrderWithDetailsById(orderId);
        System.out.println("DEBUG deliveryDate = " + dto.getDeliveryDate());
        dto.setProductWithDetailsDtos(orderService.getProductWithDetailsDtos(orderId));

        model.addAttribute("orderWithDetailsDto", dto);

        List<PartyCodeNameDto> partyCodeNameDtos = partyService.getPartyCodeNameDtos();
        model.addAttribute("partyList", partyCodeNameDtos);

        model.addAttribute("adminList", orderService.getAdminUsers());
        model.addAttribute("qualityInspectorList", orderService.getAllUsers());
        model.addAttribute("warehouseWorkerList", orderService.getAllUsers());

        // 検索フォーム用
        model.addAttribute("categoryList", categoryService.findAll());
        model.addAttribute("locationList", locationService.findAll());

        return "order/edit";
    }

    @PostMapping("/save")
    public String saveOrder(@ModelAttribute("orderWithDetailsDto") OrderWithDetailsDto orderWithDetailsDto,
            BindingResult bindingResult) {
        orderService.saveOrderWithDetails(orderWithDetailsDto);

        return "redirect:/order/search";
    }

    // 削除
    @PostMapping("/delete")
    public String deleteOrder(@RequestParam("orderId") int orderId, RedirectAttributes redirectAttributes) {
        if (!orderService.deleteOrder(orderId)) {
            // 削除失敗
            redirectAttributes.addFlashAttribute("deleteFailed", true);
            return "redirect:/order/search";
        }
        return "redirect:/order/search";
    }

    // --- 画面離脱通知 ---
    @PostMapping("/exit")
    @ResponseBody
    public ResponseEntity<String> handleExit(@RequestBody Map<String, Object> body) {
        System.out.println("◆ handleExit 呼ばれた");
        System.out.println("◆ 受信Body = " + body);

        try {
            Integer orderId = Integer.parseInt(body.get("orderId").toString());
            System.out.println("◆ パース済み orderId = " + orderId);

            // DB更新 or ログ保存など
            orderService.exitOrder(orderId);

            return ResponseEntity.ok("exit ok");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("exit error");
        }
    }

}
