/** 后端统一返回结构（tea-common Result&lt;T&gt;）：code=0 成功，其余为业务错误 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页返回结构（tea-common PageResult&lt;T&gt;：list/total/page/size，接口协议 1.2） */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 登录入参（F-SYS-03；验证码一期可选：为空则后端跳过校验） */
export interface LoginReq {
  username: string
  password: string
  captchaKey?: string
  captchaCode?: string
}

/** 登录返回：tokenName 动态（Sa-Token 配置），前端按它塞请求头 */
export interface LoginVO {
  token: string
  tokenName: string
  userId: number
  tenantId: number
  nickname: string
  userType: number
  roles: string[]
}

/** 图形验证码：image 为 data:image/png;base64 前缀串 */
export interface CaptchaVO {
  key: string
  image: string
}

/** 菜单树节点（menuType：1 目录 / 2 菜单 / 3 按钮） */
export interface MenuNode {
  id: number
  parentId: number
  menuName: string
  menuType: number
  /** 目录为 / 开头绝对路径；菜单为相对路径 */
  path: string | null
  /** views 下相对路径，如 platform/Dashboard -> src/views/platform/Dashboard.vue */
  component: string | null
  /** 按钮权限码，如 bom:add */
  perm: string | null
  sort: number
  children?: MenuNode[]
}

/** /auth/menus 返回载荷：菜单树 + 按钮权限码集合 */
export interface MenusPayload {
  menuTree: MenuNode[]
  permissions: string[]
}
