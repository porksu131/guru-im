package com.guru.im.auth.service;

import com.guru.im.auth.model.CustomUserDetails;
import com.guru.im.auth.model.pojo.UserInfo;
import com.guru.im.security.starter.JwtProperties;
import com.guru.im.security.starter.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    public AuthService(AuthenticationManager authenticationManager,
                       CustomUserDetailsService customUserDetailsService,
                       JwtUtil jwtUtil,
                       TokenService tokenService,
                       JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 登录，生成accessToken和refreshToken
     *
     * @param userName 用户名
     * @param password 密码
     * @return accessToken和refreshToken
     */
    public Map<String, Object> login(String userName, String password) {
        // 使用spring security的用户密码校验功能，校验成功后，会得到用户信息
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userName, password)
        );

        // 从数据库中查询到的信息会赋值给 principal
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userName", userDetails.getUsername());
        claims.put("uid", userDetails.getUserId());
        claims.put("authorities", getAuthorityNames(userDetails.getAuthorities()));

        String accessToken = jwtUtil.generateAccessToken(claims);
        String refreshToken = jwtUtil.generateRefreshToken(claims);
        long expiresIn = jwtProperties.getAccessTokenExpiration();

        tokenService.storeRefreshToken(userDetails.getUserId(), refreshToken);

        return Map.of("accessToken", accessToken,
                "refreshToken", refreshToken,
                "expiresIn", expiresIn,
                "userName", userName,
                "uid", userDetails.getUserId());
    }

    /**
     * 刷新，通过refreshToken来生成新的accessToken
     *
     * @param refreshToken refreshToken
     * @return accessToken和refreshToken
     */
    public Map<String, Object> refresh(String refreshToken) {
        Map<String, String> tokenMap = tokenService.refreshAccessToken(refreshToken);
        String newAccessToken = tokenMap.get("accessToken");
        String newRefreshToken = tokenMap.get("refreshToken");
        tokenService.storeRefreshToken(Long.parseLong(tokenMap.get("userId")), newRefreshToken);
        return Map.of("accessToken", newAccessToken,
                "refreshToken", newRefreshToken,
                "expiresIn", jwtProperties.getAccessTokenExpiration());
    }

    /**
     * 登出，将当前的accessToken加入黑名单，也就是置为失效
     *
     * @param request 请求信息
     */
    public void logout(HttpServletRequest request) {
        String accessToken = resolveToken(request);
        if (accessToken != null) {
            tokenService.blacklistToken(accessToken);
        }
    }

    /**
     * 注册
     *
     * @param userName 用户名
     * @param password 密码
     * @return 用户信息
     */
    public Map<String, Object> register(String userName, String password, Boolean isCreateDefaultRelation) {
        UserInfo user = customUserDetailsService.saveUser(userName, password, "", isCreateDefaultRelation);
        Map<String, Object> result = new HashMap<>();
        result.put("uid", user.getUid());
        result.put("userName", user.getUserName());
        return result;
    }

    // 从请求头中获取 accessToken
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Collection<String> getAuthorityNames(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }
}