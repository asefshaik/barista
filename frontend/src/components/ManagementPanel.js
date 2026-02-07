import React, { memo } from "react";
import "../styles/ManagementPanel.css";

function ManagementPanel({
  orders,
  baristas,
  onStartOrder,
  onCompleteOrder,
}) {
  const preparingOrders = orders.filter((o) => o.status === "PREPARING");
  // Sort ready orders by completedAt in reverse (newest first)
  const readyOrders = orders
    .filter((o) => o.status === "READY" || o.status === "COMPLETED")
    .sort((a, b) => {
      const timeA = a.completedAt ? new Date(a.completedAt) : new Date(0);
      const timeB = b.completedAt ? new Date(b.completedAt) : new Date(0);
      return timeB - timeA; // Newest first
    });

  // Find orders approaching timeout (8+ minutes waiting)
  const urgentOrders = orders.filter((o) => {
    if (o.status !== "WAITING") return false;
    const waitSeconds = o.createdAt
      ? Math.floor((new Date() - new Date(o.createdAt)) / 1000)
      : 0;
    return waitSeconds >= 480; // 8 minutes
  });

  const getDrinkNames = (drinkCodes) => {
    if (!drinkCodes || drinkCodes.length === 0) return "No drinks";
    const drinkMap = {
      COLD_BREW: "Cold Brew",
      ESPRESSO: "Espresso",
      AMERICANO: "Americano",
      CAPPUCCINO: "Cappuccino",
      LATTE: "Latte",
      SPECIALTY_MOCHA: "Mocha",
    };
    return drinkCodes.map((d) => drinkMap[d] || d).join(", ");
  };

  return (
    <div className="management-panel-container">
      <h2>🎛️ Management Panel</h2>

      {/* Manager Alerts Section */}
      {urgentOrders.length > 0 && (
        <div className="manager-alerts">
          <h3>🚨 Urgent Alerts ({urgentOrders.length})</h3>
          <div className="alert-list">
            {urgentOrders.map((order) => {
              const waitSeconds = order.createdAt
                ? Math.floor((new Date() - new Date(order.createdAt)) / 1000)
                : 0;
              const waitMins = Math.floor(waitSeconds / 60);
              return (
                <div key={order.id} className="alert-item">
                  <span className="alert-icon">⚠️</span>
                  <span>Order #{order.id} ({order.customerName || "Guest"}) waiting {waitMins} min!</span>
                  <button
                    onClick={() => onStartOrder(order.id)}
                    className="action-btn urgent-btn"
                  >
                    Start Now
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}

      <div className="management-columns">
        {/* Waiting Orders */}
        <div className="management-column">
          <h3>📋 Waiting Orders</h3>
          <div className="orders-list">
            {orders
              .filter((o) => o.status === "WAITING")
              .slice(0, 10)
              .map((order) => (
                <div key={order.id} className="order-item">
                  <div className="order-header">
                    <span className="order-id">#{order.id}</span>
                    <span className="customer">
                      {order.customerName || "Guest"}
                    </span>
                  </div>
                  <div className="order-drinks">
                    {getDrinkNames(order.drinks)}
                  </div>
                  {order.priorityReason && (
                    <div className="priority-reason">{order.priorityReason}</div>
                  )}
                  <button
                    onClick={() => {
                      console.log(
                        "[ManagementPanel] Start Making clicked for order:",
                        order.id
                      );
                      onStartOrder(order.id);
                    }}
                    className="action-btn start-btn"
                  >
                    Start Making
                  </button>
                </div>
              ))}
          </div>
        </div>

        {/* Preparing Orders */}
        <div className="management-column">
          <h3>👨‍🍳 Preparing ({preparingOrders.length})</h3>
          <div className="orders-list">
            {preparingOrders.map((order) => {
              const barista = baristas.find(
                (b) => b.id === order.assignedToBarista
              );
              return (
                <div key={order.id} className="order-item preparing">
                  <div className="order-header">
                    <span className="order-id">#{order.id}</span>
                    <span className="barista-label">
                      {barista ? barista.name : "Unknown"}
                    </span>
                  </div>
                  <div className="order-drinks">
                    {getDrinkNames(order.drinks)}
                  </div>
                  <button
                    onClick={() => onCompleteOrder(order.id)}
                    className="action-btn complete-btn"
                  >
                    Mark Ready
                  </button>
                </div>
              );
            })}
          </div>
        </div>

        {/* Ready Orders */}
        <div className="management-column">
          <h3>✅ Ready for Pickup ({readyOrders.length})</h3>
          <div className="orders-list">
            {readyOrders.map((order) => (
              <div key={order.id} className="order-item completed">
                <div className="order-header">
                  <span className="order-id">#{order.id}</span>
                  <span className="customer">
                    {order.customerName || "Guest"}
                  </span>
                </div>
                <div className="order-drinks">
                  {getDrinkNames(order.drinks)}
                </div>
                <div className="ready-badge">Ready! ☕</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// Only re-render when orders change to prevent unnecessary flickering
function areEqual(prevProps, nextProps) {
  // Use a more robust comparison: check count and status of all orders
  if (prevProps.orders.length !== nextProps.orders.length) return false;

  const prevStr = prevProps.orders.map(o => `${o.id}-${o.status}`).sort().join('|');
  const nextStr = nextProps.orders.map(o => `${o.id}-${o.status}`).sort().join('|');

  return prevStr === nextStr;
}

export default memo(ManagementPanel, areEqual);
