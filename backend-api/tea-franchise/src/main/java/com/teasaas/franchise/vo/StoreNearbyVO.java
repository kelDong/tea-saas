package com.teasaas.franchise.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * C 端附近门店 VO（F-FRA-02；距离为 Haversine 计算值，四舍五入整数米）
 */
@Data
@Schema(description = "附近门店")
public class StoreNearbyVO {

    @Schema(description = "门店 ID")
    private Long id;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "营业开始时间，如 09:30")
    private String openTime;

    @Schema(description = "营业结束时间，如 22:00")
    private String closeTime;

    @Schema(description = "距离（米，四舍五入整数）")
    private Long distanceMeters;
}
