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

import com.jp.dataxeed.pm.form.party.PartyForm;
import com.jp.dataxeed.pm.form.party.SearchPartyForm;
import com.jp.dataxeed.pm.service.PartyService;
import com.jp.dataxeed.pm.service.UserService;

@SessionAttributes("searchPartyForm")
@RequestMapping("party")
@Controller
public class PartyController {

    private final UserService userService;
    private final PartyService partyService;

    @Autowired
    public PartyController(UserService userService, PartyService partyService) {
        this.userService = userService;
        this.partyService = partyService;
    }

    // HTTPセッションに一時保持する
    @ModelAttribute("searchPartyForm")
    public SearchPartyForm iniPartyForm() {
        return new SearchPartyForm();
    }

    // 管理画面
    @GetMapping("")
    public String showParty(Model model, Principal principal) {
        // 権限取得・送信
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));

        // 検索に必要なデータはセッションから自動修復される
        // 空の検索結果を送る
        model.addAttribute("parties", List.of());
        return "party/party";
    }

    // リセットボタン
    @PostMapping("/reset")
    public String reset(SessionStatus sessionStatus) {
        sessionStatus.setComplete();
        return "redirect:/party";
    }

    // 検索処理Ⅰ POST 検索条件受信用
    @PostMapping("/search")
    public String searchPartyPost(@ModelAttribute("searchPartyForm") SearchPartyForm searchPartyForm, Model model) {
        model.addAttribute("searchPartyForm", searchPartyForm);
        return "redirect:/party/search";
    }

    // 検索処理Ⅱ GET 検索処理
    @GetMapping("/search")
    public String searchPartyGet(@ModelAttribute("searchPartyForm") SearchPartyForm searchPartyForm, Model model,
            Principal principal) {
        // 権限取得・送信
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));
        // 検索実行
        List<PartyForm> parties = partyService.searchParty(searchPartyForm);

        // 検索結果 送信
        model.addAttribute("parties", parties);
        return "party/party";
    }

    /////////////////////////////////////////

    // 新規作成画面
    @GetMapping("/new")
    public String showNew(Model model) {
        // モード選択
        model.addAttribute("formMode", "new");
        // 新しいPartyForm
        model.addAttribute("partyForm", new PartyForm());
        return "/party/form";
    }

    // 編集画面
    @GetMapping("/edit")
    public String showEdit(@RequestParam("partyId") int partyId, Model model) {
        // モード選択
        model.addAttribute("formMode", "edit");
        // 該当のPartyForm
        model.addAttribute("partyForm", partyService.findById(partyId));
        return "party/form";
    }

    ////////////////////////////////////////////////

    // 保存(編集/新規作成 → 保存)
    @PostMapping("/save")
    public String saveParty(@ModelAttribute("partyForm") PartyForm partyForm) {
        // ↓勝手に新規登録か更新か判断する
        partyService.saveParty(partyForm);
        return "redirect:/party/search";
    }

    // 削除
    @PostMapping("/delete")
    public String deletePatry(@RequestParam("partyId") int partyId) {
        partyService.deleteParty(partyId);
        return "redirect:/party/search";
    }

}
