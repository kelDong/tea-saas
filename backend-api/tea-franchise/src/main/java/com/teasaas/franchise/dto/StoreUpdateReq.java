package com.teasaas.franchise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 编辑门店入参（F-FRA-02；storeCode 可改但同样走租户内查重）
 */
@Data
public class StoreUpdateReq {

    @NotBlank(message = "门店编码不能为空")
    @Size(max = 32, message = "门店编码最长 32 字符")
    private String storeCode;

    @NotBlank(message = "门店名称不能为空")
    @Size(max = 64, message = "门店名称最长 64 字符")
    private String storeName;

    /** 可空=直营 */
    private Long franchiseeId;

    @Size(max = 255, message = "地址最长 255 字符")
    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private LocalTime openTime;

    private LocalTime closeTime;
}
