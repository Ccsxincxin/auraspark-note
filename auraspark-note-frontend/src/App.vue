<template>
  <router-view v-slot="{ Component }">
    <Transition name="page" mode="out-in">
      <component :is="Component" />
    </Transition>
  </router-view>
</template>

<style>
.page-enter-active { transition: opacity 0.3s ease; }
.page-leave-active { transition: opacity 0.2s ease; }
.page-enter-from { opacity: 0; }
.page-leave-to { opacity: 0; }
</style>
