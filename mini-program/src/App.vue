<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app';
import { useMemberStore } from '@/stores/member';

// 游客可浏览门店/商品（公开接口）；进入即静默登录（M5 R3 会话化口径，preview/下单需登录态）
onLaunch(() => {
  const member = useMemberStore();
  member.silentLogin().catch(() => undefined);
});
</script>

<style lang="scss">
/* ===== 暖茶调 token（复刻 prototype/customer-app.html :root）===== */
page {
  --cream: #faf6ee;
  --paper: #ffffff;
  --ink: #3a2b1e;
  --ink2: #8a7a66;
  --amber: #d97b29;
  --amber-deep: #b85f1c;
  --amber-soft: #f8eddd;
  --green: #5e7c4c;
  --green-soft: #e9f0e3;
  --red: #c2542f;
  --line: #efe6d6;
  --radius: 16px;

  background: var(--cream);
  color: var(--ink);
  font-size: 14px;
  font-family: 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
}

/* ===== 原型通用类（全局）===== */
.cell {
  background: var(--paper);
  margin: 12px 14px 0;
  border-radius: var(--radius);
  padding: 14px 16px;
  border: 1px solid var(--line);
}
.cell .row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  padding: 6px 0;
}
.cell .row .l {
  color: var(--ink2);
}
.cell .row .r {
  font-weight: 600;
}
.cell .row .r.amber {
  color: var(--amber-deep);
  font-weight: 700;
}
.cell-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
  display: flex;
  justify-content: space-between;
}
.cell-title .ct-sub {
  color: var(--ink2);
  font-weight: 400;
  font-size: 12px;
}
.btn {
  background: var(--amber);
  color: #fff;
  border: none;
  border-radius: 99px;
  padding: 13px 26px;
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
}
.btn.gray {
  background: #d8cfc0;
  color: #fff;
}
.btn[disabled],
.btn.disabled {
  opacity: 0.45;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  padding: 7px 14px;
  border-radius: 99px;
  border: 1px solid var(--line);
  background: var(--paper);
  font-size: 13px;
}
.chip.on {
  border-color: var(--amber);
  background: var(--amber-soft);
  color: var(--amber-deep);
  font-weight: 600;
}
.empty {
  text-align: center;
  color: var(--ink2);
  padding: 40px 0;
  font-size: 14px;
}
.sec-title {
  padding: 14px 18px 8px;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
