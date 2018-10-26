package com.xingfly.account.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Created by SuperS on 2017/9/25.
 *
 * @author SuperS
 */
@RestController
public class AccountController {

    @GetMapping("/current")
    public Principal user(Principal principal) {
        return principal;
    }

    @GetMapping("/query")
    @PreAuthorize("hasAnyAuthority('query')")
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    public String query() {
        return "具有query权限";
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('add')")
    public String add() {
        return "写add权限";
    }

}
