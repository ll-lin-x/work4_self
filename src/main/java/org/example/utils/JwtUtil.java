package org.example.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class JwtUtil {
    //有效期为
    private static final Long JWT_TTL = 60 * 60 * 1000L;// 60 * 60 *1000 一个小时
    //设置秘钥明文
    private static final String JWT_KEY = "linxi-linxi-linxi-linxi-linxi";
    // 生成安全密钥
    private static final SecretKey SECRET_KEY = new SecretKeySpec(JWT_KEY.getBytes(), "HmacSHA256");

    public static String getUUID() {
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        return token;
    }

    /**
     * 生成jtw
     *
     * @param subject token中要存放的数据（json格式）
     * @return
     */
    public static String createJWT(String subject) {
        return createJWT(subject, JWT_TTL);
    }

    /**
     * 生成jtw
     *
     * @param subject   token中要存放的数据（json格式）
     * @param ttlMillis token超时时间
     * @return
     */
    public static String createJWT(String subject, Long ttlMillis) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        if (ttlMillis == null) {
            ttlMillis = JwtUtil.JWT_TTL;
        }
        long expMillis = nowMillis + ttlMillis;
        Date expDate = new Date(expMillis);
        return Jwts.builder()
                .setId(getUUID()) //唯一的ID
                .setSubject(subject) // 主题 可以是JSON数据
                .setIssuer("lx") // 签发者
                .setIssuedAt(now)
                .setExpiration(expDate)// 签发时间
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY) //使用HS256对称加密算法签名, 第二个参数为秘钥
                .compact();
    }
    






    /**
     * 解析
     *
     * @param jwt
     * @return
     * @throws Exception
     */
    public static Claims parseJWT(String jwt) throws Exception {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(jwt)
                .getBody();
    }
}

