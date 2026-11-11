<script setup>
import { computed, ref, watch } from 'vue'
import {
  ChevronLeft,
  ChevronRight,
  Download,
  Eye,
  FileText,
  Image as ImageIcon,
  LayoutGrid,
  List,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  UploadCloud,
  Video,
  X,
} from 'lucide-vue-next'
import VueOfficeDocx from '@vue-office/docx'
import VueOfficeExcel from '@vue-office/excel'
import VueOfficePdf from '@vue-office/pdf'
import '@vue-office/docx/lib/index.css'
import '@vue-office/excel/lib/index.css'
import { deleteFile, fetchFileContent, listFiles, uploadFile } from '../api/file'

const props = defineProps({
  fileType: {
    type: String,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    required: true,
  },
})

const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(props.fileType === 'image' ? 12 : 10)
const total = ref(0)
const rows = ref([])
const loading = ref(false)
const uploading = ref(false)
const error = ref('')
const fileInput = ref(null)
const preview = ref(null)
const previewKind = ref('')
const previewSource = ref(null)
const previewText = ref('')
const previewLoading = ref(false)
const previewError = ref('')
const dragActive = ref(false)
const viewMode = ref(localStorage.getItem('image_view_mode') || 'grid')
const uploadProgress = ref({ current: 0, total: 0 })
const videoError = ref('')

const accept = computed(() => {
  if (props.fileType === 'image') return 'image/*'
  if (props.fileType === 'video') return 'video/*'
  return '*/*'
})
const resourceName = computed(() => ({
  image: '图片',
  video: '视频',
  attachment: '附件',
})[props.fileType] || '文件')

async function loadRows() {
  loading.value = true
  error.value = ''
  try {
    const result = await listFiles({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      fileType: props.fileType,
      keyword: keyword.value || undefined,
    })
    rows.value = result.records || []
    total.value = result.total || 0
  } catch (exception) {
    error.value = exception.message || '文件列表加载失败'
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function searchFiles() {
  pageNum.value = 1
  loadRows()
}

async function handleUpload(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  await uploadFiles(files)
}

async function handleDrop(event) {
  dragActive.value = false
  await uploadFiles(Array.from(event.dataTransfer?.files || []))
}

async function uploadFiles(files) {
  if (!files.length) return
  if (props.fileType === 'image' && files.length > 9) {
    error.value = '单次最多上传 9 张图片，请重新选择'
    return
  }
  uploading.value = true
  error.value = ''
  uploadProgress.value = { current: 0, total: files.length }
  try {
    for (const file of files) {
      await uploadFile(props.fileType, file)
      uploadProgress.value.current++
    }
    pageNum.value = 1
    await loadRows()
  } catch (exception) {
    const message = exception.message || '文件上传失败'
    if (uploadProgress.value.current > 0) await loadRows()
    error.value = message
  } finally {
    uploading.value = false
    uploadProgress.value = { current: 0, total: 0 }
  }
}

function setViewMode(mode) {
  viewMode.value = mode
  localStorage.setItem('image_view_mode', mode)
}

async function removeFile(item) {
  const warning = item.storageType === 'starimg'
    ? '这只会删除系统中的记录，星云图上的图片不会被删除。'
    : ''
  if (!window.confirm(`确认删除“${item.originalName}”吗？${warning}`)) return
  try {
    await deleteFile(item.id)
    await loadRows()
  } catch (exception) {
    error.value = exception.message || '文件删除失败'
  }
}

async function downloadFile(item) {
  try {
    const blob = await fetchFileContent(item.id, true)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = item.originalName
    link.click()
    URL.revokeObjectURL(url)
  } catch (exception) {
    error.value = exception.message || '文件下载失败'
  }
}

function formatSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function storageName(type) {
  return ({ aliyun: '阿里云 OSS', minio: 'MinIO', starimg: '星云图' })[type] || type
}

function formatTime(value) {
  return value ? value.replace('T', ' ') : '-'
}

function fileExtension(item) {
  const name = item?.originalName || ''
  return name.includes('.') ? name.split('.').pop().toLowerCase() : ''
}

async function openAttachmentPreview(item) {
  preview.value = item
  previewSource.value = null
  previewText.value = ''
  previewError.value = ''
  previewLoading.value = true
  const extension = fileExtension(item)
  const textExtensions = ['txt', 'md', 'json', 'xml', 'log', 'js', 'ts', 'css', 'html', 'java', 'yml', 'yaml']
  if (extension === 'pdf') previewKind.value = 'pdf'
  else if (extension === 'docx') previewKind.value = 'docx'
  else if (['xls', 'xlsx', 'xlsm', 'csv'].includes(extension)) previewKind.value = 'excel'
  else if (textExtensions.includes(extension)) previewKind.value = 'text'
  else previewKind.value = 'unsupported'

  if (previewKind.value === 'unsupported') {
    previewError.value = extension === 'doc'
      ? '旧版 DOC 文件暂不支持浏览器直接解析，请转换为 DOCX 后预览，或下载到本地查看。'
      : '该文件格式暂不支持在线预览，请下载到本地查看。'
    previewLoading.value = false
    return
  }

  try {
    const blob = await fetchFileContent(item.id, false)
    if (previewKind.value === 'text') previewText.value = await blob.text()
    else previewSource.value = await blob.arrayBuffer()
  } catch (exception) {
    previewError.value = exception.message || '文件预览加载失败'
  } finally {
    previewLoading.value = false
  }
}

function openFilePreview(item) {
  if (props.fileType === 'video') {
    videoError.value = ''
    preview.value = item
    return
  }
  openAttachmentPreview(item)
}

function previewRendered() {
  previewLoading.value = false
}

function previewFailed() {
  previewLoading.value = false
  previewError.value = '文档解析失败，请尝试下载到本地查看。'
}

function closePreview() {
  preview.value = null
  videoError.value = ''
  previewSource.value = null
  previewText.value = ''
  previewError.value = ''
}

watch(
  () => [props.fileType, pageNum.value, pageSize.value],
  () => {
    preview.value = null
    loadRows()
  },
  { immediate: true },
)
</script>

<template>
  <div class="file-page">
    <div class="page-intro">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </div>
      <button class="primary-btn" :disabled="uploading" @click="fileInput?.click()">
        <Plus :size="17" />{{ uploading ? `上传中 ${uploadProgress.current}/${uploadProgress.total}` : `上传${resourceName}` }}
      </button>
      <input
        ref="fileInput"
        type="file"
        :accept="accept"
        multiple
        hidden
        @change="handleUpload"
      />
    </div>

    <section class="panel file-panel">
      <div class="filter-bar">
        <div class="search-field">
          <Search :size="17" />
          <input
            v-model="keyword"
            placeholder="请输入文件名称"
            @keyup.enter="searchFiles"
          />
        </div>
        <button class="filter-btn" @click="searchFiles">
          <Search :size="16" />查询
        </button>
        <span class="filter-spacer"></span>
        <div v-if="fileType === 'image'" class="view-switch">
          <button
            title="图片宫格展示"
            :class="{ active: viewMode === 'grid' }"
            @click="setViewMode('grid')"
          ><LayoutGrid :size="17" /></button>
          <button
            title="列表展示"
            :class="{ active: viewMode === 'list' }"
            @click="setViewMode('list')"
          ><List :size="18" /></button>
        </div>
        <button class="plain-icon" title="刷新" @click="loadRows">
          <RefreshCw :size="17" />
        </button>
      </div>

      <div v-if="error" class="api-error">{{ error }}</div>
      <div v-if="loading" class="empty-state">正在加载文件...</div>

      <template v-else-if="fileType === 'image'">
        <div v-if="rows.length && viewMode === 'grid'" class="image-grid">
          <article v-for="item in rows" :key="item.id" class="image-card">
            <button class="image-preview" @click="preview = item">
              <img :src="item.accessUrl" :alt="item.originalName" />
            </button>
            <div class="image-info">
              <div>
                <strong :title="item.originalName">{{ item.originalName }}</strong>
                <span>{{ formatSize(item.fileSize) }} · {{ storageName(item.storageType) }}</span>
              </div>
              <div class="file-actions">
                <button title="预览" @click="preview = item"><Eye :size="15" /></button>
                <button title="下载" @click="downloadFile(item)"><Download :size="15" /></button>
                <button title="删除" @click="removeFile(item)"><Trash2 :size="15" /></button>
              </div>
            </div>
          </article>
        </div>
        <div v-else-if="rows.length" class="attachment-table image-list-table">
          <div class="attachment-row image-list-row attachment-head">
            <span>图片</span><span>图片名称</span><span>文件大小</span>
            <span>存储方式</span><span>上传人</span><span>上传时间</span><span>操作</span>
          </div>
          <div v-for="item in rows" :key="item.id" class="attachment-row image-list-row">
            <span><button class="list-thumbnail" @click="preview = item"><img :src="item.accessUrl" :alt="item.originalName" /></button></span>
            <span class="list-image-name" :title="item.originalName">{{ item.originalName }}</span>
            <span>{{ formatSize(item.fileSize) }}</span>
            <span class="storage-badge">{{ storageName(item.storageType) }}</span>
            <span>{{ item.uploader }}</span>
            <span>{{ formatTime(item.createTime) }}</span>
            <span class="file-actions">
              <button title="预览" @click="preview = item"><Eye :size="15" /></button>
              <button title="下载" @click="downloadFile(item)"><Download :size="15" /></button>
              <button title="删除" @click="removeFile(item)"><Trash2 :size="15" /></button>
            </span>
          </div>
        </div>
        <div
          v-else
          class="file-empty"
          :class="{ dragging: dragActive }"
          @dragover.prevent="dragActive = true"
          @dragleave.prevent="dragActive = false"
          @drop.prevent="handleDrop"
        >
          <div class="empty-visual image-visual">
            <span class="visual-dot dot-one"></span>
            <span class="visual-dot dot-two"></span>
            <ImageIcon :size="43" />
          </div>
          <h3>还没有图片素材</h3>
          <p>将图片拖拽到这里，或点击下方按钮上传</p>
          <button class="empty-upload-btn" :disabled="uploading" @click="fileInput?.click()">
            <UploadCloud :size="17" />{{ uploading ? '上传中...' : '选择图片' }}
          </button>
          <div class="empty-meta">
            <span>支持 JPG、PNG、GIF、WebP</span><i></i><span>单次最多 9 张</span><i></i><span>单文件不超过 50MB</span>
          </div>
        </div>
      </template>

      <template v-else>
        <div v-if="rows.length" class="attachment-table">
          <div class="attachment-row attachment-head">
            <span>文件名称</span><span>文件大小</span><span>存储方式</span>
            <span>上传人</span><span>上传时间</span><span>操作</span>
          </div>
          <div v-for="item in rows" :key="item.id" class="attachment-row">
            <span class="attachment-name">
              <Video v-if="fileType === 'video'" :size="18" />
              <FileText v-else :size="18" />
              {{ item.originalName }}
            </span>
            <span>{{ formatSize(item.fileSize) }}</span>
            <span class="storage-badge">{{ storageName(item.storageType) }}</span>
            <span>{{ item.uploader }}</span>
            <span>{{ formatTime(item.createTime) }}</span>
            <span class="file-actions">
              <button title="在线预览" @click="openFilePreview(item)"><Eye :size="15" /></button>
              <button title="下载" @click="downloadFile(item)"><Download :size="15" /></button>
              <button title="删除" @click="removeFile(item)"><Trash2 :size="15" /></button>
            </span>
          </div>
        </div>
        <div
          v-else
          class="file-empty"
          :class="{ dragging: dragActive }"
          @dragover.prevent="dragActive = true"
          @dragleave.prevent="dragActive = false"
          @drop.prevent="handleDrop"
        >
          <div class="empty-visual" :class="fileType === 'video' ? 'video-visual' : 'attachment-visual'">
            <span class="visual-dot dot-one"></span>
            <span class="visual-dot dot-two"></span>
            <Video v-if="fileType === 'video'" :size="43" />
            <UploadCloud v-else :size="43" />
          </div>
          <h3>{{ fileType === 'video' ? '还没有视频素材' : '还没有上传附件' }}</h3>
          <p>将{{ resourceName }}拖拽到这里，或点击下方按钮上传</p>
          <button class="empty-upload-btn" :disabled="uploading" @click="fileInput?.click()">
            <Plus :size="17" />{{ uploading ? '上传中...' : `选择${resourceName}` }}
          </button>
          <div v-if="fileType === 'video'" class="empty-meta">
            <span>支持 MP4、WebM、OGG、MOV 等视频格式</span><i></i><span>单文件不超过 500MB</span>
          </div>
          <div v-else class="empty-meta">
            <span>支持文档、压缩包等常用格式</span><i></i><span>单文件不超过 50MB</span>
          </div>
        </div>
      </template>

      <div class="table-footer">
        <span>共 {{ total }} 条数据</span>
        <div class="pagination">
          <button :disabled="pageNum <= 1" @click="pageNum--"><ChevronLeft :size="15" /></button>
          <button class="current">{{ pageNum }}</button>
          <button :disabled="pageNum * pageSize >= total" @click="pageNum++"><ChevronRight :size="15" /></button>
        </div>
        <select v-model.number="pageSize">
          <template v-if="fileType === 'image'">
            <option :value="12">12 条/页</option>
            <option :value="24">24 条/页</option>
            <option :value="48">48 条/页</option>
          </template>
          <template v-else>
            <option :value="10">10 条/页</option>
            <option :value="20">20 条/页</option>
            <option :value="50">50 条/页</option>
          </template>
        </select>
      </div>
    </section>

    <div v-if="preview && fileType === 'image'" class="modal-mask" @click.self="closePreview">
      <div class="image-preview-modal">
        <button class="preview-close" @click="closePreview"><X :size="20" /></button>
        <img :src="preview.accessUrl" :alt="preview.originalName" />
        <div>
          <strong>{{ preview.originalName }}</strong>
          <button @click="downloadFile(preview)"><Download :size="16" />下载原图</button>
        </div>
      </div>
    </div>

    <div v-if="preview && fileType === 'attachment'" class="modal-mask document-mask" @click.self="closePreview">
      <div class="document-preview-modal">
        <header class="document-preview-head">
          <div class="document-file-icon"><FileText :size="20" /></div>
          <div>
            <strong>{{ preview.originalName }}</strong>
            <span>{{ formatSize(preview.fileSize) }} · {{ fileExtension(preview).toUpperCase() || 'FILE' }}</span>
          </div>
          <button class="document-download" @click="downloadFile(preview)"><Download :size="16" />下载</button>
          <button class="document-close" @click="closePreview"><X :size="19" /></button>
        </header>
        <div class="document-preview-body">
          <div v-if="previewLoading" class="document-loading">
            <i></i><span>正在加载并解析文档...</span>
          </div>
          <div v-else-if="previewError" class="document-error">
            <FileText :size="48" />
            <h3>暂时无法预览</h3>
            <p>{{ previewError }}</p>
            <button @click="downloadFile(preview)"><Download :size="16" />下载文件</button>
          </div>
          <pre v-else-if="previewKind === 'text'" class="text-preview">{{ previewText }}</pre>
          <VueOfficeDocx
            v-else-if="previewKind === 'docx' && previewSource"
            :src="previewSource"
            class="office-preview"
            @rendered="previewRendered"
            @error="previewFailed"
          />
          <VueOfficeExcel
            v-else-if="previewKind === 'excel' && previewSource"
            :src="previewSource"
            class="office-preview excel-preview"
            @rendered="previewRendered"
            @error="previewFailed"
          />
          <VueOfficePdf
            v-else-if="previewKind === 'pdf' && previewSource"
            :src="previewSource"
            class="office-preview pdf-preview"
            @rendered="previewRendered"
            @error="previewFailed"
          />
        </div>
      </div>
    </div>

    <div v-if="preview && fileType === 'video'" class="modal-mask video-mask" @click.self="closePreview">
      <div class="video-preview-modal">
        <header class="video-preview-head">
          <div class="video-file-icon"><Video :size="20" /></div>
          <div>
            <strong>{{ preview.originalName }}</strong>
            <span>{{ formatSize(preview.fileSize) }} · {{ storageName(preview.storageType) }}</span>
          </div>
          <button class="document-download" @click="downloadFile(preview)"><Download :size="16" />下载</button>
          <button class="document-close" @click="closePreview"><X :size="19" /></button>
        </header>
        <div class="video-preview-body">
          <video
            :src="preview.accessUrl"
            controls
            autoplay
            preload="metadata"
            playsinline
            @error="videoError = '当前视频编码不受浏览器支持，请下载后播放或转换为 H.264 编码的 MP4 视频。'"
          >您的浏览器不支持在线视频播放。</video>
          <div v-if="videoError" class="video-error">{{ videoError }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.file-panel {
  min-height: 540px;
  padding: 18px;
  display: flex;
  flex-direction: column;
}

.file-panel .filter-bar {
  min-height: 58px;
  margin: -18px -18px 18px;
  padding: 11px 18px;
  border-bottom: 1px solid #edf1f7;
  background: linear-gradient(180deg, #fff 0%, #fbfcfe 100%);
}

.view-switch {
  height: 34px;
  padding: 3px;
  display: flex;
  align-items: center;
  gap: 2px;
  border: 1px solid #e0e8f3;
  border-radius: 7px;
  background: #f5f8fc;
}

.view-switch button {
  width: 29px;
  height: 27px;
  display: grid;
  place-items: center;
  border-radius: 5px;
  color: #8295ae;
  transition: all .18s;
}

.view-switch button:hover {
  color: #2773e8;
}

.view-switch button.active {
  color: #176cff;
  background: #fff;
  box-shadow: 0 2px 7px #315f9c1f;
}

.file-empty {
  min-height: 350px;
  flex: 1;
  border: 1px dashed #d7e3f3;
  border-radius: 12px;
  background:
    radial-gradient(circle at 50% 34%, #edf5ff 0, #f8fbff 30%, transparent 56%),
    linear-gradient(180deg, #fbfdff 0%, #f8faff 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8093ac;
  transition: border-color .2s, background .2s, transform .2s;
}

.file-empty.dragging {
  border-color: #2c76ed;
  background: #f0f6ff;
  transform: scale(.997);
}

.empty-visual {
  width: 96px;
  height: 96px;
  position: relative;
  display: grid;
  place-items: center;
  margin-bottom: 20px;
  border-radius: 30px;
  color: #397cf0;
  background: linear-gradient(145deg, #fff, #dfecff);
  box-shadow: 0 16px 34px #397cf026, inset 0 0 0 1px #fff;
}

.empty-visual::before,
.empty-visual::after {
  content: "";
  position: absolute;
  border-radius: 50%;
  border: 1px solid #cfe1fb;
}

.empty-visual::before {
  inset: -13px;
  opacity: .7;
}

.empty-visual::after {
  inset: -27px;
  opacity: .35;
}

.attachment-visual {
  color: #7558e8;
  background: linear-gradient(145deg, #fff, #eae6ff);
  box-shadow: 0 16px 34px #7558e824, inset 0 0 0 1px #fff;
}

.video-visual {
  color: #e05677;
  background: linear-gradient(145deg, #fff, #ffe8ee);
  box-shadow: 0 16px 34px #d34b6e24, inset 0 0 0 1px #fff;
}

.visual-dot {
  position: absolute;
  z-index: 2;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #76a9f7;
  box-shadow: 0 3px 8px #3175df33;
}

.dot-one {
  top: -17px;
  right: 9px;
}

.dot-two {
  width: 6px;
  height: 6px;
  left: -17px;
  bottom: 10px;
  background: #b4cdf5;
}

.file-empty h3 {
  margin: 0;
  color: #294566;
  font-size: 17px;
  font-weight: 650;
  letter-spacing: .2px;
}

.file-empty p {
  margin: 9px 0 18px;
  color: #8b9cb2;
  font-size: 13px;
}

.empty-upload-btn {
  height: 38px;
  padding: 0 18px;
  border: 1px solid #cfe0fb;
  border-radius: 7px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #276fdc;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 5px 14px #397cf014;
  transition: all .18s;
}

.empty-upload-btn:hover {
  color: #fff;
  border-color: #2877ef;
  background: #2877ef;
  box-shadow: 0 7px 17px #2877ef36;
  transform: translateY(-1px);
}

.empty-upload-btn:disabled {
  opacity: .6;
  cursor: wait;
}

.empty-meta {
  margin-top: 18px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #a3b0c1;
  font-size: 11px;
}

.empty-meta i {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #bdc8d6;
}

.table-footer {
  margin-top: auto;
}

.image-list-table {
  width: 100%;
}

.image-list-row {
  grid-template-columns: 86px minmax(180px, 1.7fr) .7fr .7fr .75fr 1fr .7fr;
  min-height: 68px;
}

.image-list-row.attachment-head {
  min-height: 42px;
}

.list-thumbnail {
  width: 58px;
  height: 44px;
  padding: 0;
  overflow: hidden;
  border: 1px solid #e4ebf4;
  border-radius: 6px;
  background: #f2f5f9;
}

.list-thumbnail img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform .2s;
}

.list-thumbnail:hover img {
  transform: scale(1.08);
}

.list-image-name {
  color: #3e597a;
  font-weight: 500;
}

.document-mask {
  padding: 24px;
}

.document-preview-modal {
  width: min(1180px, 92vw);
  height: min(820px, 90vh);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 12px;
  background: #f1f4f8;
  box-shadow: 0 22px 70px #10254452;
}

.document-preview-head {
  height: 64px;
  flex: none;
  padding: 0 18px;
  display: flex;
  align-items: center;
  gap: 11px;
  border-bottom: 1px solid #e5ebf3;
  background: #fff;
}

.document-file-icon {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #2673e9;
  background: #eaf3ff;
}

.document-preview-head>div:nth-child(2) {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.document-preview-head strong {
  max-width: 650px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #294566;
  font-size: 14px;
}

.document-preview-head span {
  margin-top: 4px;
  color: #94a4b8;
  font-size: 11px;
}

.document-download {
  height: 34px;
  margin-left: auto;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 6px;
  color: #2b70d7;
  background: #edf4ff;
  font-size: 12px;
}

.document-close {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 6px;
  color: #7589a4;
}

.document-close:hover {
  color: #e34f4f;
  background: #fff0f0;
}

.document-preview-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  position: relative;
}

.office-preview {
  min-height: 100%;
}

.excel-preview {
  background: #fff;
}

.pdf-preview {
  min-height: 100%;
  background: #525659;
}

.text-preview {
  min-height: 100%;
  margin: 0;
  padding: 26px 30px;
  overflow: auto;
  color: #334d6d;
  background: #fff;
  white-space: pre-wrap;
  word-break: break-word;
  tab-size: 4;
  font: 13px/1.75 Consolas, "SFMono-Regular", monospace;
}

.document-loading,
.document-error {
  height: 100%;
  min-height: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8496ad;
}

.document-loading i {
  width: 34px;
  height: 34px;
  margin-bottom: 14px;
  border: 3px solid #dbe6f5;
  border-top-color: #2673e9;
  border-radius: 50%;
  animation: document-spin .8s linear infinite;
}

.document-error svg {
  color: #9db2cf;
}

.document-error h3 {
  margin: 15px 0 7px;
  color: #3d5879;
  font-size: 16px;
}

.document-error p {
  max-width: 530px;
  margin: 0;
  color: #899bb2;
  font-size: 13px;
  text-align: center;
  line-height: 1.7;
}

.document-error button {
  height: 36px;
  margin-top: 18px;
  padding: 0 15px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border-radius: 6px;
  color: #fff;
  background: #2673e9;
}

.video-mask {
  padding: 24px;
}

.video-preview-modal {
  width: min(1120px, 92vw);
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 12px;
  background: #0b0d12;
  box-shadow: 0 22px 70px #07101f70;
}

.video-preview-head {
  height: 64px;
  flex: none;
  padding: 0 18px;
  display: flex;
  align-items: center;
  gap: 11px;
  border-bottom: 1px solid #e5ebf3;
  background: #fff;
}

.video-file-icon {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #e05677;
  background: #fff0f4;
}

.video-preview-head>div:nth-child(2) {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.video-preview-head strong {
  max-width: 650px;
  overflow: hidden;
  color: #294566;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-preview-head span {
  margin-top: 4px;
  color: #94a4b8;
  font-size: 11px;
}

.video-preview-body {
  min-height: 360px;
  position: relative;
  display: grid;
  place-items: center;
  background: radial-gradient(circle at center, #202632 0%, #080a0e 72%);
}

.video-preview-body video {
  width: 100%;
  max-height: calc(90vh - 64px);
  display: block;
  outline: none;
}

.video-error {
  position: absolute;
  right: 18px;
  bottom: 18px;
  left: 18px;
  padding: 11px 14px;
  border: 1px solid #8a3f5066;
  border-radius: 7px;
  color: #ffd9e2;
  background: #4c1d29e8;
  font-size: 12px;
  text-align: center;
}

@keyframes document-spin {
  to { transform: rotate(360deg); }
}

:global(:root[data-theme="dark"]) .file-panel .filter-bar {
  border-color: #2c2c32;
  background: linear-gradient(180deg, #171719, #19191c);
}

:global(:root[data-theme="dark"]) .view-switch {
  border-color: #343943;
  background: #202126;
}

:global(:root[data-theme="dark"]) .view-switch button.active {
  color: #86b4ff;
  background: #2b2c32;
}

:global(:root[data-theme="dark"]) .file-empty {
  border-color: #353b47;
  background:
    radial-gradient(circle at 50% 34%, #1b2a43 0, #15171c 34%, transparent 60%),
    #151517;
}

:global(:root[data-theme="dark"]) .empty-visual {
  background: linear-gradient(145deg, #262b35, #17243a);
  box-shadow: 0 16px 34px #0005;
}

:global(:root[data-theme="dark"]) .video-visual {
  color: #ff89a6;
  background: linear-gradient(145deg, #2c252a, #351a24);
}

:global(:root[data-theme="dark"]) .file-empty h3 {
  color: #e5ebf4;
}

:global(:root[data-theme="dark"]) .empty-upload-btn {
  color: #8bb8ff;
  border-color: #334a6c;
  background: #1d2737;
}

:global(:root[data-theme="dark"]) .list-thumbnail {
  border-color: #343943;
  background: #222226;
}

:global(:root[data-theme="dark"]) .list-image-name {
  color: #d5deeb;
}

:global(:root[data-theme="dark"]) .document-preview-modal {
  background: #111114;
}

:global(:root[data-theme="dark"]) .document-preview-head {
  border-color: #2c2c32;
  background: #19191c;
}

:global(:root[data-theme="dark"]) .video-preview-head {
  border-color: #2c2c32;
  background: #19191c;
}

:global(:root[data-theme="dark"]) .video-preview-head strong {
  color: #e1e8f2;
}

:global(:root[data-theme="dark"]) .document-preview-head strong {
  color: #e1e8f2;
}

:global(:root[data-theme="dark"]) .text-preview {
  color: #ced8e7;
  background: #151517;
}

:global(:root[data-theme="dark"]) .document-error h3 {
  color: #dce4ef;
}
</style>
