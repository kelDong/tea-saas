-- =============================================================
-- 茶饮 SaaS 数据库初始化脚本 · 02 种子数据（data）
-- 依据：数据库设计 v1.1 第 15.2 节 + PRD 5.2/5.3 权限矩阵
-- 幂等：先 DELETE 后 INSERT
-- =============================================================
USE `tea_saas`;

-- ---------- 套餐三档（P1-4 决策） ----------
DELETE FROM `sys_package` WHERE `id` IN (101, 102, 103);
INSERT INTO `sys_package` (`id`, `package_code`, `package_name`, `price_per_store`, `store_limit`, `feature_json`, `status`, `create_time`) VALUES
(101, 'BASIC',    '基础版', 9900,   5,  '{"order":true,"pay":true,"report":"basic"}',                    1, NOW()),
(102, 'PRO',      '专业版', 19900, 20,  '{"order":true,"pay":true,"member":true,"marketing":true}',       1, NOW()),
(103, 'FLAGSHIP', '旗舰版', 29900,  0,  '{"order":true,"pay":true,"member":true,"marketing":true,"data":"advanced"}', 1, NOW());

-- ---------- 菜单与按钮（固定ID段；perm 对应 PRD 5.3） ----------
DELETE FROM `sys_menu`;
INSERT INTO `sys_menu` (`id`,`parent_id`,`menu_name`,`menu_type`,`path`,`component`,`perm`,`sort`,`status`) VALUES
-- 平台端（R1）
(100, 0,   '平台管理', 1, '/platform',   NULL, NULL, 100, 1),
(101, 100, '平台工作台', 2, 'dashboard',  'platform/Dashboard',  NULL, 101, 1),
(102, 100, '品牌管理',   2, 'tenants',    'platform/Tenants',    NULL, 102, 1),
(103, 100, '套餐管理',   2, 'packages',   'platform/Packages',   NULL, 103, 1),
-- 经营（总部）
(200, 0,   '经营',   1, '/biz',        NULL, NULL, 200, 1),
(201, 200, '品牌工作台', 2, 'workspace',  'biz/Workspace',   NULL, 201, 1),
(202, 200, '门店报表',   2, 'store-report','biz/StoreReport', NULL, 202, 1),
(203, 200, '品牌汇总报表', 2, 'dashboard-report','biz/BrandReport', NULL, 203, 1),
(204, 200, '订单查询',   2, 'orders',     'biz/Orders',      NULL, 204, 1),
(205, 200, '会员管理',   2, 'members',    'biz/Members',     NULL, 205, 1),
(206, 200, '优惠券管理', 2, 'coupons',    'biz/Coupons',     NULL, 206, 1),
-- 产品（研发）
(300, 0,   '产品',   1, '/product',    NULL, NULL, 300, 1),
(301, 300, '原料档案', 2, 'materials',  'product/Materials', NULL, 301, 1),
(302, 300, '配方 BOM', 2, 'formulas',   'product/Formulas',  NULL, 302, 1),
(303, 300, '产品库',   2, 'products',   'product/Products',  NULL, 303, 1),
(304, 300, '商品上架管理', 2, 'listings', 'product/Listings', NULL, 304, 1),
-- BOM/商品 按钮
(3021, 302, '新增配方', 3, NULL, NULL, 'bom:add',     1, 1),
(3022, 302, '编辑配方', 3, NULL, NULL, 'bom:edit',    2, 1),
(3023, 302, '配方启停', 3, NULL, NULL, 'bom:status',  3, 1),
(3024, 302, '毛利试算', 3, NULL, NULL, 'bom:calc',    4, 1),
(3031, 303, '发布商品', 3, NULL, NULL, 'product:publish', 1, 1),
(3032, 303, '调整建议售价', 3, NULL, NULL, 'product:price', 2, 1),
(3033, 303, '下架商品', 3, NULL, NULL, 'product:off', 3, 1),
(3041, 304, '上架商品', 3, NULL, NULL, 'listing:add',  1, 1),
(3042, 304, '调整本店售价', 3, NULL, NULL, 'listing:price', 2, 1),
(3043, 304, '下架',     3, NULL, NULL, 'listing:off',  3, 1),
-- 供应链
(400, 0,   '供应链', 1, '/supply',     NULL, NULL, 400, 1),
(401, 400, '供应商管理', 2, 'suppliers',  'supply/Suppliers', NULL, 401, 1),
(402, 400, '分级价格表', 2, 'tier-prices','supply/TierPrices', NULL, 402, 1),
(403, 400, '采购单',   2, 'purchases',  'supply/Purchases', NULL, 403, 1),
(404, 400, '门店库存', 2, 'inventory',  'supply/Inventory', NULL, 404, 1),
-- 供应链按钮
(4031, 403, '提交采购单', 3, NULL, NULL, 'purchase:submit', 1, 1),
(4032, 403, '确认计价',   3, NULL, NULL, 'purchase:confirm', 2, 1),
(4033, 403, '发货',       3, NULL, NULL, 'purchase:ship',    3, 1),
(4034, 403, '确认入库',   3, NULL, NULL, 'purchase:receive', 4, 1),
(4035, 403, '取消采购单', 3, NULL, NULL, 'purchase:cancel',  5, 1),
(4041, 404, '盘点修正',   3, NULL, NULL, 'inv:adjust', 1, 1),
(4042, 404, '查看流水',   3, NULL, NULL, 'inv:log',    2, 1),
-- 加盟
(500, 0,   '加盟',   1, '/franchise',  NULL, NULL, 500, 1),
(501, 500, '加盟商管理', 2, 'franchisees','franchise/Franchisees', NULL, 501, 1),
(502, 500, '门店管理',   2, 'stores',     'franchise/Stores', NULL, 502, 1),
-- 系统
(600, 0,   '系统',   1, '/system',     NULL, NULL, 600, 1),
(601, 600, '用户与角色', 2, 'users',   'system/Users', NULL, 601, 1),
(602, 600, '操作日志',   2, 'op-logs', 'system/OpLogs', NULL, 602, 1),
-- 门店端
(700, 0,   '门店端', 1, '/store-term', NULL, NULL, 700, 1),
(701, 700, '门店工作台', 2, 'st-dashboard', 'store/Dashboard', NULL, 701, 1),
(702, 700, '接单收银',   2, 'st-orders',    'store/Orders',    NULL, 702, 1),
(703, 700, '收银台',     2, 'st-pos',       'store/Pos',       NULL, 703, 1),
(704, 700, '库存台账',   2, 'st-inventory', 'store/Inventory', NULL, 704, 1),
(705, 700, '日结',       2, 'st-settle',    'store/Settle',    NULL, 705, 1),
-- 门店端按钮
(7021, 702, '接单',     3, NULL, NULL, 'order:accept', 1, 1),
(7022, 702, '标记制作完成', 3, NULL, NULL, 'order:make',   2, 1),
(7023, 702, '核销取餐', 3, NULL, NULL, 'order:finish',  3, 1),
(7024, 702, '退款',     3, NULL, NULL, 'order:refund',  4, 1),
(7025, 702, '取消订单', 3, NULL, NULL, 'order:cancel',  5, 1),
(7031, 703, '手工收银', 3, NULL, NULL, 'pos:create',   1, 1);

-- ---------- 角色（R1-R8，平台预置） ----------
DELETE FROM `sys_role` WHERE `id` BETWEEN 1 AND 8;
INSERT INTO `sys_role` (`id`,`tenant_id`,`role_name`,`role_code`,`data_scope`,`remark`,`status`,`create_time`) VALUES
(1, 0, '平台管理员',   'R1', 1, '平台管理；对租户业务数据只读审计（P0-3）', 1, NOW()),
(2, 0, '总部管理员',   'R2', 2, '品牌内最高权限（R3+R4+R5 集合）',           1, NOW()),
(3, 0, '总部-产品研发', 'R3', 2, '原料/配方/商品发布',                       1, NOW()),
(4, 0, '总部-供应链',   'R4', 2, '供应商/分级价格/采购处理',                 1, NOW()),
(5, 0, '总部-运营',     'R5', 2, '加盟商/营销/报表',                         1, NOW()),
(6, 0, '加盟商老板',   'R6', 3, '名下门店运营；采购发起权（P1-1）',          1, NOW()),
(7, 0, '店长',         'R7', 4, '本店管理；收货入库/盘点/日结',              1, NOW()),
(8, 0, '店员',         'R8', 4, '本店接单/制作/核销/收银（无退款）',         1, NOW());

-- ---------- 角色授权（按 PRD 5.2/5.3 矩阵；INSERT...SELECT） ----------
DELETE FROM `sys_role_menu`;
-- R1 平台管理员：平台菜单全部 + 业务菜单只读（不含按钮，menu_type<3）
INSERT INTO `sys_role_menu` (`id`,`tenant_id`,`role_id`,`menu_id`)
SELECT ROW_NUMBER() OVER (ORDER BY m.id) + 10000, 0, 1, m.id FROM `sys_menu` m WHERE m.`menu_type` < 3;
-- R2 总部管理员：全部菜单 + 全部按钮
INSERT INTO `sys_role_menu` (`id`,`tenant_id`,`role_id`,`menu_id`)
SELECT ROW_NUMBER() OVER (ORDER BY m.id) + 11000, 0, 2, m.id FROM `sys_menu` m WHERE m.`id` >= 200;
-- R3 产品研发：产品目录 + BOM/商品按钮
INSERT INTO `sys_role_menu` (`id`,`tenant_id`,`role_id`,`menu_id`)
SELECT ROW_NUMBER() OVER (ORDER BY m.id) + 12000, 0, 3, m.id FROM `sys_menu` m WHERE m.`id` BETWEEN 300 AND 399;
-- R4 供应链：供应链目录 + 处理按钮（不含 purchase:submit）
INSERT INTO `sys_role_menu` (`id`,`tenant_id`,`role_id`,`menu_id`)
SELECT ROW_NUMBER() OVER (ORDER BY m.id) + 13000, 0, 4, m.id FROM `sys_menu` m
WHERE m.`id` BETWEEN 400 AND 499 AND (m.`menu_type` < 3 OR m.`id` = 4035 OR m.`id` BETWEEN 4032 AND 4033 OR m.`id` BETWEEN 4041 AND 4042);
-- R5 运营：经营/加盟/系统菜单（无按钮）
INSERT INTO `sys_role_menu` (`id`,`tenant_id`,`role_id`,`menu_id`)
SELECT ROW_NUMBER() OVER (ORDER BY m.id) + 14000, 0, 5, m.id FROM `sys_menu` m
WHERE (m.`id` BETWEEN 200 AND 299 OR m.`id` BETWEEN 500 AND 699) AND m.`menu_type` < 3;
-- R6 加盟商：工作台/上架/库存/采购/订单/报表 + listing/inv/purchase:submit 按钮
INSERT INTO `sys_role_menu` (`id`,`tenant_id`,`role_id`,`menu_id`)
SELECT ROW_NUMBER() OVER (ORDER BY m.id) + 15000, 0, 6, m.id FROM `sys_menu` m
WHERE m.`id` IN (201, 204, 202, 304, 403, 404) OR m.`id` IN (3041,3042,3043,4031,4034,4041,4042);
-- R7 店长：门店端全部 + 订单/收银/盘点按钮（无退款→P2 决策：店长有退款，店员无）
INSERT INTO `sys_role_menu` (`id`,`tenant_id`,`role_id`,`menu_id`)
SELECT ROW_NUMBER() OVER (ORDER BY m.id) + 16000, 0, 7, m.id FROM `sys_menu` m
WHERE m.`id` BETWEEN 700 AND 799;
-- R8 店员：门店端（工作台/接单/收银/库存只读；无退款 7024、无日结 705、无盘点 4041）
INSERT INTO `sys_role_menu` (`id`,`tenant_id`,`role_id`,`menu_id`)
SELECT ROW_NUMBER() OVER (ORDER BY m.id) + 17000, 0, 8, m.id FROM `sys_menu` m
WHERE m.`id` IN (701, 702, 703, 704, 7021, 7022, 7023, 7025, 7031);

-- ---------- 平台管理员账号 ----------
-- ⚠️ 密码为 BCrypt 占位：首次部署后用 PasswordUtil.encode("你的密码") 生成的哈希 UPDATE 此行
DELETE FROM `sys_user` WHERE `id` = 1;
INSERT INTO `sys_user` (`id`,`tenant_id`,`username`,`password`,`nickname`,`user_type`,`status`,`create_time`) VALUES
(1, 0, 'admin', '{bcrypt}REPLACE_WITH_BCRYPT_HASH_ON_FIRST_BOOT', '平台管理员', 0, 1, NOW());
DELETE FROM `sys_user_role` WHERE `user_id` = 1;
INSERT INTO `sys_user_role` (`id`,`tenant_id`,`user_id`,`role_id`) VALUES (1, 0, 1, 1);

-- ---------- 演示品牌租户（联调用） ----------
DELETE FROM `tenant` WHERE `id` = 9001;
INSERT INTO `tenant` (`id`,`tenant_code`,`tenant_name`,`contact_name`,`package_code`,`store_limit`,`status`,`create_time`) VALUES
(9001, 'DEMO-001', '山茶制造（演示）', '张总', 'PRO', 20, 1, NOW());

SET FOREIGN_KEY_CHECKS = 1;
