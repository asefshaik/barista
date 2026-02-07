import React, { useState, useEffect } from "react";
import "../styles/Statistics.css";

export default function Statistics({ stats }) {
  const [displayStats, setDisplayStats] = useState(stats);

  useEffect(() => {
    setDisplayStats(stats);
  }, [stats]);

  const formatTime = (seconds) => {
    if (!seconds) return "0s";
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    if (mins === 0) return `${secs}s`;
    return `${mins}m ${secs}s`;
  };

  const getPerformanceClass = (metric) => {
    const timeoutRate = displayStats.timeoutRate || 0;
    if (timeoutRate < 3) return "excellent";
    if (timeoutRate < 5) return "good";
    if (timeoutRate < 8) return "warning";
    return "critical";
  };

  return (
    <div className="statistics-container">
      <h2>📈 System Performance Metrics</h2>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-header">
            <h3>Total Orders</h3>
            <span className="icon">📦</span>
          </div>
          <div className="stat-display">
            <div className="main-value">{displayStats.totalOrders || 0}</div>
            <div className="sub-values">
              <div>✅ Completed: {displayStats.completedOrders || 0}</div>
              <div>⏳ Waiting: {displayStats.waitingOrders || 0}</div>
            </div>
          </div>
        </div>

        <div className="stat-card performance">
          <div className="stat-header">
            <h3>Average Wait Time</h3>
            <span className="icon">⏱</span>
          </div>
          <div className="stat-display">
            <div className="main-value">
              {formatTime(displayStats.avgWaitTimeSeconds || 0)}
            </div>
            <div className="sub-text">Target: ~4.8 min</div>
          </div>
        </div>

        <div className={`stat-card ${getPerformanceClass("timeout")}`}>
          <div className="stat-header">
            <h3>Timeout Rate</h3>
            <span className="icon">⚠️</span>
          </div>
          <div className="stat-display">
            <div className="main-value">
              {(displayStats.timeoutRate || 0).toFixed(1)}%
            </div>
            <div className="sub-text">Target: &lt;2.3%</div>
          </div>
        </div>

        <div className={`stat-card ${displayStats.complaintCount > 0 ? "warning" : ""}`}>
          <div className="stat-header">
            <h3>Manager Complaints</h3>
            <span className="icon">🚨</span>
          </div>
          <div className="stat-display">
            <div className="main-value">
              {displayStats.complaintCount || 0}
            </div>
            <div className="sub-text">Orders waited 10+ min</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-header">
            <h3>Workload Balance</h3>
            <span className="icon">⚖️</span>
          </div>
          <div className="stat-display">
            <div className="main-value">
              {(displayStats.workloadBalance || 0).toFixed(1)}%
            </div>
            <div className="sub-text">Std Dev: &lt;12%</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-header">
            <h3>Peak Rush Hours</h3>
            <span className="icon">☕</span>
          </div>
          <div className="stat-display">
            <div className="main-value">7-10 AM</div>
            <div className="sub-text">200-300 customers/day</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-header">
            <h3>Algorithm</h3>
            <span className="icon">🤖</span>
          </div>
          <div className="stat-display">
            <div className="main-value">Smart Queue</div>
            <div className="sub-text">Priority-based scheduling</div>
          </div>
        </div>
      </div>

      <div className="performance-details">
        <h3>📊 Performance Details</h3>
        <div className="details-grid">
          <div className="detail-item">
            <span className="label">Priority Scoring</span>
            <div className="breakdown">
              <div>Wait Time: 40%</div>
              <div>Complexity: 25%</div>
              <div>Urgency: 25%</div>
              <div>Loyalty: 10%</div>
            </div>
          </div>

          <div className="detail-item">
            <span className="label">Constraints Met</span>
            <div className="breakdown">
              <div>✓ No wait &gt; 10 min</div>
              <div>✓ Orders not split</div>
              <div>✓ Fair queuing</div>
              <div>✓ Workload balanced</div>
            </div>
          </div>

          <div className="detail-item">
            <span className="label">Queue Psychology</span>
            <div className="breakdown">
              <div>Allow 1-2 faster orders</div>
              <div>Track skip count</div>
              <div>Show transparency</div>
              <div>Emergency boost @8min</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
