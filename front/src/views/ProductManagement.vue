<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import {
  ChevronLeft,
  ChevronRight,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X,
} from "lucide-vue-next";
import {
  createProduct,
  deleteProduct,
  listProducts,
  updateProduct,
} from "../api/business";
import "./business.css";

const keyword = ref("");
const searchKeyword = ref("");
const status = ref("");
const searchStatus = ref("");
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const rows = ref([]);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const formError = ref("");
const editingId = ref(null);
const visible = ref(false);
const form = reactive(emptyForm());

function emptyForm() {
  return {
    productCode: "",
    productName: "",
    category: "",
    unit: "件",
    price: 0,
    stock: 0,
    status: "0",
    description: "",
  };
}

function formatMoney(value) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function formatTime(value) {
  return value ? value.replace("T", " ") : "-";
}

async function loadRows() {
  loading.value = true;
  error.value = "";
  try {
    const result = await listProducts({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined,
      status: searchStatus.value || undefined,
    });
    rows.value = result.records || [];
    total.value = result.total || 0;
    if (pageNum.value > 1 && rows.value.length === 0) pageNum.value--;
  } catch (exception) {
    error.value = exception.message || "产品列表加载失败";
    rows.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function searchRows() {
  searchKeyword.value = keyword.value.trim();
  searchStatus.value = status.value;
  if (pageNum.value === 1) loadRows();
  else pageNum.value = 1;
}

function openCreate() {
  editingId.value = null;
  Object.assign(form, emptyForm());
  formError.value = "";
  visible.value = true;
}

function openEdit(item) {
  editingId.value = item.id;
  Object.assign(form, {
    productCode: item.productCode,
    productName: item.productName,
    category: item.category || "",
    unit: item.unit || "件",
    price: item.price,
    stock: item.stock,
    status: item.status,
    description: item.description || "",
  });
  formError.value = "";
  visible.value = true;
}

async function save() {
  formError.value = "";
  if (!form.productCode.trim() || !form.productName.trim()) {
    formError.value = "请填写产品编码和名称";
    return;
  }
  if (
    Number(form.price) < 0 ||
    !Number.isInteger(Number(form.stock)) ||
    Number(form.stock) < 0
  ) {
    formError.value = "价格和库存不能为负数，库存须为整数";
    return;
  }
  saving.value = true;
  try {
    const payload = {
      ...form,
      price: Number(form.price),
      stock: Number(form.stock),
    };
    if (editingId.value) await updateProduct(editingId.value, payload);
    else await createProduct(payload);
    visible.value = false;
    await loadRows();
  } catch (exception) {
    formError.value = exception.message || "保存产品失败";
  } finally {
    saving.value = false;
  }
}

async function remove(item) {
  if (
    !window.confirm(
      `确认删除产品“${item.productName}”吗？已被订单引用的产品只能停用。`,
    )
  )
    return;
  try {
    await deleteProduct(item.id);
    await loadRows();
  } catch (exception) {
    error.value = exception.message || "删除产品失败";
  }
}

watch([pageNum, pageSize], loadRows);
onMounted(loadRows);
</script>

<template>
  <div class="business-page">
    <div class="page-intro">
      <div>
        <h1>产品管理</h1>
        <p>维护产品信息、价格和可用库存</p>
      </div>
      <button class="primary-btn" @click="openCreate">
        <Plus :size="17" />新增产品
      </button>
    </div>
    <section class="panel business-panel">
      <div class="filter-bar business-filter">
        <div class="search-field">
          <Search :size="17" /><input
            v-model="keyword"
            placeholder="请输入产品名称或编码"
            @keyup.enter="searchRows"
          />
        </div>
        <select v-model="status">
          <option value="">全部状态</option>
          <option value="0">上架</option>
          <option value="1">下架</option></select
        ><button class="filter-btn" @click="searchRows">
          <Search :size="16" />查询</button
        ><span class="filter-spacer"></span
        ><button class="plain-icon" title="刷新" @click="loadRows">
          <RefreshCw :size="17" />
        </button>
      </div>
      <div v-if="error" class="api-error">{{ error }}</div>
      <div class="business-table product-table">
        <div class="business-row business-head">
          <span>产品名称 / 编码</span><span>分类</span><span>单价</span
          ><span>库存</span><span>状态</span><span>更新时间</span
          ><span>操作</span>
        </div>
        <div v-if="loading" class="business-empty">正在加载产品...</div>
        <template v-else
          ><div v-for="item in rows" :key="item.id" class="business-row">
            <span class="product-name"
              ><strong>{{ item.productName }}</strong
              ><small>{{ item.productCode }}</small></span
            ><span>{{ item.category || "-" }}</span
            ><span class="money">{{ formatMoney(item.price) }}</span
            ><span :class="{ 'low-stock': item.stock < 10 }"
              >{{ item.stock }} {{ item.unit }}</span
            ><span
              ><i
                class="status-pill"
                :class="item.status === '0' ? 'active' : 'inactive'"
                >{{ item.status === "0" ? "上架" : "下架" }}</i
              ></span
            ><span>{{ formatTime(item.updateTime) }}</span
            ><span class="business-actions"
              ><button title="编辑" @click="openEdit(item)">
                <Pencil :size="15" /></button
              ><button title="删除" @click="remove(item)">
                <Trash2 :size="15" /></button
            ></span>
          </div>
          <div v-if="!rows.length" class="business-empty">
            暂无产品，点击右上角新增产品
          </div></template
        >
      </div>
      <div class="table-footer">
        <span>共 {{ total }} 条数据</span>
        <div class="pagination">
          <button :disabled="pageNum <= 1" @click="pageNum--">
            <ChevronLeft :size="15" /></button
          ><button class="current">{{ pageNum }}</button
          ><button :disabled="pageNum * pageSize >= total" @click="pageNum++">
            <ChevronRight :size="15" />
          </button>
        </div>
        <select v-model.number="pageSize">
          <option :value="10">10 条/页</option>
          <option :value="20">20 条/页</option>
          <option :value="50">50 条/页</option>
        </select>
      </div>
    </section>

    <div v-if="visible" class="modal-mask" @click.self="visible = false">
      <div class="modal-card business-modal">
        <div class="modal-head">
          <h3>{{ editingId ? "编辑产品" : "新增产品" }}</h3>
          <button title="关闭" @click="visible = false">
            <X :size="19" />
          </button>
        </div>
        <form @submit.prevent="save">
          <div class="business-form">
            <label
              >产品编码 <em>*</em
              ><input
                v-model="form.productCode"
                maxlength="64"
                placeholder="例如 PROD-001"
                required /></label
            ><label
              >产品名称 <em>*</em
              ><input
                v-model="form.productName"
                maxlength="120"
                placeholder="请输入产品名称"
                required /></label
            ><label
              >产品分类<input
                v-model="form.category"
                maxlength="64"
                placeholder="例如 办公用品" /></label
            ><label
              >计量单位<input
                v-model="form.unit"
                maxlength="20"
                placeholder="件" /></label
            ><label
              >销售单价 <em>*</em
              ><input
                v-model.number="form.price"
                type="number"
                min="0"
                max="9999999999.99"
                step="0.01"
                required /></label
            ><label
              >当前库存 <em>*</em
              ><input
                v-model.number="form.stock"
                type="number"
                min="0"
                max="100000000"
                step="1"
                required /></label
            ><label
              >状态<select v-model="form.status">
                <option value="0">上架</option>
                <option value="1">下架</option>
              </select></label
            ><label class="full"
              >产品说明<textarea
                v-model="form.description"
                maxlength="500"
                placeholder="选填，最多 500 字"
              ></textarea>
            </label>
          </div>
          <div v-if="formError" class="business-form-error">
            {{ formError }}
          </div>
          <div class="modal-actions">
            <button type="button" class="ghost-btn" @click="visible = false">
              取消</button
            ><button type="submit" class="primary-btn" :disabled="saving">
              {{ saving ? "保存中..." : "保存" }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
