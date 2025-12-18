import axios from 'axios';

// 创建一个基础的axios实例
const instance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 30 * 60 * 1000,
  withCredentials: true
});

// 导出默认实例，以防有其他地方引用
export default instance;
