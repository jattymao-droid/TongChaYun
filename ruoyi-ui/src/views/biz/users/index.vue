<template>
  <div class="app-container user-project-page">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="账号" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="用户账号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickName">
        <el-input v-model="queryParams.nickName" placeholder="用户昵称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="16">
        <span class="page-tip">按用户查看其创建的查询与问卷，可跳转管理或从系统用户进入本页。</span>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="用户编号" prop="userId" width="90" align="center" />
      <el-table-column label="账号" prop="userName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="昵称" prop="nickName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="部门" prop="deptName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="status" width="80" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="查询" align="center" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.queryCount || 0 }}</span>
          <span class="sub">/ {{ scope.row.queryPublished || 0 }} 已发布</span>
        </template>
      </el-table-column>
      <el-table-column label="问卷" align="center" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.surveyCount || 0 }}</span>
          <span class="sub">/ {{ scope.row.surveyPublished || 0 }} 已发布</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-search" @click="goQueries(scope.row)" v-hasPermi="['biz:query:list']">查询</el-button>
          <el-button size="mini" type="text" icon="el-icon-document" @click="goSurveys(scope.row)" v-hasPermi="['biz:survey:list']">问卷</el-button>
          <el-button size="mini" type="text" icon="el-icon-user" @click="goSysUser(scope.row)" v-hasPermi="['system:user:edit']">用户资料</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listUserProjects } from '@/api/biz/userProject'

export default {
  name: 'BizUserProjects',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      list: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        nickName: undefined,
        status: undefined,
        userId: undefined
      }
    }
  },
  created() {
    const uid = this.$route.query.userId
    if (uid) this.queryParams.userId = Number(uid) || uid
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listUserProjects(this.queryParams).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.userId = undefined
      this.handleQuery()
    },
    ownerLabel(row) {
      return row.nickName || row.userName || ('用户' + row.userId)
    },
    goQueries(row) {
      this.$router.push({
        path: '/biz/query',
        query: { createUserId: row.userId, ownerLabel: this.ownerLabel(row) }
      })
    },
    goSurveys(row) {
      this.$router.push({
        path: '/biz/survey',
        query: { createUserId: row.userId, ownerLabel: this.ownerLabel(row) }
      })
    },
    goSysUser(row) {
      this.$router.push({ path: '/system/user', query: { userName: row.userName } })
    }
  }
}
</script>

<style scoped>
.page-tip {
  font-size: 13px;
  color: #64748b;
  line-height: 28px;
}
.sub {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}
</style>
