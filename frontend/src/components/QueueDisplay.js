import React, { useState, useEffect } from "react";
import "../styles/QueueDisplay.css";

export default function QueueDisplay({ orders, baristas }) {
  const [expandedOrder, setExpandedOrder] = useState(null);

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

  const getWaitTime = (createdAt) => {
    const now = new Date();
    const created = new Date(createdAt);
    const diff = Math.floor((now - created) / 1000); // seconds
    if (diff < 60) return `${diff}s`;
    return `${Math.floor(diff / 60)}m ${diff % 60}s`;
  };

  const getPriorityColor = (score) => {
    if (score >= 70) return "#e74c3c"; // Red - high priority
    if (score >= 40) return "#f39c12"; // Orange - medium
    return "#27ae60"; // Green - low
  };

  const waitingOrders = orders.filter((o) => o.status === "WAITING");
  const sortedOrders = [...waitingOrders].sort(
    (a, b) => (b.priorityScore || 0) - (a.priorityScore || 0)
  );

  return (
    <div className="queue-display-container">
      <h2>📊 Order Queue</h2>
      <div className="queue-stats">
        <div className="stat">
          <span className="stat-label">Total Waiting</span>
          <span className="stat-value">{waitingOrders.length}</span>
        </div>
        <div className="stat">
          <span className="stat-label">Avg Wait</span>
          <span className="stat-value">
            {waitingOrders.length === 0
              ? "0s"
              : Math.round(
                waitingOrders.reduce((sum, o) => {
                  const now = new Date();
                  const created = new Date(o.createdAt);
                  return sum + (now - created) / 1000;
                }, 0) / waitingOrders.length
              ) + "s"}
          </span>
        </div>
      </div>

      {sortedOrders.length === 0 ? (
        <div className="empty-queue">
          <p>☕ No waiting orders - all caught up!</p>
        </div>
      ) : (
        <div className="queue-list">
          {sortedOrders.map((order, index) => {
            const waitSeconds = order.createdAt
              ? Math.floor((new Date() - new Date(order.createdAt)) / 1000)
              : 0;
            const isEmergency = waitSeconds >= 480; // 8 minutes
            const isApproachingTimeout = waitSeconds >= 540; // 9 minutes

            return (
              <div
                key={order.id}
                className={`queue-item ${expandedOrder === order.id ? "expanded" : ""
                  } ${isEmergency ? "emergency" : ""} ${isApproachingTimeout ? "timeout-warning" : ""}`}
                onClick={() =>
                  setExpandedOrder(expandedOrder === order.id ? null : order.id)
                }
                style={{
                  borderLeftColor: getPriorityColor(order.priorityScore || 0),
                }}
              >
                <div className="queue-item-header">
                  <div className="position-badge">#{index + 1}</div>
                  <div className="order-info">
                    <div className="customer-name">
                      {order.customerName || "Guest"}
                      {isEmergency && <span className="emergency-badge">⚠️ URGENT</span>}
                    </div>
                    <div className="order-details">
                      {getDrinkNames(order.drinks)} • {order.totalPrepTime}min
                    </div>
                    {order.priorityReason && (
                      <div className="priority-reason">{order.priorityReason}</div>
                    )}
                  </div>
                  <div
                    className="priority-badge"
                    style={{
                      backgroundColor: getPriorityColor(order.priorityScore || 0),
                    }}
                  >
                    {order.priorityScore || 0}
                  </div>
                  <div className="wait-time">
                    <div>⏱ {getWaitTime(order.createdAt)}</div>
                    {order.estimatedWaitTime && (
                      <div className="est-wait">
                        Est: {Math.ceil(order.estimatedWaitTime / 60)}m
                      </div>
                    )}
                  </div>
                </div>

                {expandedOrder === order.id && (
                  <div className="queue-item-details">
                    <div className="detail-row">
                      <span>Order ID:</span> <strong>#{order.id}</strong>
                    </div>
                    <div className="detail-row">
                      <span>Status:</span> <strong>{order.status}</strong>
                    </div>
                    <div className="detail-row">
                      <span>Assigned to:</span>
                      <strong>
                        {order.assignedToBarista
                          ? `Barista ${order.assignedToBarista}`
                          : "Unassigned"}
                      </strong>
                    </div>
                    <div className="detail-row">
                      <span>Priority Score:</span>{" "}
                      <strong>{order.priorityScore || 0}/100</strong>
                    </div>
                    <div className="detail-row">
                      <span>Loyalty:</span> <strong>{order.loyaltyStatus}</strong>
                    </div>
                    {order.hoursBeatenBy > 0 && (
                      <div className="detail-row warning">
                        <span>
                          ⚠️ {order.hoursBeatenBy} later orders served first
                        </span>
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
