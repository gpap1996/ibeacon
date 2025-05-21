<script setup>
import { RouterLink, RouterView } from 'vue-router';
</script>

<template>
  <div id="app">
    <nav>
      <RouterLink to="/" class="nav-link">AltBeacon</RouterLink> |
      <RouterLink to="/estimote" class="nav-link">Estimote</RouterLink>
    </nav>
    <RouterView />
  </div>
</template>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}

nav {
  padding: 1rem;
  background-color: #f8f9fa;
}

.nav-link {
  color: #2c3e50;
  text-decoration: none;
  padding: 0.5rem 1rem;
  margin: 0 0.5rem;
}

.nav-link:hover {
  color: #42b983;
}

.router-link-active {
  color: #42b983;
  font-weight: bold;
}
</style>
