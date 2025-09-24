package com.jp.dataxeed.pm.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.autoconfigure.jms.JmsProperties.Listener.Session; // ← 未使用のため削除
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import com.jp.dataxeed.pm.dto.ProductWithDetailsDto;
import com.jp.dataxeed.pm.form.product.SearchProductForm;
// ▼ パッケージ名のtypo修正（produt → product）
import com.jp.dataxeed.pm.service.CategoryService;
import com.jp.dataxeed.pm.service.LocationService;
import com.jp.dataxeed.pm.service.ProductService;
import com.jp.dataxeed.pm.service.UserService;

@SessionAttributes("searchProductForm")
@RequestMapping("/product")
@Controller
public class ProductController {

    private final UserService userService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final LocationService locationService;

    @Autowired
    public ProductController(UserService userService,
            ProductService productService,
            CategoryService categoryService,
            LocationService locationService) {
        this.userService = userService;
        this.productService = productService;
        this.categoryService = categoryService;
        this.locationService = locationService;
    }

    // HTTPセッションに一時保持する
    @ModelAttribute("searchProductForm")
    public SearchProductForm iniProductForm() {
        return new SearchProductForm();
    }

    // 管理画面
    @GetMapping({ "", "/" })
    public String showProduct(Model model, Principal principal) {
        // 権限取得・送信
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));
        // 検索に必要なデータはセッションから自動修復
        // カテゴリーALL取得・追加
        model.addAttribute("categoryList", categoryService.findAll());
        // ロケーションALL取得・追加
        model.addAttribute("locationList", locationService.findAll());
        // 空の検索結果を送る
        model.addAttribute("products", List.of());
        return "product/product";
    }

    // リセットボタン
    @PostMapping("/reset")
    public String reset(SessionStatus sessionStatus) {
        sessionStatus.setComplete();
        return "redirect:/product";
    }

    // 検索処理Ⅰ POST
    @PostMapping("/search")
    public String searchProductPost(@ModelAttribute("searchProductForm") SearchProductForm searchProductForm,
            Model model) {
        model.addAttribute("searchProductForm", searchProductForm);
        return "redirect:/product/search";
    }

    // 検索処理Ⅱ GET
    @GetMapping("/search")
    public String searchProductGet(@ModelAttribute("searchProductForm") SearchProductForm searchProductForm,
            Model model, Principal principal) {
        // 権限取得・送信
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));
        // 検索実行
        List<ProductWithDetailsDto> productWithDetailsDtos = productService
                .searchProductWithDetailsDto(searchProductForm);
        // 検索結果 送信（▼ ビューが参照するキー名に合わせて修正）
        model.addAttribute("products", productWithDetailsDtos);
        // カテゴリーALL取得・追加
        model.addAttribute("categoryList", categoryService.findAll());
        // ロケーションALL取得・追加
        model.addAttribute("locationList", locationService.findAll());
        return "product/product";
    }

    ///////////////////////////////////////////////////

    // 新規作成画面
    @GetMapping("/new")
    public String showNew(Model model) {
        // モード選択
        model.addAttribute("formMode", "new");
        // 新しいproductWithDetaildDtoを追加
        model.addAttribute("productWithDetailsDto", new ProductWithDetailsDto());
        // カテゴリーALL取得
        model.addAttribute("categoryList", categoryService.findAll());
        // ロケーションALL取得
        model.addAttribute("locationList", locationService.findAll());

        return "product/form";
    }

    // 編集画面
    @GetMapping("/edit")
    public String showEdit(@RequestParam("productId") int id, Model model) {
        // モード選択
        model.addAttribute("formMode", "edit");
        // DBから該当DTO取得・追加
        ProductWithDetailsDto dto = productService.getProductWithDetailsDtoById(id);
        model.addAttribute("productWithDetailsDto", dto);
        // カテゴリーALL取得
        model.addAttribute("categoryList", categoryService.findAll());
        // ロケーションALL取得
        model.addAttribute("locationList", locationService.findAll());

        return "product/form";
    }

    ////////////////////////////////////////////////////////

    // 保存(編集/新規作成 → 保存)
    @PostMapping("/save")
    public String saveProductWithDetails(
            @ModelAttribute("productWithDetailsDto") ProductWithDetailsDto productWithDetailsDto) {
        // ↓勝手に新規登録か更新か判断する ＋ 各種テーブルに対しての保存実行
        productService.saveProduct(productWithDetailsDto);
        return "redirect:/product/search";
    }

    // 削除
    @PostMapping("/delete")
    public String deleteProductWithDetails(@RequestParam("productId") int id) { // ▼ フォームのhidden名に合わせて修正
        // Product本体 関連するカテゴリ/管理場所の中間テーブルの理論削除
        productService.deleteProduct(id);
        return "redirect:/product/search";
    }
}
