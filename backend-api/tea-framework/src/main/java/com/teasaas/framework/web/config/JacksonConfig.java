package com.teasaas.framework.web.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Jackson 全局配置：Long 超出 JS Number.MAX_SAFE_INTEGER(2^53-1) 时序列化为字符串
 *
 * <p>背景：主键为雪花 ID（19 位），JSON 数字字面量经 JSON.parse 后精度丢失
 * （...5201 变 ...5200），前端按 id 回查后端必然 404——回测"开账号提示加盟商不存在"根因。
 * 安全范围内的 Long（total/tenantId/userId/金额分等小整数）仍输出数字，前端 TS 类型无需改动；
 * 雪花 ID 在前端作为不透明字符串透传（拼 URL / 回传 payload，Jackson 反序列化字符串转 Long 兼容）。</p>
 */
@Configuration
public class JacksonConfig {

    /** JS 安全整数边界 2^53-1 */
    private static final long JS_SAFE_MAX = 9_007_199_254_740_991L;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer safeLongCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, new SafeLongSerializer());
            builder.serializerByType(Long.TYPE, new SafeLongSerializer());
        };
    }

    static class SafeLongSerializer extends JsonSerializer<Long> {
        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value != null && (value > JS_SAFE_MAX || value < -JS_SAFE_MAX)) {
                gen.writeString(value.toString());
            } else {
                gen.writeNumber(value);
            }
        }
    }
}
