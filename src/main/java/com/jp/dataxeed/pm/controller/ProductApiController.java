package com.jp.dataxeed.pm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.jp.dataxeed.pm.dto.ProductWithDetailsDto;
import com.jp.dataxeed.pm.form.product.SearchProductForm;
import com.jp.dataxeed.pm.service.ProductService;

@RestController
@RequestMapping("/product/api")
public class ProductApiController {

    private final ProductService productService;

    @Autowired
    public ProductApiController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 製品名部分一致＋カテゴリ＋管理場所による検索
     */
    @GetMapping("/search")
    public List<ProductWithDetailsDto> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) List<Integer> categoryIds,
            @RequestParam(required = false) List<Integer> locationIds) {

        SearchProductForm form = new SearchProductForm();
        form.setName(name); // 部分一致検索はService/Mapper側で対応
        form.setCategoryIds(categoryIds);
        form.setLocationIds(locationIds);

        return productService.searchProductWithDetailsDto(form);
    }
}
