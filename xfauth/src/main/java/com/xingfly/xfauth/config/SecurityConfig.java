package com.xingfly.xfauth.config;

import com.xingfly.xfauth.service.security.JPAUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Created by SuperS on 2017/9/25.
 *
 * @author SuperS
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  @Autowired
  private JPAUserDetailsService jpaUserDetailsService;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // ingore是完全绕过了spring security的所有filter，相当于不走spring security
    // permitAll没有绕过spring security，其中包含了登录的以及匿名的。
    http.authorizeRequests()
        .antMatchers("/test").permitAll()
        .anyRequest().authenticated()
        .and()
        .csrf().disable();
  }

  /**
   * 密码加密的实现.
   *
   * @param auth .
   * @throws Exception .
   */
  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(jpaUserDetailsService).passwordEncoder(passwordEncoder());
  }

  /**
   * 不定义没有password grant_type
   *
   * @return .
   * @throws Exception .
   */
  @Override
  @Bean
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }
}
