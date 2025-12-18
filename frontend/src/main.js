

import { createApp } from 'vue';
import { createPinia } from 'pinia';
import axios from 'axios';
import App from './App.vue';
import router from './common/router';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import '@/assets/styles/element-override.css';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '';
axios.defaults.baseURL = apiBaseUrl;
axios.defaults.withCredentials = true;
axios.defaults.timeout = 30 * 60 * 1000; // 30分钟，避免长耗时请求超时


const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(ElementPlus);
app.mount('#app');