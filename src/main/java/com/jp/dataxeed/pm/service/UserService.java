package com.jp.dataxeed.pm.service;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jp.dataxeed.pm.entity.UserEntity;
import com.jp.dataxeed.pm.form.user.SearchUserForm;
import com.jp.dataxeed.pm.form.user.UserForm;
import com.jp.dataxeed.pm.helper.UserHelper;
import com.jp.dataxeed.pm.mapper.UserMapper;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final UserHelper userHelper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserMapper userMapper, UserHelper userHelper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userHelper = userHelper;
        this.passwordEncoder = passwordEncoder;
    }

    // 全取得（理論削除除く）
    public List<UserEntity> findAll() {
        return userMapper.findAll();
    }

    // ユーザー名で検索 →userEntity

    // searchUserForm → List<userEntity>
    public List<UserForm> searchUsers(SearchUserForm searchUserForm) {
        if (searchUserForm == null) {
            return List.of();
        }
        return userMapper.searchUsers(searchUserForm).stream()
                .map(userHelper::EntityToForm)
                .collect(Collectors.toList());

    }

    // JSで削除制限のために、ログインしているユーザのIDを抜き取る
    public int getUserEntityId(Principal principal) {
        String username = principal.getName();
        UserEntity userEntity = userMapper.findByUsername(username);
        if (userEntity == null)
            throw new IllegalStateException("ログインユーザーが存在しません。" + username);
        return userEntity.getId();
    }

    // JSで削除制限のために、ログインしているユーザのROLEを抜き取る
    public String getUesrEntityRole(Principal principal) {
        String username = principal.getName();
        UserEntity userEntity = userMapper.findByUsername(username);
        if (userEntity == null)
            throw new IllegalStateException("ログインユーザーが存在しません。" + username);
        return userEntity.getRole();
    }

    // finfById int → UserEntity
    public UserEntity findById(int id) {
        return userMapper.findById(id);
    }

    // 新規登録時 + 更新の処理 → id の有無判定
    public void saveUserForm(UserForm userForm) {
        // Form → Entity → hash化（null以外） → 各種処理 (passwordがnull notnullの場合での制御は.xmlで実施済み)
        UserEntity userEntity = userHelper.formToEntity(userForm);

        if (userForm.getId() == null) {
            // 新規登録：パスワード必須
            String password = userEntity.getPassword();
            if (password == null || password.isBlank() || password.contains(" ")) {
                throw new IllegalArgumentException("パスワードが不正です（空または空白を含む）");
            }
            userEntity.setPassword(passwordEncoder.encode(password));
            userMapper.insert(userEntity);
        } else {
            // 更新：パスワードが空なら更新しない
            String password = userEntity.getPassword();
            if (password != null && !password.isBlank() && !password.contains(" ")) {
                userEntity.setPassword(passwordEncoder.encode(password));
            } else {
                userEntity.setPassword(null); // パスワード変更しない合図
            }
            userMapper.update(userEntity);
        }
    }

    // 理論削除
    public void userDelete(int id) {
        userMapper.deleteLogical(id);
    }

    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }

}
