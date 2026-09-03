-- =============================================================
-- 茶饮 SaaS 数据库初始化脚本 · 01 表结构（schema）
-- 依据：docs/茶饮SaaS-数据库设计.md v1.1（39 张表）
-- 幂等：DROP IF EXISTS + CREATE，可重复执行
-- =============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 建库 ----------
CREATE DATABASE IF NOT EXISTS `tea_saas` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `tea_saas`;

-- ============================================================
-- 1. 平台与系统（tea-system）
-- ============================================================

-- 品牌租户（平台表，无 tenant_id）
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
  `id`            BIGINT       NOT NULL                COMMENT '雪花ID',
  `tenant_code`   VARCHAR(32)  NOT NULL                COMMENT '租户编码',
  `tenant_name`   VARCHAR(64)  NOT NULL                COMMENT '品牌名称',
  `contact_name`  VARCHAR(32)  NULL                    COMMENT '联系人',
  `contact_phone` VARCHAR(20)  NULL                    COMMENT '联系电话',
  `package_code`  VARCHAR(32)  NULL                    COMMENT '套餐编码',
  `store_limit`   INT          NOT NULL DEFAULT 0      COMMENT '门店数上限(0=不限)',
  `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '1启用/0停用',
  `expire_time`   DATETIME     NULL                    COMMENT '到期时间',
  `create_by`     BIGINT       NULL,
  `create_time`   DATETIME     NULL,
  `update_by`     BIGINT       NULL,
  `update_time`   DATETIME     NULL,
  `del_flag`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`, `del_flag`)
) ENGINE=InnoDB COMMENT='品牌租户';

-- 套餐（平台表）
DROP TABLE IF EXISTS `sys_package`;
CREATE TABLE `sys_package` (
  `id`               BIGINT      NOT NULL                COMMENT '雪花ID',
  `package_code`     VARCHAR(32) NOT NULL                COMMENT '套餐编码',
  `package_name`     VARCHAR(32) NOT NULL                COMMENT '套餐名',
  `price_per_store`  INT         NOT NULL                COMMENT '单价(分/店/月)',
  `store_limit`      INT         NOT NULL                COMMENT '门店数上限',
  `feature_json`     JSON        NOT NULL                COMMENT '功能开关',
  `status`           TINYINT     NOT NULL DEFAULT 1,
  `create_by`        BIGINT      NULL,
  `create_time`      DATETIME    NULL,
  `update_by`        BIGINT      NULL,
  `update_time`      DATETIME    NULL,
  `del_flag`         TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_package_code` (`package_code`)
) ENGINE=InnoDB COMMENT='套餐配置';

-- 用户
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`         BIGINT        NOT NULL,
  `tenant_id`  BIGINT        NOT NULL DEFAULT 0         COMMENT '0=平台',
  `username`   VARCHAR(32)   NOT NULL,
  `password`   VARCHAR(128)  NOT NULL                   COMMENT 'BCrypt',
  `nickname`   VARCHAR(32)   NULL,
  `phone`      VARCHAR(20)   NULL,
  `user_type`  TINYINT       NOT NULL DEFAULT 1         COMMENT '0平台/1总部/2加盟商/3门店',
  `status`     TINYINT       NOT NULL DEFAULT 1,
  `create_by`  BIGINT        NULL,
  `create_time` DATETIME     NULL,
  `update_by`  BIGINT        NULL,
  `update_time` DATETIME     NULL,
  `del_flag`   TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`tenant_id`, `username`, `del_flag`)
) ENGINE=InnoDB COMMENT='系统用户';

-- 角色
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT       NOT NULL,
  `tenant_id`   BIGINT       NOT NULL DEFAULT 0,
  `role_name`   VARCHAR(32)  NOT NULL,
  `role_code`   VARCHAR(32)  NOT NULL                   COMMENT 'R1-R8',
  `data_scope`  TINYINT      NOT NULL DEFAULT 2         COMMENT '1全部品牌/2本租户/3名下门店/4本店',
  `remark`      VARCHAR(255) NULL,
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `create_by`   BIGINT       NULL,
  `create_time` DATETIME     NULL,
  `update_by`   BIGINT       NULL,
  `update_time` DATETIME     NULL,
  `del_flag`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role` (`tenant_id`, `role_code`, `del_flag`)
) ENGINE=InnoDB COMMENT='角色';

-- 菜单/按钮（平台共享，无 tenant_id）
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`         BIGINT        NOT NULL,
  `parent_id`  BIGINT        NOT NULL DEFAULT 0,
  `menu_name`  VARCHAR(32)   NOT NULL,
  `menu_type`  TINYINT       NOT NULL                   COMMENT '1目录/2菜单/3按钮',
  `path`       VARCHAR(128)  NULL,
  `component`  VARCHAR(128)  NULL,
  `perm`       VARCHAR(64)   NULL                       COMMENT '权限码 bom:edit',
  `sort`       INT           NOT NULL DEFAULT 0,
  `visible`    TINYINT       NOT NULL DEFAULT 1,
  `status`     TINYINT       NOT NULL DEFAULT 1,
  `create_time` DATETIME     NULL,
  `update_time` DATETIME     NULL,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB COMMENT='菜单与按钮权限';

-- 用户-角色
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id`         BIGINT   NOT NULL,
  `tenant_id`  BIGINT   NOT NULL DEFAULT 0,
  `user_id`    BIGINT   NOT NULL,
  `role_id`    BIGINT   NOT NULL,
  `create_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB COMMENT='用户角色关联';

-- 角色-菜单
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id`          BIGINT   NOT NULL,
  `tenant_id`   BIGINT   NOT NULL DEFAULT 0,
  `role_id`     BIGINT   NOT NULL,
  `menu_id`     BIGINT   NOT NULL,
  `create_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE=InnoDB COMMENT='角色菜单关联';

-- 用户-门店绑定（一人多店；数据权限依据）
DROP TABLE IF EXISTS `sys_user_store`;
CREATE TABLE `sys_user_store` (
  `id`          BIGINT   NOT NULL,
  `tenant_id`   BIGINT   NOT NULL,
  `user_id`     BIGINT   NOT NULL,
  `store_id`    BIGINT   NOT NULL,
  `is_default`  TINYINT  NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_store` (`user_id`, `store_id`)
) ENGINE=InnoDB COMMENT='用户门店绑定';

-- 操作日志
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
  `id`          BIGINT        NOT NULL,
  `tenant_id`   BIGINT        NOT NULL DEFAULT 0,
  `user_id`     BIGINT        NULL,
  `module`      VARCHAR(32)   NULL,
  `action`      VARCHAR(32)   NULL,
  `method`      VARCHAR(128)  NULL,
  `params`      JSON          NULL,
  `ip`          VARCHAR(64)   NULL,
  `cost_ms`     INT           NULL,
  `status`      TINYINT       NOT NULL DEFAULT 1        COMMENT '1成功/0失败',
  `create_time` DATETIME      NULL,
  PRIMARY KEY (`id`),
  KEY `idx_oplog` (`tenant_id`, `user_id`, `create_time`)
) ENGINE=InnoDB COMMENT='操作日志';

-- ============================================================
-- 2. 加盟商与门店（tea-franchise）
-- ============================================================
DROP TABLE IF EXISTS `fra_franchisee`;
CREATE TABLE `fra_franchisee` (
  `id`              BIGINT       NOT NULL,
  `tenant_id`       BIGINT       NOT NULL,
  `franchisee_code` VARCHAR(32)  NOT NULL,
  `franchisee_name` VARCHAR(64)  NOT NULL,
  `contact_name`    VARCHAR(32)  NULL,
  `contact_phone`   VARCHAR(20)  NULL,
  `level_code`      VARCHAR(8)   NOT NULL DEFAULT 'C'   COMMENT 'A/B/C',
  `join_date`       DATE         NULL,
  `status`          TINYINT      NOT NULL DEFAULT 1     COMMENT '1合作中/0已终止',
  `create_by`       BIGINT       NULL,
  `create_time`     DATETIME     NULL,
  `update_by`       BIGINT       NULL,
  `update_time`     DATETIME     NULL,
  `del_flag`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_franchisee` (`tenant_id`, `franchisee_code`, `del_flag`),
  KEY `idx_fra_level` (`tenant_id`, `level_code`)
) ENGINE=InnoDB COMMENT='加盟商';

DROP TABLE IF EXISTS `fra_store`;
CREATE TABLE `fra_store` (
  `id`               BIGINT        NOT NULL,
  `tenant_id`        BIGINT        NOT NULL,
  `store_code`       VARCHAR(32)   NOT NULL,
  `store_name`       VARCHAR(64)   NOT NULL,
  `franchisee_id`    BIGINT        NULL                  COMMENT 'NULL=直营',
  `address`          VARCHAR(255)  NULL,
  `longitude`        DECIMAL(10,6) NULL,
  `latitude`         DECIMAL(10,6) NULL,
  `business_status`  TINYINT       NOT NULL DEFAULT 1    COMMENT '1营业/0打烊',
  `open_time`        TIME          NULL,
  `close_time`       TIME          NULL,
  `status`           TINYINT       NOT NULL DEFAULT 1,
  `create_by`        BIGINT        NULL,
  `create_time`      DATETIME      NULL,
  `update_by`        BIGINT        NULL,
  `update_time`      DATETIME      NULL,
  `del_flag`         TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store` (`tenant_id`, `store_code`, `del_flag`),
  KEY `idx_store_fra` (`tenant_id`, `franchisee_id`),
  KEY `idx_store_geo` (`longitude`, `latitude`)
) ENGINE=InnoDB COMMENT='门店';

-- ============================================================
-- 3. 配方 BOM（tea-bom）
-- ============================================================
DROP TABLE IF EXISTS `bom_material`;
CREATE TABLE `bom_material` (
  `id`             BIGINT        NOT NULL,
  `tenant_id`      BIGINT        NOT NULL,
  `material_code`  VARCHAR(32)   NOT NULL,
  `material_name`  VARCHAR(64)   NOT NULL,
  `category`       VARCHAR(32)   NULL,
  `unit`           VARCHAR(8)    NOT NULL               COMMENT 'g/kg/个/杯/L',
  `warn_threshold` DECIMAL(10,3) NULL,
  `cost_price`     INT           NULL                   COMMENT '参考成本(分/单位)',
  `status`         TINYINT       NOT NULL DEFAULT 1,
  `create_by`      BIGINT        NULL,
  `create_time`    DATETIME      NULL,
  `update_by`      BIGINT        NULL,
  `update_time`    DATETIME      NULL,
  `del_flag`       TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_material` (`tenant_id`, `material_code`, `del_flag`)
) ENGINE=InnoDB COMMENT='原料档案';

DROP TABLE IF EXISTS `bom_formula`;
CREATE TABLE `bom_formula` (
  `id`            BIGINT       NOT NULL,
  `tenant_id`     BIGINT       NOT NULL,
  `formula_code`  VARCHAR(32)  NOT NULL,
  `formula_name`  VARCHAR(64)  NOT NULL,
  `version`       INT          NOT NULL DEFAULT 1,
  `cost_total`    INT          NULL                    COMMENT '试算成本(分/杯)',
  `status`        TINYINT      NOT NULL DEFAULT 1,
  `create_by`     BIGINT       NULL,
  `create_time`   DATETIME     NULL,
  `update_by`     BIGINT       NULL,
  `update_time`   DATETIME     NULL,
  `del_flag`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_formula` (`tenant_id`, `formula_code`, `del_flag`)
) ENGINE=InnoDB COMMENT='配方';

DROP TABLE IF EXISTS `bom_formula_item`;
CREATE TABLE `bom_formula_item` (
  `id`           BIGINT        NOT NULL,
  `tenant_id`    BIGINT        NOT NULL,
  `formula_id`   BIGINT        NOT NULL,
  `material_id`  BIGINT        NOT NULL,
  `qty`          DECIMAL(10,3) NOT NULL                COMMENT '用量',
  `sort`         INT           NOT NULL DEFAULT 0,
  `create_time`  DATETIME      NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_formula_material` (`formula_id`, `material_id`),
  KEY `idx_fitem_material` (`material_id`)
) ENGINE=InnoDB COMMENT='配方明细';

-- ============================================================
-- 4. 商品与上架（tea-product）
-- ============================================================
DROP TABLE IF EXISTS `prd_category`;
CREATE TABLE `prd_category` (
  `id`           BIGINT      NOT NULL,
  `tenant_id`    BIGINT      NOT NULL,
  `category_name` VARCHAR(32) NOT NULL,
  `parent_id`    BIGINT      NOT NULL DEFAULT 0,
  `sort`         INT         NOT NULL DEFAULT 0,
  `create_time`  DATETIME    NULL,
  `del_flag`     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='商品分类';

DROP TABLE IF EXISTS `prd_product`;
CREATE TABLE `prd_product` (
  `id`              BIGINT       NOT NULL,
  `tenant_id`       BIGINT       NOT NULL,
  `category_id`     BIGINT       NULL,
  `product_code`    VARCHAR(32)  NOT NULL,
  `product_name`    VARCHAR(64)  NOT NULL,
  `formula_id`      BIGINT       NOT NULL,
  `suggest_price`   INT          NOT NULL               COMMENT '建议售价(分)',
  `image`           VARCHAR(255) NULL,
  `description`     VARCHAR(255) NULL,
  `publish_status`  TINYINT      NOT NULL DEFAULT 0     COMMENT '1已发布/0草稿',
  `status`          TINYINT      NOT NULL DEFAULT 1,
  `create_by`       BIGINT       NULL,
  `create_time`     DATETIME     NULL,
  `update_by`       BIGINT       NULL,
  `update_time`     DATETIME     NULL,
  `del_flag`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product` (`tenant_id`, `product_code`, `del_flag`),
  KEY `idx_product_category` (`tenant_id`, `category_id`)
) ENGINE=InnoDB COMMENT='商品';

DROP TABLE IF EXISTS `prd_product_spec`;
CREATE TABLE `prd_product_spec` (
  `id`          BIGINT      NOT NULL,
  `tenant_id`   BIGINT      NOT NULL,
  `product_id`  BIGINT      NOT NULL,
  `spec_name`   VARCHAR(16) NOT NULL                   COMMENT '温度/糖度',
  `is_required` TINYINT     NOT NULL DEFAULT 1,
  `sort`        INT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_spec_product` (`product_id`)
) ENGINE=InnoDB COMMENT='商品规格组';

DROP TABLE IF EXISTS `prd_product_spec_option`;
CREATE TABLE `prd_product_spec_option` (
  `id`           BIGINT      NOT NULL,
  `tenant_id`    BIGINT      NOT NULL,
  `spec_id`      BIGINT      NOT NULL,
  `option_name`  VARCHAR(16) NOT NULL,
  `extra_price`  INT         NOT NULL DEFAULT 0        COMMENT '加价(分)',
  `sort`         INT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_option_spec` (`spec_id`)
) ENGINE=InnoDB COMMENT='规格选项';

DROP TABLE IF EXISTS `prd_listing`;
CREATE TABLE `prd_listing` (
  `id`          BIGINT   NOT NULL,
  `tenant_id`   BIGINT   NOT NULL,
  `store_id`    BIGINT   NOT NULL,
  `product_id`  BIGINT   NOT NULL,
  `sale_price`  INT      NOT NULL                      COMMENT '本店售价(分)',
  `sort`        INT      NOT NULL DEFAULT 0,
  `status`      TINYINT  NOT NULL DEFAULT 1            COMMENT '1上架/0下架',
  `create_by`   BIGINT   NULL,
  `create_time` DATETIME NULL,
  `update_by`   BIGINT   NULL,
  `update_time` DATETIME NULL,
  `del_flag`    TINYINT  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_listing` (`tenant_id`, `store_id`, `product_id`, `del_flag`),
  KEY `idx_listing_store` (`tenant_id`, `store_id`, `status`)
) ENGINE=InnoDB COMMENT='门店上架';

-- ============================================================
-- 5. 供应链（tea-supply）
-- ============================================================
DROP TABLE IF EXISTS `sup_supplier`;
CREATE TABLE `sup_supplier` (
  `id`              BIGINT       NOT NULL,
  `tenant_id`       BIGINT       NOT NULL,
  `supplier_code`   VARCHAR(32)  NOT NULL,
  `supplier_name`   VARCHAR(64)  NOT NULL,
  `contact_name`    VARCHAR(32)  NULL,
  `contact_phone`   VARCHAR(20)  NULL,
  `payment_terms`   VARCHAR(64)  NULL,
  `status`          TINYINT      NOT NULL DEFAULT 1,
  `create_by`       BIGINT       NULL,
  `create_time`     DATETIME     NULL,
  `update_by`       BIGINT       NULL,
  `update_time`     DATETIME     NULL,
  `del_flag`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier` (`tenant_id`, `supplier_code`, `del_flag`)
) ENGINE=InnoDB COMMENT='供应商';

DROP TABLE IF EXISTS `sup_supplier_material`;
CREATE TABLE `sup_supplier_material` (
  `id`            BIGINT   NOT NULL,
  `tenant_id`     BIGINT   NOT NULL,
  `supplier_id`   BIGINT   NOT NULL,
  `material_id`   BIGINT   NOT NULL,
  `supply_price`  INT      NULL                       COMMENT '参考供货价(分)',
  `create_time`   DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sup_material` (`supplier_id`, `material_id`)
) ENGINE=InnoDB COMMENT='供应商可供应原料';

DROP TABLE IF EXISTS `sup_tier_price`;
CREATE TABLE `sup_tier_price` (
  `id`          BIGINT      NOT NULL,
  `tenant_id`   BIGINT      NOT NULL,
  `material_id` BIGINT      NOT NULL,
  `level_code`  VARCHAR(8)  NOT NULL                  COMMENT 'A/B/C',
  `price`       INT         NOT NULL                  COMMENT '单价(分)',
  `update_by`   BIGINT      NULL,
  `update_time` DATETIME    NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tier_price` (`tenant_id`, `material_id`, `level_code`)
) ENGINE=InnoDB COMMENT='分级价格';

DROP TABLE IF EXISTS `sup_purchase_order`;
CREATE TABLE `sup_purchase_order` (
  `id`             BIGINT       NOT NULL,
  `tenant_id`      BIGINT       NOT NULL,
  `order_no`       VARCHAR(32)  NOT NULL,
  `store_id`       BIGINT       NOT NULL,
  `franchisee_id`  BIGINT       NULL,
  `level_code`     VARCHAR(8)   NULL                  COMMENT '计价等级快照',
  `status`         TINYINT      NOT NULL DEFAULT 1    COMMENT '1待确认/2已确认/3已发货/4已入库/5已取消',
  `total_amount`   BIGINT       NOT NULL DEFAULT 0    COMMENT '总额(分)',
  `confirm_by`     BIGINT       NULL,
  `ship_by`        BIGINT       NULL,
  `receive_by`     BIGINT       NULL,
  `confirm_time`   DATETIME     NULL,
  `ship_time`      DATETIME     NULL,
  `receive_time`   DATETIME     NULL,
  `remark`         VARCHAR(255) NULL,
  `create_by`      BIGINT       NULL,
  `create_time`    DATETIME     NULL,
  `update_by`      BIGINT       NULL,
  `update_time`    DATETIME     NULL,
  `del_flag`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_purchase_no` (`tenant_id`, `order_no`),
  KEY `idx_purchase_store` (`tenant_id`, `store_id`, `status`)
) ENGINE=InnoDB COMMENT='采购单';

DROP TABLE IF EXISTS `sup_purchase_order_item`;
CREATE TABLE `sup_purchase_order_item` (
  `id`          BIGINT        NOT NULL,
  `tenant_id`   BIGINT        NOT NULL,
  `order_id`    BIGINT        NOT NULL,
  `material_id` BIGINT        NOT NULL,
  `qty`         DECIMAL(10,3) NOT NULL,
  `unit_price`  INT           NOT NULL                COMMENT '成交单价快照(分)',
  `amount`      BIGINT        NOT NULL                COMMENT '小计(分)',
  PRIMARY KEY (`id`),
  KEY `idx_pitem_order` (`order_id`)
) ENGINE=InnoDB COMMENT='采购单明细';

-- ============================================================
-- 6. 库存（tea-inventory）
-- ============================================================
DROP TABLE IF EXISTS `inv_store_material_stock`;
CREATE TABLE `inv_store_material_stock` (
  `id`          BIGINT        NOT NULL,
  `tenant_id`   BIGINT        NOT NULL,
  `store_id`    BIGINT        NOT NULL,
  `material_id` BIGINT        NOT NULL,
  `stock_qty`   DECIMAL(10,3) NOT NULL DEFAULT 0       COMMENT '禁止负值',
  `version`     INT           NOT NULL DEFAULT 0       COMMENT '乐观锁',
  `update_by`   BIGINT        NULL,
  `update_time` DATETIME      NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock` (`tenant_id`, `store_id`, `material_id`),
  KEY `idx_stock_material` (`tenant_id`, `material_id`)
) ENGINE=InnoDB COMMENT='门店原料库存';

DROP TABLE IF EXISTS `inv_stock_log`;
CREATE TABLE `inv_stock_log` (
  `id`          BIGINT        NOT NULL,
  `tenant_id`   BIGINT        NOT NULL,
  `store_id`    BIGINT        NOT NULL,
  `material_id` BIGINT        NOT NULL,
  `change_qty`  DECIMAL(10,3) NOT NULL                 COMMENT '入正出负',
  `before_qty`  DECIMAL(10,3) NOT NULL,
  `after_qty`   DECIMAL(10,3) NOT NULL,
  `biz_type`    TINYINT       NOT NULL                 COMMENT '1下单扣/2超时回补/3退款回补/4采购入库/5盘点',
  `biz_no`      VARCHAR(32)   NULL,
  `remark`      VARCHAR(128)  NULL,
  `create_by`   BIGINT        NULL,
  `create_time` DATETIME      NULL,
  PRIMARY KEY (`id`),
  KEY `idx_stock_log` (`tenant_id`, `store_id`, `biz_type`),
  KEY `idx_stock_log_biz` (`tenant_id`, `biz_no`)
) ENGINE=InnoDB COMMENT='库存变动流水';

-- ============================================================
-- 7. 订单与支付（tea-order / tea-payment）
-- ============================================================
DROP TABLE IF EXISTS `ord_order`;
CREATE TABLE `ord_order` (
  `id`              BIGINT       NOT NULL,
  `tenant_id`       BIGINT       NOT NULL,
  `order_no`        VARCHAR(32)  NOT NULL,
  `store_id`        BIGINT       NOT NULL,
  `member_id`       BIGINT       NULL                  COMMENT '门店收银可空',
  `order_source`    TINYINT      NOT NULL DEFAULT 1    COMMENT '1小程序/2门店收银',
  `status`          TINYINT      NOT NULL DEFAULT 1    COMMENT '1待支付/2已支付/3制作中/4待取餐/5已完成/6已取消/7已退款',
  `total_amount`    BIGINT       NOT NULL DEFAULT 0,
  `discount_amount` BIGINT       NOT NULL DEFAULT 0,
  `pay_amount`      BIGINT       NOT NULL DEFAULT 0    COMMENT '实付(分)',
  `coupon_id`       BIGINT       NULL,
  `coupon_amount`   BIGINT       NULL,
  `points_used`     INT          NULL,
  `points_amount`   BIGINT       NULL,
  `balance_amount`  BIGINT       NULL                  COMMENT '储值支付(分)',
  `pay_channel`     TINYINT      NOT NULL DEFAULT 1    COMMENT '1微信/2储值/3现金',
  `take_code`       VARCHAR(8)   NULL,
  `pay_time`        DATETIME     NULL,
  `finish_time`     DATETIME     NULL,
  `cancel_reason`   VARCHAR(128) NULL,
  `remark`          VARCHAR(255) NULL,
  `create_by`       BIGINT       NULL,
  `create_time`     DATETIME     NULL,
  `update_by`       BIGINT       NULL,
  `update_time`     DATETIME     NULL,
  `del_flag`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`tenant_id`, `order_no`),
  KEY `idx_order_store` (`tenant_id`, `store_id`, `status`, `create_time`),
  KEY `idx_order_member` (`tenant_id`, `member_id`)
) ENGINE=InnoDB COMMENT='订单';

DROP TABLE IF EXISTS `ord_order_item`;
CREATE TABLE `ord_order_item` (
  `id`           BIGINT       NOT NULL,
  `tenant_id`    BIGINT       NOT NULL,
  `order_id`     BIGINT       NOT NULL,
  `product_id`   BIGINT       NOT NULL,
  `listing_id`   BIGINT       NULL,
  `product_name` VARCHAR(64)  NOT NULL                 COMMENT '快照',
  `spec_desc`    VARCHAR(64)  NULL                     COMMENT '规格快照',
  `price`        INT          NOT NULL                 COMMENT '成交单价(分)',
  `qty`          INT          NOT NULL,
  `amount`       BIGINT       NOT NULL,
  `formula_id`   BIGINT       NOT NULL                 COMMENT '扣库存配方快照',
  `create_time`  DATETIME     NULL,
  PRIMARY KEY (`id`),
  KEY `idx_oitem_order` (`order_id`)
) ENGINE=InnoDB COMMENT='订单明细';

DROP TABLE IF EXISTS `pay_payment`;
CREATE TABLE `pay_payment` (
  `id`             BIGINT       NOT NULL,
  `tenant_id`      BIGINT       NOT NULL,
  `payment_no`     VARCHAR(32)  NOT NULL,
  `order_id`       BIGINT       NOT NULL,
  `channel`        TINYINT      NOT NULL DEFAULT 1     COMMENT '1微信/2储值',
  `amount`         BIGINT       NOT NULL,
  `status`         TINYINT      NOT NULL DEFAULT 1     COMMENT '1待支付/2成功/3失败/4已关闭',
  `transaction_id` VARCHAR(64)  NULL,
  `callback_time`  DATETIME     NULL,
  `create_time`    DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`tenant_id`, `payment_no`),
  KEY `idx_payment_order` (`tenant_id`, `order_id`)
) ENGINE=InnoDB COMMENT='支付流水';

DROP TABLE IF EXISTS `pay_refund`;
CREATE TABLE `pay_refund` (
  `id`              BIGINT       NOT NULL,
  `tenant_id`       BIGINT       NOT NULL,
  `refund_no`       VARCHAR(32)  NOT NULL,
  `order_id`        BIGINT       NOT NULL,
  `payment_id`      BIGINT       NOT NULL,
  `amount`          BIGINT       NOT NULL               COMMENT '整单退=实付',
  `refund_type`     TINYINT      NOT NULL DEFAULT 1     COMMENT '1微信原路/2退储值',
  `coupon_returned` TINYINT      NOT NULL DEFAULT 0     COMMENT '券不退还=0',
  `reason`          VARCHAR(128) NULL,
  `status`          TINYINT      NOT NULL DEFAULT 1     COMMENT '1处理中/2成功/3失败',
  `refund_time`     DATETIME     NULL,
  `create_time`     DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`tenant_id`, `refund_no`),
  KEY `idx_refund_order` (`tenant_id`, `order_id`)
) ENGINE=InnoDB COMMENT='退款流水';

DROP TABLE IF EXISTS `pay_callback_task`;
CREATE TABLE `pay_callback_task` (
  `id`              BIGINT       NOT NULL,
  `tenant_id`       BIGINT       NOT NULL DEFAULT 0,
  `task_no`         VARCHAR(32)  NOT NULL,
  `biz_type`        TINYINT      NOT NULL              COMMENT '1支付/2退款',
  `biz_no`          VARCHAR(32)  NOT NULL,
  `payload`         JSON         NOT NULL,
  `retry_count`     INT          NOT NULL DEFAULT 0,
  `max_retry`       INT          NOT NULL DEFAULT 5,
  `status`          TINYINT      NOT NULL DEFAULT 1    COMMENT '1待处理/2成功/3失败终态',
  `next_retry_time` DATETIME     NOT NULL,
  `last_error`      VARCHAR(255) NULL,
  `create_time`     DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cbtask_no` (`tenant_id`, `task_no`),
  KEY `idx_cbtask_status` (`tenant_id`, `status`, `next_retry_time`)
) ENGINE=InnoDB COMMENT='支付回调重试任务';

-- ============================================================
-- 8. 会员（tea-member）
-- ============================================================
DROP TABLE IF EXISTS `mem_member`;
CREATE TABLE `mem_member` (
  `id`           BIGINT       NOT NULL,
  `tenant_id`    BIGINT       NOT NULL,
  `openid`       VARCHAR(64)  NOT NULL,
  `unionid`      VARCHAR(64)  NULL,
  `nickname`     VARCHAR(32)  NULL,
  `avatar`       VARCHAR(255) NULL,
  `phone`        VARCHAR(20)  NULL,
  `level_id`     BIGINT       NULL,
  `total_points` INT          NOT NULL DEFAULT 0,
  `status`       TINYINT      NOT NULL DEFAULT 1,
  `create_time`  DATETIME     NULL,
  `update_time`  DATETIME     NULL,
  `del_flag`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_openid` (`tenant_id`, `openid`),
  KEY `idx_member_phone` (`tenant_id`, `phone`)
) ENGINE=InnoDB COMMENT='会员';

DROP TABLE IF EXISTS `mem_member_level`;
CREATE TABLE `mem_member_level` (
  `id`          BIGINT      NOT NULL,
  `tenant_id`   BIGINT      NOT NULL,
  `level_name`  VARCHAR(16) NOT NULL,
  `min_points`  INT         NOT NULL DEFAULT 0,
  `discount`    TINYINT     NULL                     COMMENT '95=95折',
  `create_time` DATETIME    NULL,
  `del_flag`    TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='会员等级';

DROP TABLE IF EXISTS `mem_points_log`;
CREATE TABLE `mem_points_log` (
  `id`             BIGINT   NOT NULL,
  `tenant_id`      BIGINT   NOT NULL,
  `member_id`      BIGINT   NOT NULL,
  `change_points`  INT      NOT NULL,
  `before_points`  INT      NOT NULL,
  `after_points`   INT      NOT NULL,
  `biz_type`       TINYINT  NOT NULL                  COMMENT '1消费得/2抵现扣/3赠送/4过期扣',
  `biz_no`         VARCHAR(32) NULL,
  `create_time`    DATETIME NULL,
  PRIMARY KEY (`id`),
  KEY `idx_points_log` (`tenant_id`, `member_id`, `create_time`)
) ENGINE=InnoDB COMMENT='积分流水';

DROP TABLE IF EXISTS `mem_wallet`;
CREATE TABLE `mem_wallet` (
  `id`             BIGINT   NOT NULL,
  `tenant_id`      BIGINT   NOT NULL,
  `member_id`      BIGINT   NOT NULL,
  `balance`        BIGINT   NOT NULL DEFAULT 0        COMMENT '分',
  `total_recharge` BIGINT   NOT NULL DEFAULT 0,
  `total_consume`  BIGINT   NOT NULL DEFAULT 0,
  `update_time`    DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wallet` (`tenant_id`, `member_id`)
) ENGINE=InnoDB COMMENT='储值账户';

DROP TABLE IF EXISTS `mem_recharge_record`;
CREATE TABLE `mem_recharge_record` (
  `id`             BIGINT      NOT NULL,
  `tenant_id`      BIGINT      NOT NULL,
  `member_id`      BIGINT      NOT NULL,
  `amount`         BIGINT      NOT NULL,
  `bonus`          BIGINT      NOT NULL DEFAULT 0     COMMENT '赠送(分)',
  `balance_after`  BIGINT      NOT NULL,
  `biz_no`         VARCHAR(32) NULL,
  `create_time`    DATETIME    NULL,
  PRIMARY KEY (`id`),
  KEY `idx_recharge_member` (`tenant_id`, `member_id`)
) ENGINE=InnoDB COMMENT='储值记录';

-- ============================================================
-- 9. 营销（tea-marketing）
-- ============================================================
DROP TABLE IF EXISTS `mkt_coupon_template`;
CREATE TABLE `mkt_coupon_template` (
  `id`          BIGINT        NOT NULL,
  `tenant_id`   BIGINT        NOT NULL,
  `name`        VARCHAR(32)   NOT NULL,
  `type`        TINYINT       NOT NULL                 COMMENT '1满减/2折扣',
  `threshold`   BIGINT        NULL                     COMMENT '满X分',
  `amount`      BIGINT        NULL                     COMMENT '减Y分',
  `discount`    DECIMAL(3,2)  NULL                     COMMENT '0.90',
  `total_qty`   INT           NOT NULL DEFAULT -1     COMMENT '-1不限',
  `per_limit`   INT           NOT NULL DEFAULT 1,
  `valid_type`  TINYINT       NOT NULL DEFAULT 1       COMMENT '1固定/2领取后N天',
  `valid_start` DATETIME      NULL,
  `valid_end`   DATETIME      NULL,
  `valid_days`  INT           NULL,
  `status`      TINYINT       NOT NULL DEFAULT 1,
  `create_by`   BIGINT        NULL,
  `create_time` DATETIME      NULL,
  `update_time` DATETIME      NULL,
  `del_flag`    TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='券模板';

DROP TABLE IF EXISTS `mkt_coupon_scope`;
CREATE TABLE `mkt_coupon_scope` (
  `id`         BIGINT   NOT NULL,
  `tenant_id`  BIGINT   NOT NULL,
  `coupon_id`  BIGINT   NOT NULL,
  `scope_type` TINYINT  NOT NULL                      COMMENT '1全场/2指定商品/3指定门店',
  `target_id`  BIGINT   NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cscope` (`coupon_id`)
) ENGINE=InnoDB COMMENT='券适用范围';

DROP TABLE IF EXISTS `mkt_coupon`;
CREATE TABLE `mkt_coupon` (
  `id`            BIGINT   NOT NULL,
  `tenant_id`     BIGINT   NOT NULL,
  `template_id`   BIGINT   NOT NULL,
  `member_id`     BIGINT   NOT NULL,
  `status`        TINYINT  NOT NULL DEFAULT 1         COMMENT '1未用/2已用/3过期',
  `use_order_id`  BIGINT   NULL,
  `valid_start`   DATETIME NOT NULL,
  `valid_end`     DATETIME NOT NULL,
  `receive_time`  DATETIME NOT NULL,
  `use_time`      DATETIME NULL,
  PRIMARY KEY (`id`),
  KEY `idx_coupon_member` (`tenant_id`, `member_id`, `status`)
) ENGINE=InnoDB COMMENT='用户券';

SET FOREIGN_KEY_CHECKS = 1;
