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

import com.jp.dataxeed.pm.form.category.CategoryForm;
import com.jp.dataxeed.pm.form.category.SearchCategoryForm;
import com.jp.dataxeed.pm.service.CategoryService;
import com.jp.dataxeed.pm.service.UserService;

@SessionAttributes("searchCategoryForm")
@RequestMapping("/category")
@Controller
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

    @Autowired
    public CategoryController(CategoryService categoryService, UserService userService) {
        this.categoryService = categoryService;
        this.userService = userService;
    }

    // HTTPセッションに一時保持する
    @ModelAttribute("searchCategoryForm")
    public SearchCategoryForm iniCategoryForm() {
        return new SearchCategoryForm();
    }

    // -----------------------------------------------------------

    // 管理画面
    @GetMapping("")
    public String showCategory(Model model, Principal principal) {
        // 権限取得・送信
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));
        // 検索に必要なデータはセッションから自動修復
        // 空の検索結果を送信
        model.addAttribute("categories", List.of());
        return "category/category";
    }

    // リセットボタン制御
    @PostMapping("/reset")
    public String reset(SessionStatus sessionStatus) {
        sessionStatus.setComplete();
        return "redirect:/category";
    }

    // 検索処理Ⅰ POST用 → 検索条件受送信専用
    @PostMapping("/search")
    public String searchCategoryPost(@ModelAttribute("searchCategoryForm") SearchCategoryForm searchCategoryForm) {
        return "redirect:/category/search"; // → GETへGO
    }

    // 検索処理Ⅱ GET 検索処理 → 検索処理
    @GetMapping("/search")
    public String searchCategoryGet(@ModelAttribute("searchCategoryForm") SearchCategoryForm searchCategoryForm,
            Model model, Principal principal) {
        // 権限取得・送信
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));
        // 検索実行
        List<CategoryForm> categories = categoryService.searchCategory(searchCategoryForm);
        // 検索結果送信
        model.addAttribute("categories", categories);
        return "category/category";
    }

    //////////////////////////////////////////

    // 新規作成画面
    @GetMapping("/new")
    public String showNew(Model model) {
        // モード選択
        model.addAttribute("formMode", "edit");
        // 新しいUserFormをモデルへ追加
        model.addAttribute("categoryForm", new CategoryForm());
        return "category/form";
    }

    // 編集画面
    @GetMapping("/edit")
    public String categoryCreate(@RequestParam("categoryId") int categoryId, Model model) {
        // モード選択
        model.addAttribute("formMode", "edit");
        // 該当のCategoryFormをモデルへ追加
        model.addAttribute("categoryForm", categoryService.findById(categoryId));
        return "category/form";

    }

    //////////////////////////////////////////////

    // 保存(編集/新規作成 → 保存)
    @PostMapping("/save")
    public String saveCategory(@ModelAttribute("categoryForm") CategoryForm categoryForm) {
        // ↓勝手に新規登録か更新か判断してくれる
        categoryService.saveCategory(categoryForm);
        return "redirect:/category/search";
    }

    // 削除
    @PostMapping("/delete")
    public String deleteCategory(@RequestParam("categoryId") int categoryId) {
        categoryService.deleteCategory(categoryId);
        return "redirect:/category/search";
    }

}
