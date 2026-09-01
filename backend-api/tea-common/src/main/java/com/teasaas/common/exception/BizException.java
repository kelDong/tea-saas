package com.teasaas.common.exception;

import com.teasaas.common.result.ErrorCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 业务异常（工程规范红线 4：BizException 体系，禁止原始异常泄露到 API 层）
 */
@Getter
public class BizException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
