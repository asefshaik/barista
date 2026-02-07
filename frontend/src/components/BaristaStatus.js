import React, { memo } from "react";
import CountdownTimer from "./CountdownTimer";
import "../styles/BaristaStatus.css";

function BaristaStatus({ baristas, orders }) {
  const getOrderForBarista = (baristaId) => {
    return orders.find(
      (o) => o.assignedToBarista === baristaId && o.status === "PREPARING"
    );
  };

  const getWorkloadPercentage = (barista) => {
    if (barista.workloadRatio <= 1.0) return "low";
    if (barista.workloadRatio <= 1.2) return "medium";
    return "high";
  };

  return (
    <div className="barista-status-container">
      <h2>👨‍🍳 Barista Status</h2>
      <div className="baristas-grid">
        {baristas.map((barista) => {
          const currentOrder = getOrderForBarista(barista.id);
          const workloadClass = getWorkloadPercentage(barista);
          const isBusy = currentOrder != null;

          return (
            <div
              key={barista.id}
              className={`barista-card ${isBusy ? "busy" : "available"
                } ${workloadClass}`}
            >
              <div className="barista-header">
                <h3>{barista.name}</h3>
                <div
                  className={`status-indicator ${isBusy ? "busy" : "available"
                    }`}
                >
                  {isBusy ? "🔴 Busy" : "🟢 Available"}
                </div>
              </div>

              <div className="barista-stats">
                <div className="stat">
                  <span>Orders Completed</span>
                  <strong>{barista.completedOrders}</strong>
                </div>
                <div className="stat">
                  <span>Total Work Time</span>
                  <strong>{barista.totalWorkTime} min</strong>
                </div>
                <div className="stat">
                  <span>Workload Ratio</span>
                  <strong>{(barista.workloadRatio || 0).toFixed(2)}x</strong>
                </div>
              </div>

              {isBusy && currentOrder && (
                <div className="current-order">
                  <h4>Currently Making:</h4>
                  <div className="barista-order-card">
                    <div className="barista-customer">
                      {currentOrder.customerName || "Guest"}
                    </div>
                    <div className="barista-drinks">
                      {currentOrder.drinks.join(", ")}
                    </div>
                    {barista.busyUntil && (
                      <CountdownTimer
                        endTime={barista.busyUntil}
                        label="⏱️ Ready in"
                      />
                    )}
                  </div>
                </div>
              )}

              <div className="time-status">
                <span className={`status-badge ${isBusy ? "busy" : "available"}`}>
                  {isBusy ? "🔴 Making Order" : "🟢 Ready for Next Order"}
                </span>
              </div>

              <div className={`workload-indicator ${workloadClass}`}>
                <div className="workload-bar">
                  <div
                    className="fill"
                    style={{
                      width: `${Math.min(
                        100,
                        (barista.workloadRatio || 0) * 50
                      )}%`,
                    }}
                  ></div>
                </div>
                <span className="workload-label">
                  {workloadClass === "low" && "✅ Low Load"}
                  {workloadClass === "medium" && "ℹ️ Medium Load"}
                  {workloadClass === "high" && "⚠️ High Load"}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default memo(BaristaStatus);
