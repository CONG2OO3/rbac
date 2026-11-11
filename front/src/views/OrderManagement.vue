<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import {
  ChevronLeft,
  ChevronRight,
  Eye,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X,
} from "lucide-vue-next";
import {
  changeOrderStatus,
  createOrder,
  deleteOrder,
  getOrder,
  listOrders,
  productOptions,
  updateOrder,
} from "../api/business";
import "./business.css";

const statusNames = {
  NEW: "待处理",
  CONFIRMED: "已确认",
  SHIPPED: "已发货",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
};
const nextStatus = {
  NEW: [
    { value: "CONFIRMED", label: "确认" },
    { value: "CANCELLED", label: "取消订单" },
  ],
  CONFIRMED: [
    { value: "SHIPPED", label: "发货" },
    { value: "CANCELLED", label: "取消订单" },
  ],
  SHIPPED: [{ value: "COMPLETED", label: "完成" }],
};
const keyword = ref("");
const searchKeyword = ref("");
const status = ref("");
const searchStatus = ref("");
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const rows = ref([]);
const options = ref([]);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const formError = ref("");
const visible = ref(false);
const detail = ref(null);
const editingId = ref(null);
const form = reactive({
  customerName: "",
  customerPhone: "",
  remark: "",
  items: [],
});
const estimatedTotal = computed(() =>
  form.items.reduce((sum, item) => {
    const product = options.value.find(
      (option) => Number(option.id) === Number(item.productId),
    );
    return (
      sum +
      Number(product?.price ?? item.unitPrice ?? 0) * Number(item.quantity || 0)
    );
  }, 0),
);

function formatMoney(value) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function formatTime(value) {
  return value ? value.replace("T", " ") : "-";
}

function productOf(id) {
  return options.value.find((product) => Number(product.id) === Number(id));
}

async function loadRows() {
  loading.value = true;
  error.value = "";
  try {
    const result = await listOrders({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined,
      status: searchStatus.value || undefined,
    });
    rows.value = result.records || [];
    total.value = result.total || 0;
    if (pageNum.value > 1 && rows.value.length === 0) pageNum.value--;
  } catch (exception) {
    error.value = exception.message || "订单列表加载失败";
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

async function openCreate() {
  error.value = "";
  try {
    options.value = await productOptions();
    Object.assign(form, {
      customerName: "",
      customerPhone: "",
      remark: "",
      items: [],
    });
    editingId.value = null;
    formError.value = "";
    visible.value = true;
  } catch (exception) {
    error.value = exception.message || "产品列表加载失败";
  }
}

async function openEdit(item) {
  error.value = "";
  try {
    const [result, availableProducts] = await Promise.all([
      getOrder(item.id),
      productOptions(),
    ]);
    options.value = [...availableProducts];
    for (const line of result.items) {
      if (!productOf(line.productId)) {
        options.value.push({
          id: line.productId,
          productName: `${line.productName}（已下架）`,
          price: line.unitPrice,
          stock: 0,
          status: "1",
        });
      }
    }
    Object.assign(form, {
      customerName: result.order.customerName,
      customerPhone: result.order.customerPhone || "",
      remark: result.order.remark || "",
      items: result.items.map((line) => ({
        productId: line.productId,
        quantity: line.quantity,
        unitPrice: line.unitPrice,
      })),
    });
    editingId.value = item.id;
    formError.value = "";
    visible.value = true;
  } catch (exception) {
    error.value = exception.message || "订单加载失败";
  }
}

async function showDetail(item) {
  error.value = "";
  try {
    detail.value = await getOrder(item.id);
  } catch (exception) {
    error.value = exception.message || "订单详情加载失败";
  }
}

function addLine() {
  const selected = new Set(form.items.map((item) => Number(item.productId)));
  const first = options.value.find(
    (product) => product.status === "0" && !selected.has(Number(product.id)),
  );
  if (!first) {
    formError.value = "没有更多可选产品，请先添加或上架产品";
    return;
  }
  formError.value = "";
  form.items.push({ productId: first.id, quantity: 1 });
}

function removeLine(index) {
  form.items.splice(index, 1);
}

async function save() {
  formError.value = "";
  if (!form.customerName.trim() || !form.items.length) {
    formError.value = "请填写客户名称，并至少添加一个产品";
    return;
  }
  const ids = form.items.map((item) => Number(item.productId));
  if (
    new Set(ids).size !== ids.length ||
    form.items.some(
      (item) =>
        !item.productId ||
        !Number.isInteger(Number(item.quantity)) ||
        Number(item.quantity) < 1,
    )
  ) {
    formError.value = "产品不能重复，数量需为正整数";
    return;
  }
  saving.value = true;
  try {
    const payload = {
      customerName: form.customerName.trim(),
      customerPhone: form.customerPhone.trim(),
      remark: form.remark.trim(),
      items: form.items.map((item) => ({
        productId: Number(item.productId),
        quantity: Number(item.quantity),
      })),
    };
    if (editingId.value) await updateOrder(editingId.value, payload);
    else await createOrder(payload);
    visible.value = false;
    await loadRows();
  } catch (exception) {
    formError.value = exception.message || "保存订单失败";
  } finally {
    saving.value = false;
  }
}

async function advance(item, action) {
  const warning = action.value === "CANCELLED" ? "取消后将返还产品库存。" : "";
  if (
    !window.confirm(
      `确认将订单“${item.orderNo}”设为${statusNames[action.value]}吗？${warning}`,
    )
  )
    return;
  try {
    await changeOrderStatus(item.id, action.value);
    await loadRows();
  } catch (exception) {
    error.value = exception.message || "订单状态更新失败";
  }
}

async function remove(item) {
  if (
    !window.confirm(`确认删除订单“${item.orderNo}”吗？待处理订单会返还库存。`)
  )
    return;
  try {
    await deleteOrder(item.id);
    await loadRows();
  } catch (exception) {
    error.value = exception.message || "删除订单失败";
  }
}

watch([pageNum, pageSize], loadRows);
onMounted(loadRows);
</script>

<template>
  <div class="business-page">
    <div class="page-intro">
      <div>
        <h1>订单管理</h1>
        <p>管理客户订单、产品明细及订单处理流程</p>
      </div>
      <button class="primary-btn" @click="openCreate">
        <Plus :size="17" />新增订单
      </button>
    </div>
    <section class="panel business-panel">
      <div class="filter-bar business-filter">
        <div class="search-field">
          <Search :size="17" /><input
            v-model="keyword"
            placeholder="请输入订单编号或客户名称"
            @keyup.enter="searchRows"
          />
        </div>
        <select v-model="status">
          <option value="">全部状态</option>
          <option
            v-for="(label, value) in statusNames"
            :key="value"
            :value="value"
          >
            {{ label }}
          </option></select
        ><button class="filter-btn" @click="searchRows">
          <Search :size="16" />查询</button
        ><span class="filter-spacer"></span
        ><button class="plain-icon" title="刷新" @click="loadRows">
          <RefreshCw :size="17" />
        </button>
      </div>
      <div v-if="error" class="api-error">{{ error }}</div>
      <div class="business-table order-table">
        <div class="business-row business-head">
          <span>订单编号</span><span>客户名称</span><span>产品数量</span
          ><span>订单金额</span><span>状态</span><span>创建时间</span
          ><span>操作</span>
        </div>
        <div v-if="loading" class="business-empty">正在加载订单...</div>
        <template v-else
          ><div v-for="item in rows" :key="item.id" class="business-row">
            <span
              ><button class="order-link" @click="showDetail(item)">
                {{ item.orderNo }}
              </button></span
            ><span>{{ item.customerName }}</span
            ><span>{{ item.totalQuantity }} 件</span
            ><span class="money">{{ formatMoney(item.totalAmount) }}</span
            ><span
              ><i class="status-pill" :class="item.status.toLowerCase()">{{
                statusNames[item.status] || item.status
              }}</i></span
            ><span>{{ formatTime(item.createTime) }}</span
            ><span class="business-actions"
              ><button title="查看详情" @click="showDetail(item)">
                <Eye :size="15" /></button
              ><button
                v-if="item.status === 'NEW'"
                title="编辑"
                @click="openEdit(item)"
              >
                <Pencil :size="15" /></button
              ><button
                v-if="['NEW', 'CANCELLED'].includes(item.status)"
                title="删除"
                @click="remove(item)"
              >
                <Trash2 :size="15" /></button
              ><button
                v-for="action in nextStatus[item.status] || []"
                :key="action.value"
                class="status-action"
                :class="{ danger: action.value === 'CANCELLED' }"
                @click="advance(item, action)"
              >
                {{ action.label }}
              </button></span
            >
          </div>
          <div v-if="!rows.length" class="business-empty">
            暂无订单，点击右上角新增订单
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
      <div class="modal-card business-modal order-modal">
        <div class="modal-head">
          <h3>{{ editingId ? "编辑订单" : "新增订单" }}</h3>
          <button title="关闭" @click="visible = false">
            <X :size="19" />
          </button>
        </div>
        <form @submit.prevent="save">
          <div class="order-form-scroll">
            <div class="business-form">
              <label
                >客户名称 <em>*</em
                ><input
                  v-model="form.customerName"
                  maxlength="80"
                  placeholder="请输入客户名称"
                  required /></label
              ><label
                >联系电话<input
                  v-model="form.customerPhone"
                  maxlength="30"
                  placeholder="请输入联系电话" /></label
              ><label class="full"
                >备注<textarea
                  v-model="form.remark"
                  maxlength="500"
                  placeholder="选填"
                ></textarea>
              </label>
            </div>
            <div class="order-lines-head">
              <strong>订单产品</strong
              ><button type="button" @click="addLine">
                <Plus :size="15" />添加产品
              </button>
            </div>
            <div v-if="!options.length" class="order-no-products">
              暂无上架产品，请先到产品管理新增产品
            </div>
            <div v-else-if="!form.items.length" class="order-no-products">
              点击“添加产品”选择订单产品
            </div>
            <div
              v-for="(line, index) in form.items"
              :key="index"
              class="order-line"
            >
              <select v-model.number="line.productId">
                <option
                  v-for="product in options"
                  :key="product.id"
                  :value="product.id"
                  :disabled="
                    product.status !== '0' &&
                    Number(product.id) !== Number(line.productId)
                  "
                >
                  {{ product.productName }}（{{
                    product.productCode || "已下架"
                  }}）
                </option></select
              ><input
                v-model.number="line.quantity"
                type="number"
                min="1"
                max="10000"
                step="1"
                aria-label="数量"
              /><span>{{
                formatMoney(
                  Number(
                    productOf(line.productId)?.price ?? line.unitPrice ?? 0,
                  ) * Number(line.quantity || 0),
                )
              }}</span
              ><button
                type="button"
                title="移除产品"
                @click="removeLine(index)"
              >
                <Trash2 :size="16" />
              </button>
            </div>
            <div class="order-total">
              预计合计 <strong>{{ formatMoney(estimatedTotal) }}</strong
              ><small>最终金额以提交时产品价格为准</small>
            </div>
          </div>
          <div v-if="formError" class="business-form-error">
            {{ formError }}
          </div>
          <div class="modal-actions">
            <button type="button" class="ghost-btn" @click="visible = false">
              取消</button
            ><button type="submit" class="primary-btn" :disabled="saving">
              {{ saving ? "保存中..." : "保存订单" }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="detail" class="modal-mask" @click.self="detail = null">
      <div class="modal-card business-modal order-modal">
        <div class="modal-head">
          <h3>订单详情</h3>
          <button title="关闭" @click="detail = null"><X :size="19" /></button>
        </div>
        <div class="order-detail">
          <div class="order-detail-grid">
            <div>
              <span>订单编号</span><strong>{{ detail.order.orderNo }}</strong>
            </div>
            <div>
              <span>订单状态</span
              ><i
                class="status-pill"
                :class="detail.order.status.toLowerCase()"
                >{{ statusNames[detail.order.status] }}</i
              >
            </div>
            <div>
              <span>客户名称</span
              ><strong>{{ detail.order.customerName }}</strong>
            </div>
            <div>
              <span>联系电话</span
              ><strong>{{ detail.order.customerPhone || "-" }}</strong>
            </div>
            <div>
              <span>下单时间</span
              ><strong>{{ formatTime(detail.order.createTime) }}</strong>
            </div>
            <div>
              <span>备注</span><strong>{{ detail.order.remark || "-" }}</strong>
            </div>
          </div>
          <h4>产品明细</h4>
          <div class="order-detail-row detail-head">
            <span>产品</span><span>单价</span><span>数量</span><span>小计</span>
          </div>
          <div
            v-for="line in detail.items"
            :key="line.id"
            class="order-detail-row"
          >
            <span
              >{{ line.productName }}
              <small>{{ line.productCode }}</small></span
            ><span>{{ formatMoney(line.unitPrice) }}</span
            ><span>{{ line.quantity }} {{ line.unit }}</span
            ><span>{{ formatMoney(line.lineAmount) }}</span>
          </div>
          <div class="order-detail-total">
            合计 {{ detail.order.totalQuantity }} 件　<strong>{{
              formatMoney(detail.order.totalAmount)
            }}</strong>
          </div>
        </div>
        <div class="modal-actions">
          <button class="ghost-btn" @click="detail = null">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>
