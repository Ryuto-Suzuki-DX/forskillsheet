package com.jp.dataxeed.pm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;

import com.jp.dataxeed.pm.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ここでそれぞれのパスをロールに応じてアクセス制御する
    // 例えば、/admin/**はADMINロールのみアクセス可能、/user/**はADMINとUSERロールがアクセス可能など
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/general/**").hasRole("GENERAL")
                        .requestMatchers("/user/**", "/product/**", "/party/**", "/location/**", "/order/**")
                        .hasAnyRole("ADMIN", "GENERAL") // GET
                        .requestMatchers(HttpMethod.POST, "/user/**", "/product/**", "/party/**", "/location/**",
                                "/order/**") // POST
                        .hasAnyRole("ADMIN", "GENERAL")
                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/order/exit"));

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setAuthoritiesMapper(authoritiesMapper());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public GrantedAuthoritiesMapper authoritiesMapper() {
        SimpleAuthorityMapper authorityMapper = new SimpleAuthorityMapper();
        authorityMapper.setPrefix("ROLE_"); // データベースのロール名に"ROLE_"プレフィックスを追加
        authorityMapper.setConvertToUpperCase(true); // ← これを追加
        return authorityMapper;
    }
}