<template>
  <div class="home-page">
    <div class="home-hero">
      <h1 class="home-title">Auraspark</h1>
      <p class="home-subtitle">Your stories carry their own light</p>
    </div>
    <div class="home-cards">
      <router-link to="/notes" class="home-card">
        <span class="card-icon">N</span>
        <span class="card-text">{{ t('home.notes') }}</span>
      </router-link>
      <router-link to="/files" class="home-card">
        <span class="card-icon">F</span>
        <span class="card-text">{{ t('home.files') }}</span>
      </router-link>
      <router-link to="/chat" class="home-card">
        <span class="card-icon">C</span>
        <span class="card-text">{{ t('home.chat') }}</span>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { t } from '@/i18n'
</script>

<style scoped>
.home-page { padding: 48px; display: flex; flex-direction: column; align-items: center; }
.home-hero { text-align: center; margin-bottom: 48px; margin-top: 40px; }
.home-title { font-size: 36px; font-weight: 200; letter-spacing: 10px; color: #111; margin-bottom: 8px; }
.home-subtitle { font-size: 13px; color: #aaa; letter-spacing: 3px; font-weight: 300; }
.home-cards { display: flex; gap: 24px; }
.home-card { width: 160px; padding: 32px 24px; border: 1px solid #e8e8e8; display: flex; flex-direction: column; align-items: center; gap: 16px; text-decoration: none; color: #000; transition: box-shadow 0.25s ease; }
.home-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.08); }
.card-icon { font-size: 24px; color: #999; }
.card-text { font-size: 14px; letter-spacing: 1px; }
</style>
