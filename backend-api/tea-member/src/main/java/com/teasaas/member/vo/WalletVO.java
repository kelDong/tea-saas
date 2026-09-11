package com.teasaas.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 储值账户出参（F-MEM-03，接口需求 10.3）
 */
@Data
@Schema(description = "储值账户")
public class WalletVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "余额(分)")
    private Long balance;

    @Schema(description = "累计充值(分，不含赠送)")
    private Long totalRecharge;

    @Schema(description = "累计消费(分)")
    private Long totalConsume;
}
