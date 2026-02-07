import axios from "axios";

const API_BASE_URL = "http://localhost:8081/api";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

export const orderApi = {
  createOrder: (orderData) => apiClient.post("/orders/create", orderData),
  getWaitingOrders: () => apiClient.get("/orders/waiting"),
  getPreparingOrders: () => apiClient.get("/orders/preparing"),
  getCompletedOrders: () => apiClient.get("/orders/completed"),
  startOrder: (orderId) => apiClient.put(`/orders/${orderId}/start`),
  completeOrder: (orderId) => apiClient.put(`/orders/${orderId}/complete`),
  getOrder: (orderId) => apiClient.get(`/orders/${orderId}`),
};

export const baristaApi = {
  getAllBaristas: () => apiClient.get("/baristas"),
  getBarista: (id) => apiClient.get(`/baristas/${id}`),
};

export const statsApi = {
  getStats: () => apiClient.get("/stats"),
};

export class WebSocketService {
  constructor() {
    this.socket = null;
    this.connected = false;
    this.onMessageReceived = null;
  }

  connect(onMessageReceived) {
    this.onMessageReceived = onMessageReceived;
    // Polling disabled to favor App.js centralized fetch
    // this.startPolling();
    this.connected = true;
    console.log("WebSocketService connected (idle mode)");
  }

  startPolling() {
    this.pollInterval = setInterval(() => {
      Promise.all([
        apiClient.get("/orders/waiting"),
        apiClient.get("/baristas"),
      ])
        .then(([ordersRes, baristasRes]) => {
          if (this.onMessageReceived) {
            this.onMessageReceived({
              waitingOrders: ordersRes.data,
              baristas: baristasRes.data,
            });
          }
        })
        .catch((err) => {
          console.error("Polling error:", err);
        });
    }, 5000); // Poll every 5 seconds
  }

  disconnect() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
    this.connected = false;
    console.log("Disconnected from API");
  }

  isConnected() {
    return this.connected;
  }
}
