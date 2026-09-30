package com.teasaas.it;

import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.member.entity.MemMember;
import com.teasaas.member.mapper.MemMemberMapper;
import com.teasaas.order.entity.OrdOrder;
import com.teasaas.order.mapper.OrdOrderMapper;
import com.teasaas.server.TeaSaasApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 资金链路集成测试基类（Review §4-4）
 *
 * <p>环境：本机 Docker tea-mysql(3307)/tea-redis(6379)/minio(9000)，独立测试库 tea_saas_test
 * （每轮测试启动时从 sql/init/01_schema.sql DROP+CREATE 重建，与开发库 tea_saas 完全隔离）。
 * 定时调度经 tea.job.enabled=false 停用（见 application-test.yml）。</p>
 *
 * <p>用例不挂 @Transactional：并发线程各自经服务代理开独立事务提交，
 * 主线程测试事务回滚不了子线程数据，且测试库本就一次性，数据用随机单号/ID 自然隔离。</p>
 */
@SpringBootTest(classes = TeaSaasApplication.class)
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public abstract class IntegrationTestBase {

    /** 测试租户（无外键约束，任意值即可，全链路统一） */
    protected static final Long TENANT_ID = 88001L;

    /** 测试库建库连接（与 01_schema.sql 头部一致：root/root@3307） */
    private static final String INIT_URL =
            "jdbc:mysql://localhost:3307/?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai";

    private static final AtomicLong SEQ = new AtomicLong();

    private static volatile boolean schemaReady = false;

    protected final MemMemberMapper memMemberMapper;
    protected final OrdOrderMapper ordOrderMapper;

    /** 构造器注入（红线5，pre-commit 全仓门禁含测试）：类上 @TestConstructor(ALL) 使 SpringExtension 解析全部构造参数 */
    protected IntegrationTestBase(MemMemberMapper memMemberMapper, OrdOrderMapper ordOrderMapper) {
        this.memMemberMapper = memMemberMapper;
        this.ordOrderMapper = ordOrderMapper;
    }

    /** 每轮测试重建测试库 schema（JVM 内仅一次；DROP+CREATE 保证干净起点） */
    @BeforeAll
    static void rebuildTestSchema() throws Exception {
        synchronized (IntegrationTestBase.class) {
            if (schemaReady) {
                return;
            }
            String sql = Files.readString(locateSchema(), StandardCharsets.UTF_8);
            if (!sql.isEmpty() && sql.charAt(0) == '\uFEFF') {
                sql = sql.substring(1); // 去 BOM
            }
            sql = sql.replace("`tea_saas`", "`tea_saas_test`");
            try (Connection conn = DriverManager.getConnection(INIT_URL, "root", "root")) {
                ScriptUtils.executeSqlScript(conn, new EncodedResource(
                        new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            }
            schemaReady = true;
        }
    }

    private static Path locateSchema() {
        List<String> candidates = List.of(
                "sql/init/01_schema.sql",              // CWD=backend-api
                "../sql/init/01_schema.sql",           // CWD=tea-server（mvn test 默认）
                "backend-api/sql/init/01_schema.sql"); // CWD=tea-saas 仓库根
        for (String candidate : candidates) {
            Path path = Path.of(candidate);
            if (Files.exists(path)) {
                return path;
            }
        }
        throw new IllegalStateException("找不到 01_schema.sql，工作目录=" + System.getProperty("user.dir"));
    }

    @BeforeEach
    void bindTenant() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void unbindTenant() {
        TenantContext.clear();
    }

    // ==================== 并发工具 ====================

    /** 并发动作（index=线程编号，便于构造互异单号） */
    @FunctionalInterface
    protected interface ConcurrentAction {
        Object run(int index) throws Exception;
    }

    /**
     * 并发执行：CyclicBarrier 对齐起跑线最大化竞争窗口，每线程独立注入/清理租户上下文。
     * 返回逐线程出口——正常返回值或捕获的异常（Throwable），由用例按"恰好一次生效"口径断言。
     */
    protected static List<Object> runConcurrently(int threads, ConcurrentAction action) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CyclicBarrier barrier = new CyclicBarrier(threads);
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int index = i;
                futures.add(pool.submit(() -> {
                    barrier.await(30, TimeUnit.SECONDS);
                    TenantContext.setTenantId(TENANT_ID);
                    try {
                        return action.run(index);
                    } catch (Throwable t) {
                        return t;
                    } finally {
                        TenantContext.clear();
                    }
                }));
            }
            List<Object> outcomes = new ArrayList<>(threads);
            for (Future<Object> future : futures) {
                outcomes.add(future.get(60, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    protected static long countOk(List<Object> outcomes) {
        return outcomes.stream().filter(o -> !(o instanceof Throwable)).count();
    }

    protected static long countBiz(List<Object> outcomes, ErrorCode code) {
        return outcomes.stream()
                .filter(o -> o instanceof BizException biz && biz.getCode().equals(code.getCode()))
                .count();
    }

    // ==================== 数据工厂（无外键，随机 ID 天然隔离） ====================

    protected static long nextId() {
        return 9_000_000_000L + SEQ.incrementAndGet();
    }

    /** 12 位短随机串：biz_no/order_no 列多为 VARCHAR(32)，全量 UUID 会超长截断 */
    protected static String uid() {
        return UUID.randomUUID().toString().substring(0, 12);
    }

    protected MemMember insertMember() {
        MemMember member = new MemMember();
        member.setTenantId(TENANT_ID);
        member.setOpenid("it-" + UUID.randomUUID());
        member.setNickname("IT会员");
        member.setStatus(1);
        member.setTotalPoints(0);
        member.setCreateTime(LocalDateTime.now());
        memMemberMapper.insert(member);
        return member;
    }

    protected OrdOrder insertOrder(Long memberId, int orderSource, int status, long payAmount) {
        OrdOrder order = new OrdOrder();
        order.setTenantId(TENANT_ID);
        order.setOrderNo("IT" + Long.toHexString(System.nanoTime())
                + UUID.randomUUID().toString().substring(0, 4));
        order.setStoreId(nextId());
        order.setMemberId(memberId);
        order.setOrderSource(orderSource);
        order.setStatus(status);
        order.setTotalAmount(payAmount);
        order.setPayAmount(payAmount);
        ordOrderMapper.insert(order);
        return order;
    }

}
