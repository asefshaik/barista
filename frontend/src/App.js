import React, { useState, useEffect, useCallback } from "react";
import "./App.css";
import OrderForm from "./components/OrderForm";
import QueueDisplay from "./components/QueueDisplay";
import BaristaStatus from "./components/BaristaStatus";
import Statistics from "./components/Statistics";
import ManagementPanel from "./components/ManagementPanel";
import SimulationStats from "./components/SimulationStats";
import { WebSocketService } from "./services/api";

function App() {
  const [orders, setOrders] = useState([]);
  const [baristas, setBaristas] = useState([]);
  const [stats, setStats] = useState({});
  const [activeTab, setActiveTab] = useState("customer");
  // Fetch initial data
  useEffect(() => {
    fetchAllData();

    const refreshInterval = setInterval(fetchAllData, 5000);
    return () => clearInterval(refreshInterval);
  }, []);

  const fetchAllData = async () => {
    try {
      const [waitingRes, preparingRes, completedRes, baristasRes, statsRes, readyRes] = await Promise.all([
        fetch("http://localhost:8081/api/orders/waiting"),
        fetch("http://localhost:8081/api/orders/preparing"),
        fetch("http://localhost:8081/api/orders/completed"),
        fetch("http://localhost:8081/api/baristas"),
        fetch("http://localhost:8081/api/stats"),
        fetch("http://localhost:8081/api/orders/ready"),
      ]);

      let allOrders = [];
      if (waitingRes.ok) {
        const waitingData = await waitingRes.json();
        allOrders = allOrders.concat(waitingData);
      }
      if (preparingRes.ok) {
        const preparingData = await preparingRes.json();
        allOrders = allOrders.concat(preparingData);
      }
      if (readyRes.ok) {
        const readyData = await readyRes.json();
        allOrders = allOrders.concat(readyData);
      }
      if (completedRes.ok) {
        const completedData = await completedRes.json();
        allOrders = allOrders.concat(completedData);
      }


      // Only update state if data has actually changed (prevents blinking)
      setOrders(prevOrders => {
        const newIds = allOrders.map(o => `${o.id}-${o.status}`).sort().join('|');
        const oldIds = prevOrders.map(o => `${o.id}-${o.status}`).sort().join('|');
        return newIds !== oldIds ? allOrders : prevOrders;
      });

      if (baristasRes.ok) {
        const baristasData = await baristasRes.json();
        setBaristas(prevBaristas => {
          const newStr = JSON.stringify(baristasData.map(b => ({
            id: b.id,
            busy: b.currentOrderId,
            completed: b.completedOrders,
            workTime: b.totalWorkTime,
            workload: b.workloadRatio
          })));
          const oldStr = JSON.stringify(prevBaristas.map(b => ({
            id: b.id,
            busy: b.currentOrderId,
            completed: b.completedOrders,
            workTime: b.totalWorkTime,
            workload: b.workloadRatio
          })));
          return newStr !== oldStr ? baristasData : prevBaristas;
        });
      }

      if (statsRes.ok) {
        const statsData = await statsRes.json();
        setStats(prevStats => {
          const newStr = JSON.stringify(statsData);
          const oldStr = JSON.stringify(prevStats);
          return newStr !== oldStr ? statsData : prevStats;
        });
      }
    } catch (error) {
      console.error("Error fetching data:", error);
    }
  };

  const handleOrderCreated = useCallback(async () => {
    await fetchAllData();
  }, []);

  const handleStartOrder = useCallback(async (orderId) => {
    try {
      console.log("[FRONTEND] Starting order:", orderId);
      const response = await fetch(
        `http://localhost:8081/api/orders/${orderId}/start`,
        {
          method: "PUT",
        }
      );
      console.log("[FRONTEND] Start order response status:", response.status);
      const responseData = await response.json();
      console.log("[FRONTEND] Response data:", responseData);

      if (response.ok) {
        console.log("[FRONTEND] Order started successfully, refreshing data");
        await fetchAllData();
      } else {
        console.error("[FRONTEND] Failed to start order:", responseData);
      }
    } catch (error) {
      console.error("[FRONTEND] Error starting order:", error);
    }
  }, []);

  const handleCompleteOrder = useCallback(async (orderId) => {
    try {
      console.log("[FRONTEND] Completing order:", orderId);
      const response = await fetch(
        `http://localhost:8081/api/orders/${orderId}/complete`,
        {
          method: "PUT",
        }
      );
      console.log(
        "[FRONTEND] Complete order response status:",
        response.status
      );
      const responseData = await response.json();
      console.log("[FRONTEND] Complete response data:", responseData);

      if (response.ok) {
        console.log("[FRONTEND] Order completed successfully, refreshing data");
        await fetchAllData();
      } else {
        console.error("[FRONTEND] Failed to complete order:", responseData);
      }
    } catch (error) {
      console.error("[FRONTEND] Error completing order:", error);
    }
  }, []);

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-content">
          <h1>☕ Barista Queue Management System</h1>
          <div className="header-subtitle">
            Smart Order Scheduling for Bean & Brew Café
          </div>
        </div>
      </header>

      <nav className="app-nav">
        <button
          className={`nav-btn ${activeTab === "customer" ? "active" : ""}`}
          onClick={() => setActiveTab("customer")}
        >
          👤 Customer View
        </button>
        <button
          className={`nav-btn ${activeTab === "queue" ? "active" : ""}`}
          onClick={() => setActiveTab("queue")}
        >
          📊 Queue Monitor
        </button>
        <button
          className={`nav-btn ${activeTab === "barista" ? "active" : ""}`}
          onClick={() => setActiveTab("barista")}
        >
          👨‍🍳 Barista View
        </button>
        <button
          className={`nav-btn ${activeTab === "management" ? "active" : ""}`}
          onClick={() => setActiveTab("management")}
        >
          🎛️ Management
        </button>
        <button
          className={`nav-btn ${activeTab === "stats" ? "active" : ""}`}
          onClick={() => setActiveTab("stats")}
        >
          📈 Statistics
        </button>
        <button
          className={`nav-btn ${activeTab === "simulation" ? "active" : ""}`}
          onClick={() => setActiveTab("simulation")}
        >
          🧪 Simulation
        </button>
      </nav>

      <main className="app-main">
        {activeTab === "customer" && (
          <div className="tab-content">
            <OrderForm onOrderCreated={handleOrderCreated} />
          </div>
        )}

        {activeTab === "queue" && (
          <div className="tab-content">
            <QueueDisplay orders={orders} baristas={baristas} />
          </div>
        )}

        {activeTab === "barista" && (
          <div className="tab-content">
            <BaristaStatus baristas={baristas} orders={orders} />
          </div>
        )}

        {activeTab === "management" && (
          <div className="tab-content">
            <ManagementPanel
              orders={orders}
              baristas={baristas}
              onStartOrder={handleStartOrder}
              onCompleteOrder={handleCompleteOrder}
            />
          </div>
        )}

        {activeTab === "stats" && (
          <div className="tab-content">
            <Statistics stats={stats} />
          </div>
        )}

        {activeTab === "simulation" && (
          <div className="tab-content">
            <SimulationStats />
          </div>
        )}
      </main>

      <footer className="app-footer">
        <p>
          Smart Queue Management System | Operating Hours: 7-10 AM | 3 Baristas
          | Avg Wait: 4.8 min
        </p>
      </footer>
    </div>
  );
}

export default App;
