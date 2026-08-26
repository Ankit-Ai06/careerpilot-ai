import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080/api",
});

api.interceptors.request.use(
  (config) => {
    const publicEndpoints = [
      "/auth/login",
      "/auth/register",
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

// If the backend says our token is invalid/expired, don't leave the
// user staring at silent failures across every page - log them out
// and send them back to /login with the reason preserved.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    const isAuthRequest = ["/auth/login", "/auth/register"].some((endpoint) =>
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