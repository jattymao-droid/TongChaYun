<template>
  <div :class="['survey-studio', { embedded }]" v-loading="loading">
    <header class="studio-top">
      <div class="top-left">
        <el-button v-if="!embedded" type="text" icon="el-icon-arrow-left" class="back-btn" @click="goBack">返回</el-button>
        <input class="title-input" v-model="survey.surveyName" placeholder="问卷标题" @blur="saveSurveyMetaQuiet" />
        <span class="save-state" :class="saveStateClass">{{ saveStateText }}</span>
      </div>
      <div class="top-tabs">
        <button type="button" class="tab on">编辑</button>
        <button type="button" class="tab" @click="goShare">分享</button>
        <button type="button" class="tab" @click="goStats">统计</button>
      </div>
      <div class="top-right">
        <el-button size="small" icon="el-icon-view" @click="goPreview">试答</el-button>
        <el-button size="small" type="primary" plain :loading="saving" @click="handleSave" v-hasPermi="['biz:survey:edit']">保存</el-button>
        <el-button size="small" type="primary" :loading="publishing" @click="goPublish" v-hasPermi="['biz:survey:publish']">发布</el-button>
      </div>
    </header>

    <div class="studio-body">
      <aside class="left-panel">
        <div class="left-nav">
          <button type="button" :class="{ on: leftMode === 'types' }" @click="leftMode = 'types'">
            <i class="el-icon-s-grid"></i><span>题型</span>
          </button>
          <button type="button" :class="{ on: leftMode === 'outline' }" @click="leftMode = 'outline'">
            <i class="el-icon-s-order"></i><span>大纲</span>
          </button>
          <button type="button" :class="{ on: leftMode === 'logic' }" @click="leftMode = 'logic'">
            <i class="el-icon-share"></i><span>逻辑</span>
          </button>
        </div>

        <div class="left-body">
        <template v-if="leftMode === 'types'">
          <div v-for="g in typeGroups" :key="g.name" class="type-group">
            <div class="group-name">{{ g.name }}</div>
            <div class="type-grid">
              <button
                v-for="t in g.items"
                :key="t.value"
                type="button"
                class="type-chip"
                @click="addQuestion(t.value)"
              >
                <i :class="t.icon"></i>
                <span>{{ t.label }}</span>
              </button>
            </div>
          </div>
        </template>

        <template v-else-if="leftMode === 'outline'">
          <div class="panel-title">大纲 <em>{{ questions.length }}</em></div>
          <draggable v-model="questions" handle=".ol-drag" animation="200">
            <div
              v-for="(q, idx) in questions"
              :key="q._key"
              class="outline-item"
              :class="{ on: idx === activeIndex, 'is-break': q.qType === 'page_break' }"
              @click="activeIndex = idx; scrollToQ(idx)"
            >
              <i class="el-icon-rank ol-drag"></i>
              <span class="ol-no">{{ outlineLabel(q, idx) }}</span>
              <span class="ol-title">{{ q.qType === 'page_break' ? (q.title || '分页') : (q.title || '未命名') }}</span>
            </div>
          </draggable>
          <el-empty v-if="!questions.length" description="暂无题目" :image-size="56" />
        </template>

        <template v-else>
          <div class="panel-title">逻辑</div>
          <survey-jump-flow :questions="questionsForFlow" />
        </template>
        </div>
      </aside>

      <main class="canvas" ref="canvas">
        <div class="canvas-sheet">
          <div class="sheet-head" :class="{ on: rightTab === 'survey' && !current }" @click="selectSurvey">
            <input class="sheet-title" v-model="survey.surveyName" placeholder="点击编辑问卷标题" @focus="selectSurvey" @blur="saveSurveyMetaQuiet" />
            <textarea class="sheet-desc" v-model="survey.surveyDesc" rows="2" placeholder="请输入问卷说明（选填）" @focus="selectSurvey" @blur="saveSurveyMetaQuiet" />
          </div>

          <div v-if="!questions.length" class="canvas-empty">
            <div class="empty-ico"><i class="el-icon-edit-outline"></i></div>
            <h3>开始设计问卷</h3>
            <p>从左侧选择题型，或一键添加单选题</p>
            <el-button type="primary" icon="el-icon-plus" @click="leftMode = 'types'; addQuestion('radio')">添加单选题</el-button>
          </div>

          <draggable v-model="questions" handle=".q-drag" animation="200" class="q-list">
            <div
              v-for="(q, idx) in questions"
              :key="q._key"
              class="q-wrap"
            >
            <div
              :ref="'qblock' + idx"
              class="q-block"
              :class="{ on: idx === activeIndex, flash: flashKey === q._key }"
              @click="activeIndex = idx; rightTab = 'question'"
            >
              <div class="q-toolbar">
                <i class="el-icon-rank q-drag" title="拖拽排序"></i>
                <span class="q-no">{{ outlineLabel(q, idx) }}</span>
                <el-tag size="mini" effect="plain">{{ typeLabel(q.qType) }}</el-tag>
                <el-tag size="mini" type="danger" effect="plain" v-if="q.required === '1' && !isDisplayOnly(q.qType)">必答</el-tag>
                <div class="q-actions">
                  <el-button type="text" icon="el-icon-document-copy" title="复制" @click.stop="duplicateQuestion(idx)" />
                  <el-button type="text" icon="el-icon-top" title="上移" :disabled="idx === 0" @click.stop="moveQuestion(idx, -1)" />
                  <el-button type="text" icon="el-icon-bottom" title="下移" :disabled="idx === questions.length - 1" @click.stop="moveQuestion(idx, 1)" />
                  <el-button type="text" icon="el-icon-delete" title="删除" @click.stop="removeQuestion(idx)" />
                </div>
              </div>

              <div class="q-body" v-if="q.qType === 'page_break'">
                <div class="page-break-bar">
                  <span class="pb-line" />
                  <span class="pb-label">分页符</span>
                  <span class="pb-line" />
                </div>
                <el-input
                  v-model="q.title"
                  size="small"
                  placeholder="下一页标题（选填）"
                  @click.native.stop
                />
                <div class="hint">作答端「按分页」模式下，填写者在此处分页；标题将显示在下一页顶部</div>
              </div>

              <div class="q-body" v-else>
                <div class="q-title-row">
                  <span class="req" v-if="q.required === '1' && !isDisplayOnly(q.qType)">*</span>
                  <el-input
                    type="textarea"
                    :autosize="{ minRows: 1, maxRows: 4 }"
                    v-model="q.title"
                    placeholder="请输入题目标题"
                    @click.native.stop
                  />
                </div>

                <div v-if="q.qType === 'section'" class="q-preview section-preview">
                  <el-input type="textarea" :rows="3" v-model="q._content" placeholder="说明文案" @click.native.stop />
                </div>
                <div v-else-if="q.qType === 'agreement'" class="q-preview agreement-preview" @click.stop>
                  <div class="agree-html" v-html="q._content || '<p class=\"muted-ph\">请在右侧编辑协议正文</p>'" />
                  <el-checkbox :value="true" disabled>{{ q._agreeLabel || '我已阅读并同意' }}</el-checkbox>
                  <div
                    v-for="(sq, si) in designBoundSignatures(idx)"
                    :key="'bs-' + si"
                    class="agree-sign-preview"
                    @click.stop="activeIndex = sq.i"
                  >
                    <div class="agree-sign-title">{{ sq.q.title || '手写签名' }}</div>
                    <div class="muted-box">签名区（显示在协议最下方）</div>
                  </div>
                </div>
                <div v-else-if="q.qType === 'signature'" class="q-preview muted-box">
                  {{ q._bindAgreementSort != null && q._bindAgreementSort !== '' ? '手写签名区（将显示在绑定协议最下方）' : '手写签名区' }}
                </div>

                <div v-else-if="needOptions(q.qType) && q.qType !== 'cascade_select'" class="q-preview opts">
                  <div v-for="(opt, oi) in q._options" :key="oi" class="opt-edit" @click.stop>
                    <span class="opt-mark">{{ q.qType === 'checkbox' ? '▢' : '○' }}</span>
                    <el-input v-model="opt.label" size="small" placeholder="选项文案" @input="syncOptions" />
                    <el-input v-if="isImageOptionType(q.qType)" v-model="opt.imageUrl" size="small" placeholder="图片 URL" />
                    <el-button type="text" icon="el-icon-delete" @click="activeIndex = idx; removeOption(oi)" />
                  </div>
                  <el-button v-if="q.qType !== 'yesno'" size="mini" icon="el-icon-plus" @click.stop="activeIndex = idx; addOption()">添加选项</el-button>
                  <el-button v-if="q.qType !== 'yesno'" size="mini" icon="el-icon-edit-outline" @click.stop="activeIndex = idx; openBatchOptions()">批量编辑</el-button>
                </div>

                <div v-else-if="q.qType === 'cascade_select'" class="q-preview muted-box">级联选项请在右侧属性中配置</div>
                <div v-else-if="q.qType === 'matrix_radio'" class="q-preview muted-box">矩阵行列请在右侧属性中配置</div>
                <div v-else-if="isTextType(q.qType)" class="q-preview">
                  <el-input size="small" disabled :placeholder="q._placeholder || '填写者输入区域'" />
                </div>
                <div v-else-if="q.qType === 'rate'" class="q-preview"><el-rate disabled :value="3" /></div>
                <div v-else-if="q.qType === 'nps' || q.qType === 'slider'" class="q-preview muted-box">{{ typeLabel(q.qType) }}预览</div>
                <div v-else class="q-preview muted-box">{{ typeLabel(q.qType) }}</div>
              </div>
            </div>
            <button type="button" class="insert-gap" title="在此插入题目" @click.stop="insertQuestionAt(idx + 1)">
              <i class="el-icon-plus"></i>
              <span>插入题目</span>
            </button>
            </div>
          </draggable>

          <div class="add-bar">
            <el-button type="primary" plain icon="el-icon-plus" @click="leftMode = 'types'; addQuestion('radio')">添加单选题</el-button>
            <el-dropdown trigger="click" @command="addQuestion">
              <el-button plain>更多题型<i class="el-icon-arrow-down el-icon--right"></i></el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item v-for="t in typeList" :key="t.value" :command="t.value">{{ t.label }}</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>

          <div class="sheet-end">
            <div class="end-title">问卷到此结束</div>
            <div class="end-sub">感谢填写</div>
          </div>
        </div>
      </main>

      <aside class="right-panel">
        <div class="rp-tabs">
          <button type="button" :class="{ on: rightTab === 'survey' }" @click="rightTab = 'survey'; selectSurvey()">整卷</button>
          <button type="button" :class="{ on: rightTab === 'question' }" @click="rightTab = 'question'">题目</button>
          <button type="button" :class="{ on: rightTab === 'options' }" @click="rightTab = 'options'" :disabled="!current || !needOptions(current.qType)">选项</button>
        </div>

        <div class="rp-body" v-if="rightTab === 'survey'">
          <el-form label-position="top" size="small">
            <el-form-item label="问卷标题">
              <el-input v-model="survey.surveyName" @blur="saveSurveyMetaQuiet" />
            </el-form-item>
            <el-form-item label="问卷说明">
              <el-input type="textarea" :rows="4" v-model="survey.surveyDesc" @blur="saveSurveyMetaQuiet" />
            </el-form-item>
            <el-form-item label="填答方式">
              <el-radio-group v-model="fillMode" @change="saveSurveyMetaQuiet">
                <el-radio label="all">整页展示</el-radio>
                <el-radio label="step">一页一题</el-radio>
                <el-radio label="pages">按分页</el-radio>
              </el-radio-group>
              <div class="hint">「按分页」需插入分页符；一页一题适合较长问卷</div>
            </el-form-item>
            <el-form-item label="题目数">
              <span>{{ questions.length }}</span>
            </el-form-item>
          </el-form>
        </div>

        <div class="rp-body" v-else-if="current && (rightTab === 'question' || rightTab === 'options')">
          <el-form label-position="top" size="small">
            <el-form-item label="题型" v-if="rightTab === 'question'">
              <el-select v-model="current.qType" style="width:100%" @change="onTypeChange">
                <el-option v-for="t in typeList" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item :label="current.qType === 'page_break' ? '下一页标题' : '题干'" v-if="rightTab === 'question'">
              <el-input v-model="current.title" type="textarea" :rows="3" :placeholder="current.qType === 'page_break' ? '选填，显示在下一页顶部' : ''" />
            </el-form-item>
            <el-form-item label="必答题" v-if="rightTab === 'question' && !isDisplayOnly(current.qType)">
              <el-switch v-model="current.required" active-value="1" inactive-value="0" />
            </el-form-item>

            <template v-if="rightTab === 'options' || (rightTab === 'question' && needOptions(current.qType))">
              <el-form-item :label="current.qType === 'cascade_select' ? '级联选项' : '选项'" v-if="needOptions(current.qType) && current.qType !== 'cascade_select'">
                <div v-for="(opt, oi) in current._options" :key="oi" class="opt-row" :class="{ stacked: isImageOptionType(current.qType) }">
                  <el-input v-model="opt.label" placeholder="选项文案" @input="syncOptions" />
                  <el-input v-if="isImageOptionType(current.qType)" v-model="opt.imageUrl" placeholder="图片 URL" />
                  <el-select v-if="canJump(current.qType)" v-model="opt._toSort" placeholder="跳转" clearable style="width: 120px">
                    <el-option label="下一题" :value="null" />
                    <el-option label="结束填写" :value="-1" />
                    <el-option v-for="(tq, ti) in questions" :key="tq._key" :label="'跳到 Q' + (ti + 1)" :value="ti" :disabled="ti === activeIndex" />
                  </el-select>
                  <el-button type="text" icon="el-icon-delete" @click="removeOption(oi)" />
                </div>
                <el-button size="mini" icon="el-icon-plus" @click="addOption" v-if="current.qType !== 'yesno'">添加选项</el-button>
                <el-button size="mini" icon="el-icon-edit-outline" @click="openBatchOptions" v-if="current.qType !== 'yesno'">批量编辑</el-button>
                <div class="hint" v-if="canJump(current.qType)">可为选项配置跳题</div>
              </el-form-item>

              <el-form-item label="级联选项" v-if="current.qType === 'cascade_select'">
                <div v-for="(opt, oi) in current._options" :key="'c'+oi" class="cascade-block">
                  <div class="opt-row">
                    <el-input v-model="opt.label" placeholder="一级文案" @input="syncOptions" />
                    <el-input v-model="opt.value" placeholder="value" style="width:90px" @input="syncOptions" />
                    <el-button type="text" icon="el-icon-delete" @click="removeOption(oi)" />
                  </div>
                  <div v-for="(child, ci) in (opt.children || [])" :key="'cc'+ci" class="opt-row child-row">
                    <el-input v-model="child.label" placeholder="二级文案" />
                    <el-input v-model="child.value" placeholder="value" style="width:90px" />
                    <el-button type="text" icon="el-icon-delete" @click="removeCascadeChild(oi, ci)" />
                  </div>
                  <el-button size="mini" icon="el-icon-plus" @click="addCascadeChild(oi)">添加子项</el-button>
                </div>
                <el-button size="mini" icon="el-icon-plus" class="mt8" @click="addCascadeParent">添加一级</el-button>
              </el-form-item>
            </template>

            <template v-if="rightTab === 'question'">
              <el-form-item label="矩阵行" v-if="needRows(current.qType)">
                <div v-for="(row, ri) in current._rows" :key="ri" class="opt-row">
                  <el-input v-model="row.label" placeholder="陈述文案" />
                  <el-input v-model="row.value" placeholder="value" style="width:90px" />
                  <el-button type="text" icon="el-icon-delete" @click="removeRow(ri)" />
                </div>
                <el-button size="mini" icon="el-icon-plus" @click="addRow">添加行</el-button>
              </el-form-item>
              <el-form-item label="占位符" v-if="isTextType(current.qType)">
                <el-input v-model="current._placeholder" />
              </el-form-item>
              <el-form-item label="最少字数" v-if="isTextType(current.qType)">
                <el-input-number v-model="current._minLength" :min="0" :max="5000" />
              </el-form-item>
              <el-form-item label="最多字数" v-if="isTextType(current.qType)">
                <el-input-number v-model="current._maxLength" :min="0" :max="5000" />
              </el-form-item>
              <el-form-item label="最高分" v-if="current.qType === 'rate'">
                <el-input-number v-model="current._max" :min="1" :max="10" />
              </el-form-item>
              <el-form-item label="大小限制" v-if="current.qType === 'file'">
                <el-input-number v-model="current._maxSizeMb" :min="1" :max="10" />
                <span class="hint"> MB</span>
              </el-form-item>
              <el-form-item label="上传提示" v-if="current.qType === 'file'">
                <el-input v-model="current._placeholder" />
              </el-form-item>
              <el-form-item label="说明内容" v-if="current.qType === 'section'">
                <el-input v-model="current._content" type="textarea" :rows="4" />
              </el-form-item>
              <el-form-item label="协议正文" v-if="current.qType === 'agreement'">
                <editor v-model="current._content" :min-height="180" type="base64" />
              </el-form-item>
              <el-form-item label="同意文案" v-if="current.qType === 'agreement'">
                <el-input v-model="current._agreeLabel" placeholder="我已阅读并同意" />
              </el-form-item>
              <el-form-item label="绑定协议" v-if="current.qType === 'signature'">
                <el-select v-model="current._bindAgreementSort" clearable placeholder="不绑定（始终显示）" style="width:100%" @change="onBindAgreementChange">
                  <el-option v-for="opt in agreementOptions" :key="opt.value" :label="opt.label" :value="opt.value" :disabled="opt.value === activeIndex" />
                </el-select>
                <div class="hint">绑定后，签名显示在该协议最下方；填写者勾选同意后才可签名</div>
              </el-form-item>
              <el-form-item label="笔迹颜色" v-if="current.qType === 'signature'">
                <el-color-picker v-model="current._penColor" size="mini" />
              </el-form-item>
              <el-form-item label="签名板高度" v-if="current.qType === 'signature'">
                <el-input-number v-model="current._padHeight" :min="100" :max="360" :step="10" />
              </el-form-item>
              <el-form-item label="最小值" v-if="isNumberType(current.qType) || current.qType === 'nps'">
                <el-input-number v-model="current._min" :disabled="current.qType === 'nps'" />
              </el-form-item>
              <el-form-item label="最大值" v-if="isNumberType(current.qType) || current.qType === 'nps' || current.qType === 'rate'">
                <el-input-number v-model="current._max" :disabled="current.qType === 'nps'" :min="current.qType === 'rate' ? 1 : 0" :max="current.qType === 'rate' ? 10 : 100000" />
              </el-form-item>
              <el-form-item label="步长" v-if="isNumberType(current.qType)">
                <el-input-number v-model="current._step" :min="0.01" :step="1" />
              </el-form-item>
              <el-form-item label="左端文案" v-if="current.qType === 'nps'">
                <el-input v-model="current._leftLabel" />
              </el-form-item>
              <el-form-item label="右端文案" v-if="current.qType === 'nps'">
                <el-input v-model="current._rightLabel" />
              </el-form-item>
              <el-form-item label="显示条件" v-if="!isDisplayOnly(current.qType)">
                <el-select v-model="current._visibleIfSource" clearable placeholder="始终显示" style="width:100%" class="mb8">
                  <el-option
                    v-for="(tq, ti) in questions"
                    :key="'vif-' + tq._key"
                    :label="'仅当 Q' + (ti + 1) + ' 选中指定值时显示'"
                    :value="ti"
                    :disabled="ti === activeIndex"
                  />
                </el-select>
                <el-input
                  v-if="current._visibleIfSource !== null && current._visibleIfSource !== undefined && current._visibleIfSource !== ''"
                  v-model="current._visibleIfValue"
                  placeholder="触发显示的选项 value"
                />
              </el-form-item>
            </template>
          </el-form>
        </div>
        <div class="rp-body empty" v-else>
          <el-empty description="请选择一道题目" :image-size="56" />
        </div>
      </aside>
    </div>

    <el-dialog title="批量编辑选项" :visible.sync="batchOpen" width="480px" append-to-body>
      <el-alert title="每行一个选项；可用「文案|值」格式。保存后将替换当前题目全部选项。" type="info" :closable="false" show-icon class="mb12" />
      <el-input
        type="textarea"
        :rows="10"
        v-model="batchText"
        placeholder="选项1&#10;选项2|B&#10;非常满意|5"
      />
      <span slot="footer">
        <el-button @click="batchOpen = false">取消</el-button>
        <el-button type="primary" @click="applyBatchOptions">应用</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import { getSurvey, saveSurveyQuestions, updateSurvey, publishSurvey } from '@/api/biz/survey'
import SurveyJumpFlow from '@/components/biz/SurveyJumpFlow'
import Editor from '@/components/Editor'

let keySeq = 1

export default {
  name: 'BizSurveyDesign',
  components: { draggable, SurveyJumpFlow, Editor },
  dicts: ['biz_question_type'],
  props: {
    embedded: { type: Boolean, default: false },
    surveyIdProp: { type: [String, Number], default: null }
  },
  data() {
    return {
      loading: false,
      saving: false,
      publishing: false,
      surveyId: null,
      survey: {},
      questions: [],
      activeIndex: 0,
      savedSnapshot: '',
      leftMode: 'types',
      rightTab: 'question',
      fillMode: 'all',
      batchOpen: false,
      batchText: '',
      flashKey: '',
      savePulse: false,
      typeList: [
        { value: 'radio', label: '单选', icon: 'el-icon-success' },
        { value: 'checkbox', label: '多选', icon: 'el-icon-finished' },
        { value: 'select', label: '下拉', icon: 'el-icon-arrow-down' },
        { value: 'yesno', label: '是非题', icon: 'el-icon-circle-check' },
        { value: 'likert', label: '量表', icon: 'el-icon-s-data' },
        { value: 'image_radio', label: '图片单选', icon: 'el-icon-picture-outline' },
        { value: 'image_checkbox', label: '图片多选', icon: 'el-icon-picture' },
        { value: 'input', label: '单行填空', icon: 'el-icon-edit' },
        { value: 'textarea', label: '多行填空', icon: 'el-icon-document' },
        { value: 'number', label: '数字', icon: 'el-icon-s-finance' },
        { value: 'email', label: '邮箱', icon: 'el-icon-message' },
        { value: 'phone', label: '手机号', icon: 'el-icon-mobile-phone' },
        { value: 'url', label: '网址', icon: 'el-icon-link' },
        { value: 'idcard', label: '身份证', icon: 'el-icon-postcard' },
        { value: 'date', label: '日期', icon: 'el-icon-date' },
        { value: 'time', label: '时间', icon: 'el-icon-alarm-clock' },
        { value: 'datetime', label: '日期时间', icon: 'el-icon-time' },
        { value: 'rate', label: '评分', icon: 'el-icon-star-off' },
        { value: 'nps', label: 'NPS', icon: 'el-icon-s-marketing' },
        { value: 'slider', label: '滑块', icon: 'el-icon-s-operation' },
        { value: 'file', label: '附件上传', icon: 'el-icon-paperclip' },
        { value: 'matrix_radio', label: '矩阵单选', icon: 'el-icon-s-grid' },
        { value: 'cascade_select', label: '级联选择', icon: 'el-icon-share' },
        { value: 'section', label: '说明段落', icon: 'el-icon-info' },
        { value: 'page_break', label: '分页符', icon: 'el-icon-minus' },
        { value: 'agreement', label: '协议同意', icon: 'el-icon-document-checked' },
        { value: 'signature', label: '手写签名', icon: 'el-icon-edit-outline' }
      ]
    }
  },
  computed: {
    current() {
      return this.questions[this.activeIndex] || null
    },
    dirty() {
      return this.savedSnapshot !== '' && this.snapshotOf() !== this.savedSnapshot
    },
    saveStateText() {
      if (this.saving) return '保存中…'
      if (this.savePulse) return '已保存'
      return this.dirty ? '未保存' : '已保存'
    },
    saveStateClass() {
      return {
        dirty: this.dirty && !this.saving && !this.savePulse,
        saving: this.saving,
        saved: this.savePulse || (!this.dirty && !this.saving)
      }
    },
    typeGroups() {
      const map = {}
      this.typeList.forEach(t => { map[t.value] = t })
      const pick = keys => keys.map(k => map[k]).filter(Boolean)
      return [
        { name: '选择', items: pick(['radio', 'checkbox', 'select', 'image_radio', 'image_checkbox', 'yesno', 'likert']) },
        { name: '文本输入', items: pick(['input', 'textarea', 'phone', 'email', 'url', 'idcard']) },
        { name: '高级题型', items: pick(['number', 'date', 'time', 'datetime', 'rate', 'nps', 'slider', 'file', 'matrix_radio', 'cascade_select']) },
        { name: '结构', items: pick(['section', 'page_break']) },
        { name: '合规', items: pick(['agreement', 'signature']) }
      ]
    },
    agreementOptions() {
      return this.questions
        .map((q, i) => ({ q, i }))
        .filter(x => x.q.qType === 'agreement')
        .map(x => ({
          value: x.i,
          label: 'Q' + this.answerableNo(x.i) + ' ' + (x.q.title || '未命名协议')
        }))
    },
    questionsForFlow() {
      return this.questions.map((q, i) => ({
        title: q.title,
        qType: q.qType,
        sort: i,
        optionsJson: this.needOptions(q.qType) ? JSON.stringify((q._options || []).map(o => ({ label: o.label, value: o.value }))) : null,
        propsJson: JSON.stringify(this.propsOf(q))
      }))
    }
  },
  created() {
    this.surveyId = this.surveyIdProp != null ? this.surveyIdProp : this.$route.params.surveyId
    this.loadDetail()
    window.addEventListener('beforeunload', this.onBeforeUnload)
  },
  beforeDestroy() {
    window.removeEventListener('beforeunload', this.onBeforeUnload)
  },
  beforeRouteLeave(to, from, next) {
    if (this.embedded || !this.dirty) return next()
    this.$confirm('有未保存的修改，确定离开？', '提示', { type: 'warning' }).then(() => next()).catch(() => next(false))
  },
  methods: {
    propsOf(q) {
      const jumps = []
      if (this.canJump(q.qType) && q._options) {
        q._options.forEach(o => {
          if (o._toSort !== null && o._toSort !== undefined && o._toSort !== '') {
            jumps.push({ value: o.value || o.label, toSort: Number(o._toSort) })
          }
        })
      }
      const props = { jumps }
      if (q._visibleIfSource !== null && q._visibleIfSource !== undefined && q._visibleIfSource !== '' && String(q._visibleIfValue || '') !== '') {
        props.visibleIf = { sourceSort: Number(q._visibleIfSource), value: String(q._visibleIfValue) }
      }
      return props
    },
    snapshotOf() {
      try {
        return JSON.stringify({
          name: this.survey.surveyName,
          desc: this.survey.surveyDesc,
          qs: this.buildPayload()
        })
      } catch (e) {
        return String(Date.now())
      }
    },
    isDirty() { return this.dirty },
    onBeforeUnload(e) {
      if (!this.dirty) return
      e.preventDefault()
      e.returnValue = ''
    },
    selectSurvey() {
      this.rightTab = 'survey'
    },
    scrollToQ(idx) {
      this.$nextTick(() => {
        const ref = this.$refs['qblock' + idx]
        const el = Array.isArray(ref) ? ref[0] : ref
        if (el && el.scrollIntoView) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      })
    },
    moveQuestion(idx, delta) {
      const to = idx + delta
      if (to < 0 || to >= this.questions.length) return
      const arr = this.questions
      const tmp = arr[idx]
      this.$set(arr, idx, arr[to])
      this.$set(arr, to, tmp)
      this.activeIndex = to
    },
    onTypeChange() {
      if (!this.current) return
      const q = this.normalizeQuestion({
        qType: this.current.qType,
        title: this.current.title,
        required: this.current.required,
        optionsJson: '',
        propsJson: ''
      })
      q._key = this.current._key
      this.$set(this.questions, this.activeIndex, q)
      if (this.needOptions(q.qType)) this.rightTab = 'options'
    },
    goBack() {
      if (this.dirty) {
        this.$confirm('有未保存的修改，确定离开？', '提示', { type: 'warning' }).then(() => {
          this.$router.push('/biz/survey')
        }).catch(() => {})
        return
      }
      this.$router.push('/biz/survey')
    },
    goPreview() {
      this.handleSave().then(() => {
        this.$router.push('/biz/survey-preview/index/' + this.surveyId)
      }).catch(() => {})
    },
    goShare() {
      this.handleSave().then(() => {
        this.$router.push('/biz/survey-setup/index/' + this.surveyId + '?step=3')
      }).catch(() => {})
    },
    goStats() {
      this.$router.push('/biz/survey-stats/index/' + this.surveyId)
    },
    goPublish() {
      this.handleSave().then(() => {
        this.publishing = true
        return publishSurvey(this.surveyId)
      }).then(() => {
        this.$modal.msgSuccess('发布成功')
        this.$router.push('/biz/survey-setup/index/' + this.surveyId + '?step=3')
      }).catch(() => {}).finally(() => { this.publishing = false })
    },
    saveSurveyMetaQuiet() {
      if (!this.surveyId) return
      let theme = {}
      try { theme = this.survey.themeJson ? JSON.parse(this.survey.themeJson) : {} } catch (e) { theme = {} }
      theme.fillMode = this.fillMode === 'step' || this.fillMode === 'pages' ? this.fillMode : 'all'
      const themeJson = JSON.stringify(theme)
      this.survey.themeJson = themeJson
      updateSurvey({
        surveyId: this.surveyId,
        surveyName: this.survey.surveyName,
        surveyDesc: this.survey.surveyDesc,
        themeJson
      }).catch(() => {})
    },
    outlineLabel(q, idx) {
      if (q && q.qType === 'page_break') return '分页'
      return 'Q' + this.answerableNo(idx)
    },
    answerableNo(idx) {
      let n = 0
      for (let i = 0; i <= idx; i++) {
        const item = this.questions[i]
        if (!item || this.isDisplayOnly(item.qType)) continue
        n++
      }
      return n
    },
    onBindAgreementChange(val) {
      if (!this.current || this.current.qType !== 'signature') return
      if (val === null || val === undefined || val === '') {
        this.current._visibleIfSource = null
        this.current._visibleIfValue = ''
      } else {
        this.current._visibleIfSource = Number(val)
        this.current._visibleIfValue = '1'
      }
    },
    designBoundSignatures(agreementIndex) {
      return this.questions
        .map((q, i) => ({ q, i }))
        .filter(x => x.q.qType === 'signature'
          && x.q._bindAgreementSort != null
          && x.q._bindAgreementSort !== ''
          && Number(x.q._bindAgreementSort) === Number(agreementIndex))
    },
    openBatchOptions() {
      if (!this.current || !this.needOptions(this.current.qType) || this.current.qType === 'yesno' || this.current.qType === 'cascade_select') return
      this.batchText = (this.current._options || []).map(o => {
        if (o.value && o.value !== o.label) return (o.label || '') + '|' + o.value
        return o.label || ''
      }).join('\n')
      this.batchOpen = true
    },
    applyBatchOptions() {
      if (!this.current) return
      const lines = String(this.batchText || '').split(/\r?\n/).map(s => s.trim()).filter(Boolean)
      if (!lines.length) {
        this.$modal.msgWarning('请至少填写一行选项')
        return
      }
      const opts = lines.map((line, i) => {
        const p = line.indexOf('|')
        let label = line
        let value = String(i + 1)
        if (p > 0) {
          label = line.slice(0, p).trim()
          value = line.slice(p + 1).trim() || value
        }
        return { label, value, imageUrl: '', children: [], _toSort: null }
      })
      this.$set(this.current, '_options', opts)
      this.syncOptions()
      this.batchOpen = false
      this.$modal.msgSuccess('选项已更新')
    },
    typeLabel(type) {
      const hit = (this.dict.type.biz_question_type || []).find(d => d.value === type)
      if (hit) return hit.label
      const t = this.typeList.find(x => x.value === type)
      return t ? t.label : type
    },
    needOptions(type) { return ['radio', 'checkbox', 'select', 'yesno', 'likert', 'image_radio', 'image_checkbox', 'matrix_radio', 'cascade_select'].includes(type) },
    needRows(type) { return type === 'matrix_radio' },
    canJump(type) { return ['radio', 'select', 'yesno', 'image_radio', 'likert'].includes(type) },
    isTextType(type) { return ['input', 'textarea', 'phone', 'email', 'url', 'idcard'].includes(type) },
    isImageOptionType(type) { return type === 'image_radio' || type === 'image_checkbox' },
    isNumberType(type) { return ['number', 'slider'].includes(type) },
    isDisplayOnly(type) { return type === 'section' || type === 'page_break' },
    loadDetail() {
      this.loading = true
      return getSurvey(this.surveyId).then(res => {
        const data = res.data || {}
        this.survey = data.survey || {}
        let theme = {}
        try { theme = this.survey.themeJson ? JSON.parse(this.survey.themeJson) : {} } catch (e) { theme = {} }
        this.fillMode = theme.fillMode === 'step' || theme.fillMode === 'pages' ? theme.fillMode : 'all'
        this.questions = (data.questions || []).map(q => this.normalizeQuestion(q))
        this.activeIndex = 0
        this.rightTab = this.questions.length ? 'question' : 'survey'
        this.$nextTick(() => { this.savedSnapshot = this.snapshotOf() })
      }).finally(() => { this.loading = false })
    },
    normalizeQuestion(q) {
      let options = []
      try { options = q.optionsJson ? JSON.parse(q.optionsJson) : [] } catch (e) { options = [] }
      let props = {}
      try { props = q.propsJson ? JSON.parse(q.propsJson) : {} } catch (e) { props = {} }
      if (!options.length && this.needOptions(q.qType)) {
        if (q.qType === 'yesno') options = [{ label: '是', value: '1' }, { label: '否', value: '0' }]
        else if (q.qType === 'likert') options = [
          { label: '非常不同意', value: '1' }, { label: '不同意', value: '2' },
          { label: '一般', value: '3' }, { label: '同意', value: '4' }, { label: '非常同意', value: '5' }
        ]
        else if (q.qType === 'image_radio' || q.qType === 'image_checkbox') options = [{ label: '选项A', value: 'A', imageUrl: '' }, { label: '选项B', value: 'B', imageUrl: '' }]
        else if (q.qType === 'matrix_radio') options = [
          { label: '非常不同意', value: '1' }, { label: '不同意', value: '2' },
          { label: '一般', value: '3' }, { label: '同意', value: '4' }, { label: '非常同意', value: '5' }
        ]
        else if (q.qType === 'cascade_select') options = [
          { label: '北京市', value: 'bj', children: [{ label: '海淀区', value: 'hd' }, { label: '朝阳区', value: 'cy' }] },
          { label: '上海市', value: 'sh', children: [{ label: '浦东新区', value: 'pd' }, { label: '徐汇区', value: 'xh' }] }
        ]
        else options = [{ label: '选项A', value: 'A' }, { label: '选项B', value: 'B' }]
      }
      let rows = Array.isArray(props.rows) ? props.rows.map(r => ({ label: r.label || '', value: r.value || '' })) : []
      if (!rows.length && q.qType === 'matrix_radio') {
        rows = [{ label: '陈述一', value: 'r1' }, { label: '陈述二', value: 'r2' }]
      }
      const jumpMap = {}
      ;(props.jumps || []).forEach(j => { jumpMap[String(j.value)] = j.toSort === undefined ? null : j.toSort })
      options = options.map(o => ({
        label: o.label,
        value: o.value,
        imageUrl: o.imageUrl || '',
        children: Array.isArray(o.children) ? o.children.map(c => ({ label: c.label || '', value: c.value || '' })) : [],
        _toSort: Object.prototype.hasOwnProperty.call(jumpMap, String(o.value)) ? jumpMap[String(o.value)] : null
      }))
      return {
        ...q,
        _key: 'q' + (keySeq++),
        _options: options,
        _rows: rows,
        _placeholder: props.placeholder || (
          q.qType === 'phone' ? '请输入手机号'
            : (q.qType === 'email' ? '请输入邮箱'
              : (q.qType === 'url' ? '请输入网址，如 https://example.com'
                : (q.qType === 'idcard' ? '请输入18位身份证号' : '')))
        ),
        _minLength: props.minLength == null ? undefined : Number(props.minLength),
        _maxLength: props.maxLength == null ? undefined : Number(props.maxLength),
        _max: props.max == null ? (q.qType === 'nps' ? 10 : (q.qType === 'slider' ? 100 : 5)) : Number(props.max),
        _min: props.min == null ? (q.qType === 'nps' || q.qType === 'slider' ? 0 : undefined) : Number(props.min),
        _step: props.step == null ? 1 : Number(props.step),
        _maxSizeMb: props.maxSizeMb == null ? 5 : Number(props.maxSizeMb),
        _leftLabel: props.leftLabel || (q.qType === 'nps' ? '不可能' : ''),
        _rightLabel: props.rightLabel || (q.qType === 'nps' ? '非常可能' : ''),
        _content: props.content || '',
        _agreeLabel: props.agreeLabel || '我已阅读并同意',
        _bindAgreementSort: props.bindAgreementSort != null && props.bindAgreementSort !== ''
          ? Number(props.bindAgreementSort)
          : (props.visibleIf && props.visibleIf.sourceSort != null && String(props.visibleIf.value) === '1'
            ? Number(props.visibleIf.sourceSort) : null),
        _penColor: props.penColor || '#111111',
        _padHeight: props.padHeight == null ? 160 : Number(props.padHeight),
        _visibleIfSource: props.visibleIf && props.visibleIf.sourceSort != null ? Number(props.visibleIf.sourceSort) : null,
        _visibleIfValue: props.visibleIf && props.visibleIf.value != null ? String(props.visibleIf.value) : '',
        required: this.isDisplayOnly(q.qType) ? '0' : (q.required || '0')
      }
    },
    addQuestion(type, atIndex) {
      const titleMap = {
        section: '填写说明',
        page_break: '',
        agreement: '知情同意协议',
        signature: '手写签名'
      }
      const title = Object.prototype.hasOwnProperty.call(titleMap, type) ? titleMap[type] : this.typeLabel(type)
      const q = this.normalizeQuestion({
        qType: type,
        title,
        required: this.isDisplayOnly(type) ? '0' : '1',
        optionsJson: '',
        propsJson: ''
      })
      if (type === 'section') q._content = '请在此填写说明文案，不会作为答题项。'
      if (type === 'agreement') {
        q._content = '<p>请在此编辑协议正文。填写者需勾选同意后方可继续相关签名。</p>'
        q._agreeLabel = '我已阅读并同意'
      }
      if (type === 'signature') {
        q._penColor = '#111111'
        q._padHeight = 160
        q._bindAgreementSort = null
      }
      if (type === 'matrix_radio') {
        q._rows = [{ label: '陈述一', value: 'r1' }, { label: '陈述二', value: 'r2' }]
      }
      if (type === 'nps') { q._min = 0; q._max = 10 }
      if (type === 'slider') { q._min = 0; q._max = 100; q._step = 1 }
      if (type === 'number') { q._min = undefined; q._max = undefined; q._step = 1 }
      const idx = (atIndex == null || atIndex < 0 || atIndex > this.questions.length) ? this.questions.length : atIndex
      this.questions.splice(idx, 0, q)
      this.activeIndex = idx
      this.rightTab = this.needOptions(type) ? 'options' : 'question'
      this.leftMode = 'types'
      this.flashKey = q._key
      setTimeout(() => { if (this.flashKey === q._key) this.flashKey = '' }, 900)
      this.$nextTick(() => this.scrollToQ(this.activeIndex))
      if (type === 'page_break' && this.fillMode === 'all') {
        this.fillMode = 'pages'
        this.saveSurveyMetaQuiet()
        this.$modal.msgSuccess('已切换为「按分页」填答方式')
      }
    },
    insertQuestionAt(idx) {
      this.leftMode = 'types'
      this.addQuestion('radio', idx)
    },
    duplicateQuestion(idx) {
      const src = this.questions[idx]
      if (!src) return
      const clone = this.normalizeQuestion({
        qType: src.qType,
        title: (src.title || '') + '（副本）',
        required: src.required,
        optionsJson: this.needOptions(src.qType) ? JSON.stringify(src._options.map(o => ({ label: o.label, value: o.value, imageUrl: o.imageUrl, children: o.children }))) : '',
        propsJson: ''
      })
      clone._placeholder = src._placeholder
      clone._minLength = src._minLength
      clone._maxLength = src._maxLength
      clone._max = src._max
      clone._maxSizeMb = src._maxSizeMb
      clone._content = src._content
      clone._agreeLabel = src._agreeLabel
      clone._bindAgreementSort = src._bindAgreementSort
      clone._penColor = src._penColor
      clone._padHeight = src._padHeight
      clone._visibleIfSource = src._visibleIfSource
      clone._visibleIfValue = src._visibleIfValue
      if (this.needOptions(src.qType)) {
        clone._options = src._options.map(o => ({
          label: o.label, value: o.value, imageUrl: o.imageUrl || '',
          children: (o.children || []).map(c => ({ label: c.label, value: c.value })),
          _toSort: null
        }))
      }
      this.questions.splice(idx + 1, 0, clone)
      this.activeIndex = idx + 1
    },
    removeQuestion(idx) {
      const q = this.questions[idx]
      const name = (q && q.title) ? q.title : ('Q' + (idx + 1))
      this.$confirm('确定删除题目「' + name + '」？', '删除确认', { type: 'warning' }).then(() => {
        this.questions.splice(idx, 1)
        if (this.activeIndex >= this.questions.length) this.activeIndex = Math.max(0, this.questions.length - 1)
      }).catch(() => {})
    },
    addOption() {
      if (!this.current) return
      const n = this.current._options.length + 1
      this.current._options.push({ label: '选项' + n, value: String(n), imageUrl: '', children: [], _toSort: null })
      this.syncOptions()
    },
    addRow() {
      if (!this.current) return
      if (!this.current._rows) this.$set(this.current, '_rows', [])
      const n = this.current._rows.length + 1
      this.current._rows.push({ label: '陈述' + n, value: 'r' + n })
    },
    removeRow(ri) {
      if (!this.current || !this.current._rows) return
      this.current._rows.splice(ri, 1)
    },
    removeOption(oi) {
      this.current._options.splice(oi, 1)
      this.syncOptions()
    },
    addCascadeParent() {
      if (!this.current._options) this.$set(this.current, '_options', [])
      this.current._options.push({ label: '一级' + (this.current._options.length + 1), value: 'p' + (this.current._options.length + 1), children: [{ label: '二级1', value: 'c1' }] })
      this.syncOptions()
    },
    addCascadeChild(oi) {
      const opt = this.current._options[oi]
      if (!opt.children) this.$set(opt, 'children', [])
      opt.children.push({ label: '子项' + (opt.children.length + 1), value: 'c' + (opt.children.length + 1) })
    },
    removeCascadeChild(oi, ci) {
      const opt = this.current._options[oi]
      if (opt && opt.children) opt.children.splice(ci, 1)
    },
    syncOptions() {
      if (!this.current) return
      this.current._options.forEach((o, i) => { if (!o.value) o.value = String(i + 1) })
    },
    buildPayload() {
      return this.questions.map((q, i) => {
        const options = this.needOptions(q.qType) ? q._options.map(o => {
          const row = { label: o.label, value: o.value || o.label }
          if (q.qType === 'image_radio' || q.qType === 'image_checkbox') row.imageUrl = o.imageUrl || ''
          if (q.qType === 'cascade_select') {
            row.children = (o.children || []).map(c => ({
              label: c.label,
              value: c.value || c.label
            })).filter(c => c.label)
          }
          return row
        }) : null
        const jumps = []
        if (this.canJump(q.qType) && q._options) {
          q._options.forEach(o => {
            if (o._toSort !== null && o._toSort !== undefined && o._toSort !== '') {
              jumps.push({ value: o.value || o.label, toSort: Number(o._toSort) })
            }
          })
        }
        const props = { placeholder: q._placeholder || '', jumps }
        if (q._minLength) props.minLength = q._minLength
        if (q._maxLength) props.maxLength = q._maxLength
        if (q.qType === 'rate') props.max = q._max || 5
        if (q.qType === 'nps') { props.min = 0; props.max = 10; props.leftLabel = q._leftLabel || '不可能'; props.rightLabel = q._rightLabel || '非常可能' }
        if (q.qType === 'number' || q.qType === 'slider') {
          if (q._min != null && q._min !== '') props.min = q._min
          if (q._max != null && q._max !== '') props.max = q._max
          if (q._step != null) props.step = q._step
        }
        if (q.qType === 'phone') props.format = 'phone'
        if (q.qType === 'email') props.format = 'email'
        if (q.qType === 'url') props.format = 'url'
        if (q.qType === 'idcard') props.format = 'idcard'
        if (q.qType === 'file') props.maxSizeMb = q._maxSizeMb || 5
        if (q.qType === 'section') props.content = q._content || ''
        if (q.qType === 'agreement') {
          props.content = q._content || ''
          props.agreeLabel = q._agreeLabel || '我已阅读并同意'
        }
        if (q.qType === 'signature') {
          props.penColor = q._penColor || '#111111'
          props.padHeight = q._padHeight || 160
          if (q._bindAgreementSort !== null && q._bindAgreementSort !== undefined && q._bindAgreementSort !== '') {
            props.bindAgreementSort = Number(q._bindAgreementSort)
            props.visibleIf = { sourceSort: Number(q._bindAgreementSort), value: '1' }
          }
        }
        if (q.qType === 'matrix_radio') {
          props.rows = (q._rows || []).map((r, ri) => ({
            label: r.label || ('陈述' + (ri + 1)),
            value: r.value || ('r' + (ri + 1))
          }))
        }
        if (q.qType !== 'signature' && q._visibleIfSource !== null && q._visibleIfSource !== undefined && q._visibleIfSource !== '' && String(q._visibleIfValue || '') !== '') {
          props.visibleIf = { sourceSort: Number(q._visibleIfSource), value: String(q._visibleIfValue) }
        }
        return {
          qType: q.qType,
          title: (q.title || '').trim(),
          required: this.isDisplayOnly(q.qType) ? '0' : (q.required || '0'),
          optionsJson: options ? JSON.stringify(options) : null,
          propsJson: JSON.stringify(props),
          sort: i
        }
      })
    },
    validateQuestions() {
      if (!this.questions.length) {
        this.$modal.msgError('至少添加一道题目')
        return false
      }
      if (!this.questions.some(q => !this.isDisplayOnly(q.qType))) {
        this.$modal.msgError('至少添加一道可作答题（说明段落与分页符不算）')
        return false
      }
      for (const q of this.questions) {
        if (q.qType === 'page_break') continue
        if (!q.title || !q.title.trim()) {
          this.$modal.msgError('请填写所有题干')
          return false
        }
        if (q.qType === 'agreement' && !(q._content || '').replace(/<[^>]+>/g, '').trim()) {
          this.$modal.msgError('请填写协议正文：' + (q.title || '协议同意'))
          return false
        }
        if (this.needOptions(q.qType) && (!q._options || !q._options.length)) {
          this.$modal.msgError('选择题请配置选项')
          return false
        }
        if (this.needRows(q.qType) && (!q._rows || !q._rows.length)) {
          this.$modal.msgError('矩阵题请配置行')
          return false
        }
      }
      return true
    },
    handleSave() {
      if (!this.validateQuestions()) return Promise.reject(new Error('invalid'))
      this.saving = true
      return updateSurvey({
        surveyId: this.surveyId,
        surveyName: this.survey.surveyName,
        surveyDesc: this.survey.surveyDesc
      }).then(() => saveSurveyQuestions(this.surveyId, this.buildPayload())).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.savePulse = true
        setTimeout(() => { this.savePulse = false }, 1600)
        return this.loadDetail()
      }).finally(() => { this.saving = false })
    },
    saveForWizard() {
      return this.handleSave()
    }
  }
}
</script>

<style scoped>
.survey-studio {
  --ss-blue: #2b6de5;
  /* 勿用负 margin：本页无 app-container，-20px 会顶进固定顶栏 */
  height: calc(100vh - var(--fixed-header-height, 90px));
  max-height: calc(100vh - var(--fixed-header-height, 90px));
  display: flex;
  flex-direction: column;
  background: #eef2f8;
  margin: 0;
  overflow: hidden;
  font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}
.survey-studio.embedded {
  height: calc(100vh - 280px);
  max-height: calc(100vh - 280px);
  min-height: 480px;
  margin: 0;
  border: 1px solid #e5eaf3;
  border-radius: 12px;
  overflow: hidden;
}

.studio-top {
  min-height: 56px;
  height: auto;
  flex-shrink: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 12px 16px;
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #e8ecf2;
  box-sizing: border-box;
  z-index: 2;
}
.top-left, .top-right { display: flex; align-items: center; gap: 12px; min-width: 0; }
.top-right { justify-content: flex-end; gap: 10px; }
.back-btn { color: #4e5969 !important; padding: 0 4px !important; }
.title-input {
  border: 0; outline: none; background: transparent;
  font-size: 15px; font-weight: 650; color: #1f2329;
  max-width: 240px; min-width: 0;
}
.save-state { font-size: 12px; color: #94a3b8; white-space: nowrap; }
.save-state.dirty { color: #e6a23c; }
.top-tabs {
  display: flex; gap: 2px; background: #f2f4f7; padding: 4px;
  border-radius: 10px; flex-shrink: 0;
}
.tab {
  border: 0; background: transparent; height: 32px; padding: 0 18px;
  border-radius: 8px; color: #4e5969; cursor: pointer; font-size: 13px;
  white-space: nowrap;
}
.tab.on { background: #fff; color: var(--ss-blue); font-weight: 650; box-shadow: 0 1px 4px rgba(15,23,42,.06); }

.studio-body {
  flex: 1 1 auto;
  min-height: 0;
  height: 0;
  display: grid;
  grid-template-columns: 268px minmax(0, 1fr) 300px;
  grid-template-rows: minmax(0, 1fr);
  overflow: hidden;
}
.studio-body > * {
  min-height: 0;
  min-width: 0;
}

.left-panel {
  background: #fff;
  border-right: 1px solid #e8ecf2;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}
.left-nav {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  padding: 12px 12px 10px;
  border-bottom: 1px solid #eef1f6;
  background: #fafbfd;
}
.left-nav button {
  border: 0;
  background: transparent;
  border-radius: 10px;
  height: 40px;
  color: #86909c;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: background .15s, color .15s;
}
.left-nav button i { font-size: 15px; }
.left-nav button.on {
  color: var(--ss-blue);
  background: #fff;
  font-weight: 650;
  box-shadow: 0 1px 4px rgba(15,23,42,.06);
}
.left-nav button:hover:not(.on) { color: var(--ss-blue); background: #eef4ff; }
.left-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 14px 14px 18px;
  -webkit-overflow-scrolling: touch;
}
.panel-title { font-size: 14px; font-weight: 700; margin-bottom: 12px; color: #1f2329; }
.panel-title em { font-style: normal; color: #86909c; font-weight: 500; margin-left: 6px; }
.type-group { margin-bottom: 16px; }
.group-name { font-size: 12px; color: #86909c; margin-bottom: 8px; }
.type-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.type-chip {
  border: 1px solid #e8ecf2; background: #fafbfd; border-radius: 10px;
  padding: 12px 10px; cursor: pointer; text-align: left;
  display: flex; align-items: center; gap: 8px; font-size: 12px; color: #334155;
}
.type-chip:hover { border-color: #93c5fd; color: var(--ss-blue); background: #f0f7ff; }
.type-chip i { font-size: 14px; }
.outline-item {
  display: flex; align-items: center; gap: 6px; padding: 8px;
  border-radius: 8px; cursor: pointer; font-size: 12px; margin-bottom: 4px;
}
.outline-item.on, .outline-item:hover { background: #eef4ff; }
.ol-drag { cursor: move; color: #c0c4cc; }
.ol-no { color: #86909c; font-weight: 650; }
.ol-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.canvas {
  height: 100%;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  padding: 24px 20px 40px;
  background:
    radial-gradient(circle at 20% 10%, rgba(59,130,246,.06), transparent 40%),
    linear-gradient(180deg, #e9eef7 0%, #eef2f8 100%);
}
.canvas-sheet {
  max-width: 720px;
  margin: 0 auto;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 10px 36px rgba(15, 23, 42, 0.08);
  padding: 28px 28px 20px;
  border: 1px solid #e8ecf2;
}
.sheet-head { margin-bottom: 18px; padding: 8px; border-radius: 10px; }
.sheet-head.on { background: #f8fbff; box-shadow: inset 0 0 0 1px #bfdbfe; }
.sheet-title {
  width: 100%; border: 0; outline: none; font-size: 24px; font-weight: 750;
  color: #0f172a; margin-bottom: 8px; background: transparent;
}
.sheet-desc {
  width: 100%; border: 0; outline: none; resize: none; font-size: 13px;
  color: #64748b; line-height: 1.6; background: transparent;
}
.q-block {
  border: 1px solid transparent; border-radius: 12px; padding: 12px;
  margin-bottom: 12px; transition: border-color .15s, box-shadow .15s;
}
.q-block:hover { border-color: #e2e8f0; }
.q-block.on {
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(43, 109, 229, 0.12);
  background: #fbfdff;
}
.q-toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.q-drag { cursor: move; color: #94a3b8; }
.q-no { font-size: 12px; font-weight: 700; color: #64748b; }
.q-actions { margin-left: auto; display: flex; }
.q-title-row { display: flex; gap: 6px; align-items: flex-start; margin-bottom: 10px; }
.req { color: #f53f3f; line-height: 32px; font-weight: 700; }
.q-preview { margin-top: 4px; }
.opt-edit { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.opt-mark { width: 18px; color: #94a3b8; text-align: center; }
.muted-box {
  padding: 12px; border-radius: 8px; background: #f8fafc; color: #94a3b8; font-size: 12px;
}
.add-bar { display: flex; gap: 10px; justify-content: center; margin: 16px 0 8px; flex-wrap: wrap; }
.sheet-end {
  margin-top: 20px; padding: 20px; text-align: center;
  border-top: 1px dashed #e5eaf3; color: #94a3b8;
}
.end-title { font-size: 14px; font-weight: 650; color: #64748b; }
.end-sub { margin-top: 4px; font-size: 12px; }

.right-panel {
  background: #fff;
  border-left: 1px solid #e8ecf2;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}
.rp-tabs {
  display: flex; border-bottom: 1px solid #e8ecf2; padding: 0 8px;
}
.rp-tabs button {
  flex: 1; height: 44px; border: 0; background: transparent; cursor: pointer;
  color: #86909c; font-size: 13px; position: relative;
}
.rp-tabs button.on { color: var(--ss-blue); font-weight: 650; }
.rp-tabs button.on::after {
  content: ''; position: absolute; left: 20%; right: 20%; bottom: 0; height: 2px;
  background: var(--ss-blue); border-radius: 2px;
}
.rp-tabs button:disabled { opacity: .4; cursor: not-allowed; }
.rp-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 14px;
}
.rp-body.empty { display: flex; align-items: center; justify-content: center; }
.opt-row { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; flex-wrap: wrap; }
.opt-row.stacked { flex-direction: column; align-items: stretch; }
.opt-row.child-row { padding-left: 12px; }
.cascade-block { margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px dashed #e5e7eb; }
.mt8 { margin-top: 8px; }
.mb8 { margin-bottom: 8px; }
.mb12 { margin-bottom: 12px; }
.hint { margin-top: 6px; color: #94a3b8; font-size: 12px; }
.outline-item.is-break .ol-title { color: #64748b; font-style: italic; }
.page-break-bar {
  display: flex; align-items: center; gap: 10px; margin-bottom: 10px;
}
.page-break-bar .pb-line { flex: 1; height: 0; border-top: 1px dashed #94a3b8; }
.page-break-bar .pb-label {
  font-size: 12px; font-weight: 650; color: #64748b;
  padding: 2px 10px; border-radius: 999px; background: #f1f5f9;
}
.agreement-preview {
  border: 1px solid #e5e7eb; border-radius: 10px; padding: 10px 12px; background: #fafafa;
}
.agree-html {
  max-height: 140px; overflow: auto; margin-bottom: 10px; font-size: 13px; line-height: 1.55; color: #334155;
}
.agree-html >>> p { margin: 0 0 8px; }
.agree-sign-preview {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #e5e7eb;
  cursor: pointer;
}
.agree-sign-title {
  font-size: 12px;
  font-weight: 650;
  color: #334155;
  margin-bottom: 6px;
}

@media (max-width: 1200px) {
  .studio-body { grid-template-columns: 240px minmax(0, 1fr) 280px; }
}
@media (max-width: 960px) {
  .survey-studio {
    height: calc(100vh - var(--fixed-header-height, 90px));
    max-height: calc(100vh - var(--fixed-header-height, 90px));
    margin: 0;
  }
  .survey-studio.embedded {
    height: calc(100vh - 300px);
    max-height: calc(100vh - 300px);
  }
  .studio-body {
    grid-template-columns: 1fr;
    grid-template-rows: auto minmax(0, 1fr) auto;
  }
  .left-panel, .right-panel { border: 0; border-top: 1px solid #e8ecf2; max-height: 240px; }
  .canvas { min-height: 280px; }
  .studio-top {
    grid-template-columns: 1fr;
    min-height: 0;
    height: auto;
    padding: 12px 16px;
    gap: 10px;
  }
  .top-tabs { justify-self: start; }
  .top-right { justify-content: flex-start; flex-wrap: wrap; }
}

.save-state.saving { color: var(--ss-blue); }
.save-state.saved { color: #16a34a; }
.type-chip:active { transform: scale(.96); background: #e8effc; }
.q-block.flash {
  animation: qFlash .9s ease;
}
@keyframes qFlash {
  0% { box-shadow: 0 0 0 0 rgba(43,109,229,.45); }
  40% { box-shadow: 0 0 0 4px rgba(43,109,229,.28); }
  100% { box-shadow: 0 0 0 0 rgba(43,109,229,0); }
}
.canvas-empty {
  text-align: center; padding: 56px 20px 40px; color: #64748b;
}
.canvas-empty .empty-ico {
  width: 56px; height: 56px; margin: 0 auto 12px; border-radius: 16px;
  background: #eef2ff; color: var(--ss-blue); display: flex; align-items: center; justify-content: center;
  font-size: 26px;
}
.canvas-empty h3 { margin: 0 0 6px; font-size: 16px; color: #1f2329; }
.canvas-empty p { margin: 0 0 16px; font-size: 13px; }
.q-wrap { position: relative; }
.insert-gap {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  width: 100%; height: 28px; margin: 4px 0 10px; border: 0; background: transparent;
  color: transparent; cursor: pointer; border-radius: 8px; font-size: 12px;
  transition: color .15s, background .15s, height .15s;
}
.insert-gap:hover {
  color: var(--ss-blue); background: rgba(43,109,229,.06); height: 32px;
}
.insert-gap i { font-size: 12px; }
</style>
