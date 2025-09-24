package com.jp.dataxeed.pm.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

//全コントローラーのメソッドでかならず実行される
//currentPath fragmentの変数にリクエストパスをセットする
@ControllerAdvice
public class GlobalModelAttribute {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
