package com.guru.im.gateway.tcp.service;

public interface AuthorizeService {
    boolean isAuth(long uid, String token);

    Long authenticate(String token);
}
