package com.celcioblond.pms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration tokenTtl;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration:PT8H}") Duration tokenTtl) {
        // hmacShaKeyFor rejects keys shorter than 256 bits, so a weak secret fails at startup
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.tokenTtl = tokenTtl;
    }

    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + tokenTtl.toMillis()))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies signature and expiry and returns the username (subject).
     *
     * @throws JwtException if the token is malformed, tampered with or expired
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
