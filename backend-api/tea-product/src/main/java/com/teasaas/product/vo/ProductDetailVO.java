package com.teasaas.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情 VO（F-PRD-01：含配方与规格）
 */
@Data
@Schema(description = "商品详情")
public class ProductDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "商品编码")
    private String productCode;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "配方ID")
    private Long formulaId;

    @Schema(description = "配方名称")
    private String formulaName;

    @Schema(description = "建议售价（分）")
    private Integer suggestPrice;

    @Schema(description = "图片")
    private String image;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "发布状态：1已发布/0草稿（下架）")
    private Integer publishStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "规格组")
    private List<SpecNode> specs;

    /**
     * 规格组节点（温度/糖度）
     */
    @Data
    @Schema(description = "规格组")
    public static class SpecNode implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "规格ID")
        private Long id;

        @Schema(description = "规格名")
        private String specName;

        @Schema(description = "是否必选：1是/0否")
        private Integer isRequired;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "选项列表")
        private List<OptionNode> options;
    }

    /**
     * 规格选项节点（少冰/全糖）
     */
    @Data
    @Schema(description = "规格选项")
    public static class OptionNode implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "选项ID")
        private Long id;

        @Schema(description = "选项名")
        private String optionName;

        @Schema(description = "加价（分）")
        private Integer extraPrice;

        @Schema(description = "排序")
        private Integer sort;
    }
}
