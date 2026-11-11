<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Plus, Search, RefreshCw, Download, Upload, Pencil, Trash2, ChevronLeft, ChevronRight, ShieldCheck, X } from 'lucide-vue-next'
import { resourceData } from '../data/mockData'
import { createResource, deleteResource, exportResource, getRoleMenus, getUserRoles, importResource, listResource, setRoleMenus, setUserRoles, updateResource } from '../api/system'

const props = defineProps({ resource: String, title: String, subtitle: String })
const systemResources = ['users', 'roles', 'menus', 'departments', 'positions']
const isSystem = computed(() => systemResources.includes(props.resource))
const query = ref('')
const status = ref('全部状态')
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const error = ref('')
const rows = ref([])
const records = ref([])
const total = ref(0)
const visible = ref(false)
const authVisible = ref(false)
const authLoading = ref(false)
const authTarget = ref(null)
const authIds = ref([])
const roleOptions = ref([])
const menuOptions = ref([])
const editingId = ref(null)
const importInput = ref(null)

const configs = {
  users: {
    endpoint: 'user', columns: [['nickName', '用户昵称'], ['userName', '账号'], ['phone', '手机号码'], ['deptName', '所属部门'], ['statusText', '状态']],
    fields: [['userName', '账号', 'text', true], ['nickName', '用户昵称', 'text', true], ['password', '初始密码', 'password', false], ['phone', '手机号', 'text', false], ['email', '邮箱', 'text', false], ['deptId', '所属部门', 'dept', false], ['status', '状态', 'status', true], ['remark', '备注', 'textarea', false]],
  },
  roles: {
    endpoint: 'role', columns: [['roleName', '角色名称'], ['roleKey', '角色标识'], ['dataScopeText', '数据权限'], ['statusText', '状态'], ['createTime', '创建时间']],
    fields: [['roleName', '角色名称', 'text', true], ['roleKey', '角色标识', 'text', true], ['roleSort', '显示顺序', 'number', false], ['dataScope', '数据范围', 'scope', true], ['status', '状态', 'status', true], ['remark', '备注', 'textarea', false]],
  },
  menus: {
    endpoint: 'menu', columns: [['menuName', '菜单名称'], ['menuTypeText', '类型'], ['parentName', '上级菜单'], ['path', '路由地址'], ['statusText', '状态']],
    fields: [['menuName', '菜单名称', 'text', true], ['parentId', '上级菜单', 'menu', false], ['orderNum', '显示顺序', 'number', false], ['path', '路由地址', 'text', false], ['component', '组件路径', 'text', false], ['menuType', '菜单类型', 'menuType', true], ['visible', '显示状态', 'visible', true], ['status', '状态', 'status', true], ['perms', '权限标识', 'text', false], ['icon', '图标', 'text', false], ['remark', '备注', 'textarea', false]],
  },
  departments: {
    endpoint: 'dept', columns: [['deptName', '部门名称'], ['leader', '负责人'], ['phone', '联系电话'], ['orderNum', '排序'], ['statusText', '状态']],
    fields: [['deptName', '部门名称', 'text', true], ['parentId', '上级部门', 'dept', false], ['orderNum', '显示顺序', 'number', false], ['leader', '负责人', 'text', false], ['phone', '联系电话', 'text', false], ['email', '邮箱', 'text', false], ['status', '状态', 'status', true]],
  },
  positions: {
    endpoint: 'post', columns: [['postName', '岗位名称'], ['postCode', '岗位编码'], ['postSort', '显示顺序'], ['statusText', '状态'], ['createTime', '创建时间']],
    fields: [['postName', '岗位名称', 'text', true], ['postCode', '岗位编码', 'text', true], ['postSort', '显示顺序', 'number', false], ['status', '状态', 'status', true], ['remark', '备注', 'textarea', false]],
  },
}
const config = computed(() => configs[props.resource])
const form = reactive({})
const deptOptions = ref([])
for (const key of Object.keys(resourceData)) {
  if (!configs[key]) {
    configs[key] = {
      endpoint: key,
      columns: resourceData[key].columns.map((label, index) => [String(index), label]),
      fields: [],
    }
  }
}

function text(value) { return value === null || value === undefined || value === '' ? '-' : value }
function statusText(value) { return value === '0' ? '正常' : value === '1' ? '停用' : text(value) }
function decorate(item) {
  return { ...item, statusText: statusText(item.status), dataScopeText: ({ 1: '全部数据', 2: '自定义数据', 3: '本部门', 4: '本部门及以下', 5: '仅本人' })[item.dataScope] || text(item.dataScope), menuTypeText: ({ M: '目录', C: '菜单', F: '按钮' })[item.menuType] || (item.menuType ? text(item.menuType) : '-'), parentName: menuOptions.value.find((menu) => menu.id === item.parentId)?.menuName || (item.parentId ? `ID:${item.parentId}` : '顶级菜单'), deptName: item.deptName || deptOptions.value.find((dept) => dept.id === item.deptId)?.deptName || (item.deptId ? `ID:${item.deptId}` : '-') }
}
const displayRecords = computed(() => records.value.map(decorate))
function displayRow(item) { return config.value.columns.map(([key]) => text(item[key])) }
async function loadRows() {
  if (!isSystem.value) {
    const source = resourceData[props.resource]?.rows || []
    records.value = source.map((row, index) => ({ id: index, ...Object.fromEntries(row.map((value, column) => [String(column), value])) }))
    rows.value = records.value
    total.value = rows.value.length
    return
  }
  loading.value = true; error.value = ''
  try {
    if (props.resource === 'departments') { const all = await listResource('departments', { pageNum: 1, pageSize: 100 }); deptOptions.value = all.records || [] }
    if (props.resource === 'menus') { const all = await listResource('menus', { pageNum: 1, pageSize: 100 }); menuOptions.value = all.records || [] }
    if (props.resource === 'users') { const depts = await listResource('departments', { pageNum: 1, pageSize: 100 }); deptOptions.value = depts.records || [] }
    const result = await listResource(props.resource, { pageNum: pageNum.value, pageSize: pageSize.value, keyword: query.value || undefined })
    records.value = result.records || []; rows.value = records.value.map(decorate); total.value = result.total || 0
  } catch (e) { error.value = e.message || '接口请求失败'; records.value = []; rows.value = []; total.value = 0 } finally { loading.value = false }
}
watch(() => [props.resource, pageNum.value, pageSize.value], loadRows, { immediate: true })
function resetForm() { Object.keys(form).forEach((key) => delete form[key]); config.value.fields.forEach(([key]) => { form[key] = key === 'status' ? '0' : key === 'dataScope' ? '1' : key === 'menuType' ? 'C' : key === 'visible' ? '0' : '' }); form.parentId = props.resource === 'menus' || props.resource === 'departments' ? 0 : form.parentId }
function openAdd() { if (!isSystem.value) { window.alert('该业务页面暂未接入后端接口'); return } editingId.value = null; resetForm(); visible.value = true }
function openEdit(item) { if (!isSystem.value) { window.alert('该业务页面暂未接入后端接口'); return } editingId.value = item.id; resetForm(); config.value.fields.forEach(([key]) => { form[key] = item[key] ?? form[key] }); visible.value = true }
async function submitForm() { error.value = ''; try { const payload = { ...form }; if (!editingId.value && props.resource === 'users' && !payload.password) payload.password = '123456'; if (editingId.value) { payload.id = editingId.value; await updateResource(props.resource, payload) } else await createResource(props.resource, payload); visible.value = false; await loadRows() } catch (e) { error.value = e.message || '保存失败' } }
async function removeItem(item) { if (!isSystem.value) { window.alert('该业务页面暂未接入后端接口'); return } if (!window.confirm(`确认删除“${item[config.value.columns[0][0]]}”吗？`)) return; try { await deleteResource(props.resource, item.id); await loadRows() } catch (e) { error.value = e.message || '删除失败' } }
async function openAuth(item) { authTarget.value = item; authIds.value = []; authVisible.value = true; authLoading.value = true; try { if (props.resource === 'users') { const [assigned, roles] = await Promise.all([getUserRoles(item.id), listResource('roles', { pageNum: 1, pageSize: 100 })]); authIds.value = assigned || []; roleOptions.value = roles.records || [] } else { const [assigned, menus] = await Promise.all([getRoleMenus(item.id), listResource('menus', { pageNum: 1, pageSize: 100 })]); authIds.value = assigned || []; menuOptions.value = menus.records || [] } } catch (e) { error.value = e.message || '授权数据加载失败' } finally { authLoading.value = false } }
async function saveAuth() { try { if (props.resource === 'users') await setUserRoles(authTarget.value.id, authIds.value); else await setRoleMenus(authTarget.value.id, authIds.value); authVisible.value = false } catch (e) { error.value = e.message || '授权保存失败' } }
function toggleAuth(id) { const index = authIds.value.indexOf(id); if (index >= 0) authIds.value.splice(index, 1); else authIds.value.push(id) }
const authOptions = computed(() => props.resource === 'users' ? roleOptions.value : menuOptions.value)
async function exportRows() {
  if (!isSystem.value) { window.alert('该业务页面暂未接入后端接口'); return }
  try {
    const blob = await exportResource(props.resource)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${title || props.resource}-导出.xlsx`
    link.click()
    URL.revokeObjectURL(url)
  } catch (e) { error.value = e.message || '导出失败' }
}
function openImport() { if (!isSystem.value) { window.alert('该业务页面暂未接入后端接口'); return }; importInput.value?.click() }
async function handleImport(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  try {
    const result = await importResource(props.resource, file)
    await loadRows()
    window.alert(`导入成功，共导入 ${result.count || 0} 条数据`)
  } catch (e) { error.value = e.message || '导入失败' }
}
</script>

<template>
  <div class="resource-page">
    <div class="page-intro"><div><h1>{{ title }}</h1><p>{{ subtitle }}</p></div><button class="primary-btn" @click="openAdd"><Plus :size="17" />新增{{ title.replace('管理', '') }}</button></div>
    <section class="panel resource-panel">
      <div class="filter-bar"><div class="search-field"><Search :size="17" /><input v-model="query" @keyup.enter="pageNum = 1; loadRows()" :placeholder="`请输入${title.replace('管理', '')}名称`" /></div><select v-model="status"><option>全部状态</option><option>正常</option><option>停用</option></select><button class="filter-btn" @click="pageNum = 1; loadRows()"><Search :size="16" />查询</button><span class="filter-spacer"></span><button class="plain-icon" title="刷新" @click="loadRows"><RefreshCw :size="17" /></button><button class="plain-icon" title="导入 Excel" @click="openImport"><Upload :size="17" /></button><button class="plain-icon" title="导出 Excel" @click="exportRows"><Download :size="17" /></button><input ref="importInput" type="file" accept=".xlsx,.xls" hidden @change="handleImport" /></div>
      <div v-if="error" class="api-error">{{ error }}</div>
      <div class="data-table"><div class="data-row data-head"><span v-for="column in config?.columns || [['name','名称']]" :key="column[0]">{{ column[1] }}</span><span>操作</span></div><div v-if="loading" class="empty-state">正在加载数据...</div><template v-else><div v-for="item in displayRecords" :key="item.id" class="data-row"><span v-for="column in config?.columns || []" :key="column[0]" :class="{ 'status-cell': column[0] === 'statusText' && item.status === '0', 'muted-cell': column[0] === 'statusText' && item.status === '1' }"><i v-if="column[0] === 'statusText' && item.status === '0'"></i>{{ text(item[column[0]]) }}</span><span class="row-actions"><button title="编辑" @click="openEdit(item)"><Pencil :size="15" /></button><button v-if="resource === 'users' || resource === 'roles'" title="权限" @click="openAuth(item)"><ShieldCheck :size="15" /></button><button title="删除" @click="removeItem(item)"><Trash2 :size="15" /></button></span></div><div v-if="!displayRecords.length" class="empty-state">暂无数据</div></template></div>
      <div class="table-footer"><span>共 {{ total }} 条数据</span><div class="pagination"><button :disabled="pageNum <= 1" @click="pageNum--"><ChevronLeft :size="15" /></button><button class="current">{{ pageNum }}</button><button :disabled="pageNum * pageSize >= total" @click="pageNum++"><ChevronRight :size="15" /></button></div><select v-model.number="pageSize"><option :value="10">10 条/页</option><option :value="20">20 条/页</option></select></div>
    </section>
    <div v-if="visible" class="modal-mask" @click.self="visible = false"><div class="modal-card resource-modal"><div class="modal-head"><h3>{{ editingId ? '编辑' : '新增' }}{{ title.replace('管理', '') }}</h3><button @click="visible = false"><X :size="19" /></button></div><div class="form-grid dynamic-form"><template v-for="field in config.fields" :key="field[0]"><label :class="{ full: field[2] === 'textarea' }"><span>{{ field[1] }}<em v-if="field[3]">*</em></span><textarea v-if="field[2] === 'textarea'" v-model="form[field[0]]" rows="3"></textarea><select v-else-if="field[2] === 'status'" v-model="form[field[0]]"><option value="0">正常</option><option value="1">停用</option></select><select v-else-if="field[2] === 'scope'" v-model="form[field[0]]"><option value="1">全部数据</option><option value="3">本部门</option><option value="4">本部门及以下</option><option value="5">仅本人</option></select><select v-else-if="field[2] === 'menuType'" v-model="form[field[0]]"><option value="M">目录</option><option value="C">菜单</option><option value="F">按钮</option></select><select v-else-if="field[2] === 'visible'" v-model="form[field[0]]"><option value="0">显示</option><option value="1">隐藏</option></select><select v-else-if="field[2] === 'dept'" v-model.number="form[field[0]]"><option :value="0">顶级部门</option><option v-for="dept in deptOptions" :key="dept.id" :value="dept.id">{{ dept.deptName }}</option></select><select v-else-if="field[2] === 'menu'" v-model.number="form[field[0]]"><option :value="0">顶级菜单</option><option v-for="menu in menuOptions" :key="menu.id" :value="menu.id">{{ menu.menuName }}</option></select><input v-else v-model="form[field[0]]" :type="field[2]" /></label></template><div v-if="error" class="login-error full">{{ error }}</div></div><div class="modal-actions"><button class="ghost-btn" @click="visible = false">取消</button><button class="primary-btn" @click="submitForm">保存</button></div></div></div>
    <div v-if="authVisible" class="modal-mask" @click.self="authVisible = false"><div class="modal-card auth-modal"><div class="modal-head"><h3>{{ resource === 'users' ? '分配角色' : '分配菜单' }} · {{ authTarget?.nickName || authTarget?.roleName }}</h3><button @click="authVisible = false"><X :size="19" /></button></div><div v-if="authLoading" class="empty-state">正在加载授权数据...</div><div v-else class="auth-options"><label v-for="item in authOptions" :key="item.id" class="auth-option"><input type="checkbox" :checked="authIds.includes(item.id)" @change="toggleAuth(item.id)" /><span>{{ item[resource === 'users' ? 'roleName' : 'menuName'] }}</span><small>{{ item[resource === 'users' ? 'roleKey' : 'perms'] }}</small></label></div><div class="modal-actions"><button class="ghost-btn" @click="authVisible = false">取消</button><button class="primary-btn" @click="saveAuth">保存授权</button></div></div></div>
  </div>
</template>
