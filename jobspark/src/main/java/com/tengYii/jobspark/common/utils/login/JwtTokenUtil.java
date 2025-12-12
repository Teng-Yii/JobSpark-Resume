package com.tengYii.jobspark.common.utils.login;

import com.tengYii.jobspark.common.utils.RedisUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenUtil {

    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";

    /**
     * JWT签名密钥
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Token过期时间（秒）默认2小时
     */
    @Value("${jwt.access-token-expiration:7200}")
    private Long accessTokenExpiration;

    /**
     * 用于黑名单控制
     */
    @Autowired
    private RedisUtil redisUtil;


    /**
     * 生成 Access Token（短期）
     */
    public String generateAccessToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration * 1000))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 将 Token 加入黑名单（用于登出）
     *
     * @param token         完整的 JWT 字符串
     * @param expireSeconds 黑名单保留时间（应 >= token 剩余有效期）
     */
    public void blacklistToken(String token, long expireSeconds) {
        if (StringUtils.isEmpty(token)) return;

        try {
            // 解析过期时间，确保黑名单至少保留到原 Token 过期
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            long actualExpire = Math.max(expireSeconds,
                    (expiration.getTime() - System.currentTimeMillis()) / 1000);

            // 存入 Redis，key 为 token hash（避免存储完整 token）
            String tokenHash = DigestUtils.sha256Hex(token);
            // 使用 RedisUtil 设置带过期时间的值
            String key = TOKEN_BLACKLIST_PREFIX + tokenHash;
            redisUtil.set(key, "invalid", actualExpire, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 如果 token 无效，无需加入黑名单
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        if (StringUtils.isEmpty(token)) return false;
        String tokenHash = DigestUtils.sha256Hex(token);
        String key = TOKEN_BLACKLIST_PREFIX + tokenHash;
        return redisUtil.hasKey(key);
    }

    /**
     * 从 Token 中解析用户 ID
     */
    public Long getUserIdFromToken(String token) {
        if (StringUtils.isEmpty(token)) return null;
        try {
            Claims claims = parseClaims(token);
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证 Token 是否有效（未过期 + 未被加入黑名单）
     */
    public Boolean validateToken(String token) {
        if (StringUtils.isEmpty(token)) return false;

        // 先检查黑名单
        if (isTokenBlacklisted(token)) {
            return false;
        }

        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 安全解析 Claims（复用）
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
}