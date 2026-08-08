<template>
  <div class="page">
    <div class="hero">
      <h1>{{ siteName }}</h1>
      <p>轻量公开端 · 查询 / 问卷</p>
    </div>
    <div class="card home-links">
      <p class="tip">请使用管理端发布后的短链访问（路径形如 /h5/q/xxx、/h5/s/xxx）。</p>
    </div>
  </div>
</template>

<script setup>
const siteName = import.meta.env.VITE_SITE_NAME || '通查云'
</script>
