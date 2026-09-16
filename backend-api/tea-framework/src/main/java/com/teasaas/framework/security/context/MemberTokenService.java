package com.teasaas.framework.security.context;

import com.teasaas.common.constant.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * C 端会员会话 token（Review §2 会话化；与 Sa-Token 体系并存互不干扰，不进 StpUtil）
 *
 * <p>token = UUID，Redis {@code tea-saas:member:token:{token} → "tenantId:memberId"}，
 * TTL 默认 7 天（tea.app.member-token-ttl-days）。签发方：wx-login；校验方：C 端拦截器。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberTokenService {

    private static final String SEPARATOR = ":";

    private final StringRedisTemplate stringRedisTemplate;

    /** token 有效期（天）；正式运营可与 Sa-Token 会话时长对齐调整 */
    @Value("${tea.app.member-token-ttl-days:7}")
    private long ttlDays;

    /** 签发：写 Redis 并返回 token（wx-login 调用） */
    public String issue(Long tenantId, Long memberId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(RedisConstants.MEMBER_TOKEN + token,
                tenantId + SEPARATOR + memberId, Duration.ofDays(ttlDays));
        return token;
    }

    /**
     * 解析：token → 会员身份；不存在/过期/格式损坏返回 null（拦截器按未登录拒绝）。
     * 不过期续期（7 天滑动窗口语义留给二期，当前固定窗口足够小程序场景）。
     */
    public MemberContext.MemberInfo resolve(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String value = stringRedisTemplate.opsForValue().get(RedisConstants.MEMBER_TOKEN + token);
        if (value == null) {
            return null;
        }
        int idx = value.indexOf(SEPARATOR);
        if (idx <= 0 || idx == value.length() - 1) {
            log.warn("会员token值格式异常，已作废 value={}", value);
            stringRedisTemplate.delete(RedisConstants.MEMBER_TOKEN + token);
            return null;
        }
        try {
            return new MemberContext.MemberInfo(Long.parseLong(value.substring(0, idx)),
                    Long.parseLong(value.substring(idx + 1)));
        } catch (NumberFormatException e) {
            log.warn("会员token值解析失败，已作废 value={}", value);
            stringRedisTemplate.delete(RedisConstants.MEMBER_TOKEN + token);
            return null;
        }
    }
}
