import axios from 'axios';

const setupInterceptors = () => {
  axios.interceptors.request.use(
    (config) => {
      const user = JSON.parse(localStorage.getItem('user'));
      if (user && user.token) {
        // 为请求添加Authorization头部
        config.headers['Authorization'] = `Bearer ${user.token}`;
      }
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  axios.interceptors.response.use(
    (res) => {
      return res;
    },
    async (err) => {
      const originalConfig = err.config;

      // 如果是401错误（未授权）且不是登录请求，则清除用户信息并跳转到登录页
      if (err.response.status === 401 && !originalConfig._retry && !originalConfig.url.includes('/auth/login')) {
        localStorage.removeItem('user');
        window.location.href = '/login';
      }

      return Promise.reject(err);
    }
  );
};

export default setupInterceptors; 