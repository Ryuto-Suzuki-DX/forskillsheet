package com.jp.dataxeed.pm.mapper;

import java.util.List;

import org.springframework.stereotype.Service;

import com.jp.dataxeed.pm.entity.UserEntity;
import com.jp.dataxeed.pm.form.user.SearchUserForm;

@Service
public interface UserMapper {

    // 全取得（理論削除除く）
    List<UserEntity> findAll();

    // ユーザー名で検索 →userEntity
    UserEntity findByUsername(String username);

    // int → UserEntity
    UserEntity findById(int id);

    // searchUserForm → List<userEntity>
    List<UserEntity> searchUsers(SearchUserForm searchUserForm);

    // update(.xmlでパスワード有無による判定制御 serviceでハッシュ化対応制御)
    void update(UserEntity userEntity);

    // insert(.xmlでパスワード有無による判定制御 serviceでハッシュ化対応制御)
    void insert(UserEntity userEntity);

    // delete (updateによる理論削除)
    void deleteLogical(int id);

    boolean existsByUsername(String username);
}
