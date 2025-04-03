package com.ecommerce.cartservice.security;

import lombok.Data;

@Data
public class UserPrincipal {
    private Long id;
    private String username;
    private String authId;
}
