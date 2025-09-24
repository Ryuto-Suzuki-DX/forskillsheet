package com.jp.dataxeed.pm.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import com.jp.dataxeed.pm.form.user.SearchUserForm;
import com.jp.dataxeed.pm.form.user.UserForm;
import com.jp.dataxeed.pm.helper.UserHelper;
import com.jp.dataxeed.pm.service.UserService;

@SessionAttributes("searchUserForm")
@RequestMapping("/user")
@Controller
public class UserController {

    private final UserService userService;
    private final UserHelper userHelper;

    @Autowired
    public UserController(UserService userService, UserHelper userHelper) {
        this.userService = userService;
        this.userHelper = userHelper;
    }

    // HTTPセッションに一時保持する
    @ModelAttribute("searchUserForm")
    public SearchUserForm initUserForm() {
        return new SearchUserForm();
    }

    // 管理画面
    @GetMapping("")
    public String showUser(Model model, Principal principal) {
        // JSで削除制限のために、ログインしているユーザのID ROLEを抜き取る
        model.addAttribute("loginUserId", userService.getUserEntityId(principal));
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));

        // ユーザー検索に必要なデータをモデルはセッションから自動復元
        // 空の検索結果を渡す
        model.addAttribute("users", List.of());
        return "user/user";
    }

    // リセットボタン
    @PostMapping("/reset")
    public String reset(SessionStatus sessionStatus) {
        sessionStatus.setComplete();
        return "redirect:/user";
    }

    // 検索結果表示 → POST用
    @PostMapping("/search")
    public String searchUserPost(@ModelAttribute("searchUserForm") SearchUserForm searchUserForm) {
        return "redirect:/user/search";
    }

    // 検索結果表示 → GET用
    @GetMapping("/search")
    public String searchUserGet(@ModelAttribute("searchUserForm") SearchUserForm searchUserForm, Model model,
            Principal principal) {
        // JSで削除制限のために、ログインしているユーザのID ROLEを抜き取る
        model.addAttribute("loginUserId", userService.getUserEntityId(principal));
        model.addAttribute("loginUserRole", userService.getUesrEntityRole(principal));

        // 検索結果分を取得、モデルへ渡す (空の場合の対処はhtmlへ委任)
        List<UserForm> users = userService.searchUsers(searchUserForm);
        model.addAttribute("users", users);

        return "user/user";
    }

    ///////////////////////////////////////////////

    // 新規登録画面表示
    @GetMapping("/new")
    public String userCreate(Model model) {
        // モード選択
        model.addAttribute("formMode", "new");
        // 新しいUserFormをモデルへ追加
        model.addAttribute("userForm", new UserForm());
        return "user/form";
    }

    // 編集画面表示
    @GetMapping("/edit")
    public String userEdit(@RequestParam("userId") int userId, Model model) {
        // モード選択
        model.addAttribute("formMode", "edit");
        // 該当のUserFormをモデルへ追加
        model.addAttribute("userForm", userHelper.EntityToForm(userService.findById(userId)));
        return "user/form";
    }

    //////////////////////////////////////////////
    // ※注意！ redirect: はGET送信になってしまう

    // 新規登録 ＋ 更新 の処理
    @PostMapping("/save")
    public String userSave(
            @ModelAttribute("userForm") UserForm userForm) {
        // userIdのセット！
        userService.saveUserForm(userForm);
        return "redirect:/user/search";
    }

    // 削除処理
    @PostMapping("/delete")
    public String userDelete(@RequestParam("userId") int userId) {
        userService.userDelete(userId);
        return "redirect:/user/search";
    }

    //////////////////////////////////////////////
    @GetMapping("/api/exists")
    @ResponseBody
    public Map<String, Object> existsUsername(@RequestParam("username") String username) {
        boolean exists = userService.existsByUsername(username); // ← サービスに追加（下記）
        return Map.of("exists", exists);
    }
}
