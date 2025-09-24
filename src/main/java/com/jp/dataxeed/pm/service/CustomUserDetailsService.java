package com.jp.dataxeed.pm.service;

import com.jp.dataxeed.pm.entity.UserEntity;
import com.jp.dataxeed.pm.mapper.UserMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userMapper.findByUsername(username);

        if (userEntity == null) {
            throw new UsernameNotFoundException("ユーザーが見つかりません: " + username);
        }

        // パスワードとロールをSpring SecurityのUserにマッピング
        // 悲しいけど、UserDetailクラスをカスタムしないと、Id/RoleをSpringSecurityにつたえることはできないらしい
        // これが標準装備↓らしい
        return User.builder()
                .username(userEntity.getUsername())
                .password(userEntity.getPassword())
                .roles(userEntity.getRole()) // ROLE_ というプレフィックスが自動で追加される（SimpleAuthorityMapperが担当）
                .build();
    }
}
