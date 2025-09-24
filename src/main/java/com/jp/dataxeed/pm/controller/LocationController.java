package com.jp.dataxeed.pm.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import com.jp.dataxeed.pm.form.location.LocationForm;
import com.jp.dataxeed.pm.form.location.SearchLocationForm;
import com.jp.dataxeed.pm.service.LocationService;
import com.jp.dataxeed.pm.service.UserService;

@SessionAttributes("searchLocationForm")
@RequestMapping("location")
@Controller
public class LocationController {

    private final UserService userService;
    private final LocationService locationService;

    @Autowired
    public LocationController(UserService userService, LocationService locationService) {
        this.userService = userService;
        this.locationService = locationService;
    }

    // HTTPセッションに一時保持する
    @ModelAttribute("searchLocationForm")
    public SearchLocationForm iniLocationForm() {
        return new SearchLocationForm();
    }

    //// -------------------------------------------------------------------------------

    // 管理画面
    @GetMapping("")
    public String showLocation(Model model, Principal principal) {
        // 権限取得・送信
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));
        // 検索に必要なデータはセッションから自動修復
        // 空の検索結果を送信
        model.addAttribute("locations", List.of());
        return "location/location";
    }

    // リセットボタン制御
    @PostMapping("/reset")
    public String reset(SessionStatus sessionStatus) {
        sessionStatus.setComplete();
        return "redirect:/location";
    }

    // 検索処理Ⅰ
    @PostMapping("/search")
    public String serchLocationPost(@ModelAttribute("searchLocationForm") SearchLocationForm searchLocationForm) {
        return "redirect:/location/search";
    }

    // 検索処理Ⅱ
    @GetMapping("/search")
    public String searchLocationGet(@ModelAttribute("searchLocationForm") SearchLocationForm searchLocationForm,
            Model model, Principal principal) {
        // 権限取得・送信
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));
        // 検索実行
        List<LocationForm> locations = locationService.searchLocation(searchLocationForm);
        // 検索結果送信
        model.addAttribute("locations", locations);
        return "/location/location";
    }

    ///////////////////////////////////////////////////

    // 新規作成画面
    @GetMapping("/new")
    public String shoNew(Model model) {
        // モード選択
        model.addAttribute("formMode", "new");
        // 新しいlcationForm
        model.addAttribute("locationForm", new LocationForm());
        return "/location/form";
    }

    // 編集画面
    @GetMapping("/edit")
    public String showEdit(@RequestParam("locationId") int locationId, Model model) {
        // モード選択
        model.addAttribute("formMode", "edit");
        // 該当のlocationForm
        model.addAttribute("locationForm", locationService.findById(locationId));
        return "location/form";
    }

    ////////////////////////////////////////////////////

    // 保存(編集/新規作成 → 保存)
    @PostMapping("/save")
    public String saveLocation(@ModelAttribute("locationForm") LocationForm locationForm) {
        // ↓勝手に新規登録か更新か判断する
        locationService.saveLocation(locationForm);
        return "redirect:/location/search";
    }

    // 削除
    @PostMapping("/delete")
    public String deleteLocation(@RequestParam("locationId") int locationId) {
        locationService.deleteLocation(locationId);
        return "redirect:/location/search";
    }
}
