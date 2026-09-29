package com.teasaas.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 券模板详情出参（F-MKT-01，接口需求 11.1；编辑回填含适用范围）
 *
 * <p>范围口径：mkt_coupon_scope 无行 = 全场（scopeType=1），
 * 存在 scope_type=2 行 = 指定商品券。</p>
 */
@Data
@Schema(description = "券模板详情")
public class CouponTemplateDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "类型：1满减/2折扣")
    private Integer type;

    @Schema(description = "门槛（分）")
    private Long threshold;

    @Schema(description = "满减额（分）")
    private Long amount;

    @Schema(description = "折扣率")
    private BigDecimal discount;

    @Schema(description = "发放总量（-1 不限）")
    private Integer totalQty;

    @Schema(description = "每人限领")
    private Integer perLimit;

    @Schema(description = "有效期类型：1固定时段/2领取后N天")
    private Integer validType;

    @Schema(description = "固定时段起")
    private LocalDateTime validStart;

    @Schema(description = "固定时段止")
    private LocalDateTime validEnd;

    @Schema(description = "领取后有效天数")
    private Integer validDays;

    @Schema(description = "状态：1启用/2停用")
    private Integer status;

    @Schema(description = "适用范围：1全场/2指定商品")
    private Integer scopeType;

    @Schema(description = "指定商品列表（scopeType=2 时非空）")
    private List<ScopeNode> products = new ArrayList<>();

    /**
     * 范围商品节点（商品已删除时 productName 为空，前端显示占位）
     */
    @Data
    @Schema(description = "券适用商品")
    public static class ScopeNode implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "商品ID")
        private Long productId;

        @Schema(description = "商品名称")
        private String productName;

        public ScopeNode() {
        }

        public ScopeNode(Long productId, String productName) {
            this.productId = productId;
            this.productName = productName;
        }
    }
}
