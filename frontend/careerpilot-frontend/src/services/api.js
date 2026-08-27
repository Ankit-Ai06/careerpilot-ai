import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
});

api.interceptors.request.use(
  (config) => {
    const publicEndpoints = [
      "/api/auth/login",
      "/api/auth/register",
    ];

    const isPublicEndpoint = publicEndpoints.some((endpoint) =>
      config.url?.startsWith(endpoint)
    );

    if (!isPublicEndpoint) {
      const token = localStorage.getItem("token");

      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }

    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    const isAuthRequest = [
      "/api/auth/login",
      "/api/auth/register",
    ].some((endpoint) =>
      error.config?.url?.startsWith(endpoint)
    );

    if (status === 401 && !isAuthRequest) {
      localStorage.removeItem("token");

      if (window.location.pathname !== "/login") {
        window.location.href = "/login?sessionExpired=1";
      }
    }

    return Promise.reject(error);
  }
);

export default api;