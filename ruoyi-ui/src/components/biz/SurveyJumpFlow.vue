<template>
  <div class="jump-flow" v-if="nodes.length">
    <div class="flow-head">
      <span class="title">题目流程图</span>
      <span class="sub">展示默认顺序与跳题 / 显隐关系</span>
    </div>
    <div class="flow-scroll">
      <div class="flow-track">
        <template v-for="(node, idx) in nodes">
          <div :key="'n-' + node.key" class="flow-node" :class="node.cls">
            <div class="node-badge">{{ node.badge }}</div>
            <div class="node-main">
              <div class="node-title" :title="node.title">{{ node.title }}</div>
              <div class="node-meta">{{ node.meta }}</div>
            </div>
          </div>
          <div v-if="idx < nodes.length - 1" :key="'e-' + node.key" class="flow-edge-wrap">
            <div class="flow-edge"></div>
            <div v-if="edgeLabels[idx] && edgeLabels[idx].length" class="edge-tags">
              <span v-for="(lab, li) in edgeLabels[idx]" :key="li" class="edge-tag" :class="lab.cls">{{ lab.text }}</span>
            </div>
          </div>
        </template>
      </div>

      <div v-if="jumpEdges.length" class="jump-list">
        <div class="jump-list-title">跳题规则</div>
        <div v-for="(e, i) in jumpEdges" :key="i" class="jump-item">
          <span class="from">Q{{ e.fromNo }}</span>
          <span class="via">选「{{ e.label }}」</span>
          <i class="el-icon-right"></i>
          <span class="to">{{ e.toText }}</span>
        </div>
      </div>
      <div v-else class="jump-empty">未配置跳题：填写时按题目顺序依次显示</div>

      <div v-if="visibleEdges.length" class="jump-list">
        <div class="jump-list-title">显示条件</div>
        <div v-for="(e, i) in visibleEdges" :key="'v' + i" class="jump-item soft">
          <span class="from">Q{{ e.targetNo }}</span>
          <span class="via">仅当 Q{{ e.sourceNo }} = 「{{ e.value }}」时显示</span>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="jump-flow empty">
    <span>暂无题目，无法生成流程图</span>
  </div>
</template>

<script>
export default {
  name: 'SurveyJumpFlow',
  props: {
    questions: { type: Array, default: () => [] }
  },
  computed: {
    normalized() {
      return (this.questions || []).map((q, i) => {
        let props = {}
        try { props = q.propsJson ? JSON.parse(q.propsJson) : (q.props || {}) } catch (e) { props = {} }
        let options = []
        try { options = q.optionsJson ? JSON.parse(q.optionsJson) : (q._options || []) } catch (e) { options = [] }
        return {
          sort: q.sort != null ? Number(q.sort) : i,
          index: i,
          no: i + 1,
          title: q.title || ('题目' + (i + 1)),
          qType: q.qType || 'input',
          jumps: Array.isArray(props.jumps) ? props.jumps : [],
          visibleIf: props.visibleIf || null,
          options
        }
      })
    },
    nodes() {
      const list = this.normalized.map(q => ({
        key: 'q' + q.no,
        badge: 'Q' + q.no,
        title: q.title,
        meta: this.typeLabel(q.qType) + (q.jumps.length ? ' · 含跳题' : ''),
        cls: q.jumps.length ? 'has-jump' : ''
      }))
      list.push({
        key: 'end',
        badge: 'END',
        title: '结束填写',
        meta: '提交答卷',
        cls: 'is-end'
      })
      return list
    },
    edgeLabels() {
      // labels between node i and i+1 (default next); jump details listed below
      const n = this.normalized.length
      const labels = []
      for (let i = 0; i < n; i++) {
        const q = this.normalized[i]
        const labs = []
        if (!q.jumps.length) labs.push({ text: '下一题', cls: '' })
        else labs.push({ text: '默认下一题', cls: 'muted' })
        labels.push(labs)
      }
      // last edge to END
      labels.push([{ text: '完成', cls: '' }])
      return labels
    },
    jumpEdges() {
      const bySort = {}
      this.normalized.forEach(q => { bySort[q.sort] = q })
      const edges = []
      this.normalized.forEach(q => {
        (q.jumps || []).forEach(j => {
          const toSort = j.toSort
          let toText = '下一题'
          if (toSort === -1 || toSort === '-1') toText = '结束填写'
          else if (toSort !== null && toSort !== undefined && toSort !== '') {
            const target = bySort[Number(toSort)]
            toText = target ? ('Q' + target.no + ' ' + target.title) : ('排序 ' + toSort)
          }
          const opt = (q.options || []).find(o => String(o.value) === String(j.value))
          edges.push({
            fromNo: q.no,
            label: (opt && opt.label) || j.value || '-',
            toText
          })
        })
      })
      return edges
    },
    visibleEdges() {
      const bySort = {}
      this.normalized.forEach(q => { bySort[q.sort] = q })
      const list = []
      this.normalized.forEach(q => {
        const v = q.visibleIf
        if (!v || v.sourceSort == null || v.sourceSort === '') return
        const src = bySort[Number(v.sourceSort)]
        list.push({
          targetNo: q.no,
          sourceNo: src ? src.no : Number(v.sourceSort) + 1,
          value: v.value != null ? String(v.value) : ''
        })
      })
      return list
    }
  },
  methods: {
    typeLabel(t) {
      const map = {
        radio: '单选', checkbox: '多选', select: '下拉', input: '填空', textarea: '多行',
        rate: '评分', nps: 'NPS', yesno: '是非', number: '数字', date: '日期',
        datetime: '日期时间', slider: '滑块', phone: '手机', email: '邮箱',
        file: '附件', image_radio: '图片单选', matrix_radio: '矩阵单选',
        cascade_select: '级联', section: '说明'
      }
      return map[t] || t || '题目'
    }
  }
}
</script>

<style scoped>
.jump-flow {
  margin-top: 20px;
  padding: 16px 16px 12px;
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  background: linear-gradient(180deg, #f8fbff 0%, #fff 48%);
}
.jump-flow.empty {
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  padding: 24px;
}
.flow-head { margin-bottom: 14px; }
.flow-head .title { font-size: 15px; font-weight: 650; color: #1f2329; margin-right: 8px; }
.flow-head .sub { font-size: 12px; color: #86909c; }
.flow-scroll { overflow-x: auto; padding-bottom: 4px; }
.flow-track {
  display: flex;
  align-items: flex-start;
  gap: 0;
  min-width: max-content;
  padding: 4px 2px 12px;
}
.flow-node {
  width: 168px;
  min-height: 72px;
  border-radius: 10px;
  border: 1px solid #dbe4f0;
  background: #fff;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.04);
  display: flex;
  gap: 10px;
  padding: 10px;
}
.flow-node.has-jump { border-color: #93c5fd; background: #f8fbff; }
.flow-node.is-end { border-color: #86efac; background: #f0fdf4; }
.node-badge {
  flex-shrink: 0;
  height: 24px;
  min-width: 36px;
  padding: 0 8px;
  border-radius: 999px;
  background: #e8f0fe;
  color: #2b6de5;
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.flow-node.is-end .node-badge { background: #dcfce7; color: #15803d; }
.node-main { min-width: 0; flex: 1; }
.node-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2329;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.node-meta { margin-top: 4px; font-size: 11px; color: #86909c; }
.flow-edge-wrap {
  width: 72px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding-top: 28px;
}
.flow-edge {
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, #93c5fd, #60a5fa);
  position: relative;
}
.flow-edge::after {
  content: '';
  position: absolute;
  right: -1px;
  top: -4px;
  border: 5px solid transparent;
  border-left-color: #60a5fa;
}
.edge-tags {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}
.edge-tag {
  font-size: 11px;
  color: #2b6de5;
  background: #eef4ff;
  border-radius: 999px;
  padding: 1px 8px;
  white-space: nowrap;
}
.edge-tag.muted { color: #86909c; background: #f1f5f9; }
.jump-list { margin-top: 8px; padding-top: 10px; border-top: 1px dashed #e5eaf3; }
.jump-list-title { font-size: 12px; font-weight: 650; color: #4e5969; margin-bottom: 8px; }
.jump-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #4e5969;
  padding: 4px 0;
}
.jump-item .from, .jump-item .to { font-weight: 650; color: #1f2329; }
.jump-item .via { color: #86909c; }
.jump-item.soft .via { color: #64748b; }
.jump-empty { margin-top: 10px; font-size: 12px; color: #94a3b8; }
</style>
