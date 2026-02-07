import React, { useState, memo } from "react";
import "../styles/SimulationStats.css";

function SimulationStats() {
    const [results, setResults] = useState([]);
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [selectedTestCase, setSelectedTestCase] = useState(null);

    const runSimulation = async () => {
        setLoading(true);
        setError("");
        setResults([]);
        setSummary(null);
        setSelectedTestCase(null);

        try {
            const response = await fetch("http://localhost:8081/api/simulation/run", {
                method: "POST",
            });

            if (!response.ok) {
                throw new Error("Failed to run simulation");
            }

            const data = await response.json();
            setResults(data.testCases || []);
            setSummary(data.summary || null);
        } catch (err) {
            setError("Error running simulation: " + err.message);
        } finally {
            setLoading(false);
        }
    };

    const formatTime = (seconds) => {
        if (!seconds && seconds !== 0) return "0s";
        const mins = Math.floor(seconds / 60);
        const secs = Math.round(seconds % 60);
        if (mins === 0) return `${secs}s`;
        return `${mins}m ${secs}s`;
    };

    const getPerformanceClass = (avgWait) => {
        if (avgWait < 180) return "excellent"; // < 3 min
        if (avgWait < 240) return "good"; // < 4 min (target)
        if (avgWait < 480) return "warning"; // < 8 min
        return "critical"; // 8+ min
    };

    const handleTestCaseClick = (testCase) => {
        setSelectedTestCase(selectedTestCase?.testCaseNumber === testCase.testCaseNumber ? null : testCase);
    };

    const getStatusEmoji = (status) => {
        switch (status) {
            case "COMPLETED": return "✅";
            case "ABANDONED": return "🚪";
            case "COMPLAINT": return "📢";
            default: return "❓";
        }
    };

    return (
        <div className="simulation-container">
            <h2>📊 Algorithm Performance Analysis</h2>


            <button
                className={`run-btn ${loading ? "loading" : ""}`}
                onClick={runSimulation}
                disabled={loading}
            >
                {loading ? "🔄 Running Simulation..." : "▶️ Run 10 Test Cases"}
            </button>

            {error && <div className="error-message">{error}</div>}

            {results.length > 0 && (
                <>
                    <div className="results-table-container">
                        <table className="results-table">
                            <thead>
                                <tr>
                                    <th>Test Case</th>
                                    <th>Orders</th>
                                    <th>Avg Wait</th>
                                    <th>B1</th>
                                    <th>B2</th>
                                    <th>B3</th>
                                    <th>Complaints</th>
                                    <th>Timeout %</th>
                                </tr>
                            </thead>
                            <tbody>
                                {results.map((result) => (
                                    <tr
                                        key={result.testCaseNumber}
                                        className={`${getPerformanceClass(result.avgWaitTimeSeconds)} ${selectedTestCase?.testCaseNumber === result.testCaseNumber ? "selected" : ""}`}
                                        onClick={() => handleTestCaseClick(result)}
                                        style={{ cursor: "pointer" }}
                                    >
                                        <td className="test-number">
                                            #{result.testCaseNumber}
                                            <span className="click-hint">👆</span>
                                        </td>
                                        <td>{result.totalOrders}</td>
                                        <td className="wait-time">
                                            {formatTime(result.avgWaitTimeSeconds)}
                                        </td>
                                        <td>{result.barista1Orders}</td>
                                        <td>{result.barista2Orders}</td>
                                        <td>{result.barista3Orders}</td>
                                        <td className={result.complaints > 0 ? "has-complaints" : ""}>
                                            {result.complaints || 0}
                                        </td>
                                        <td>{result.timeoutRate?.toFixed(1)}%</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {/* Detailed Order View */}
                    {selectedTestCase && selectedTestCase.orders && (
                        <div className="order-details-section">
                            <h3>
                                📋 Test Case #{selectedTestCase.testCaseNumber} - All Orders ({selectedTestCase.orders.length})
                                <button className="close-btn" onClick={() => setSelectedTestCase(null)}>✕</button>
                            </h3>
                            <div className="order-legend">
                                <span>✅ Completed</span>
                                <span>🚪 Abandoned (new: 8min)</span>
                                <span>📢 Complaint (old: 10min)</span>
                            </div>
                            <div className="orders-grid">
                                {selectedTestCase.orders.map((order) => (
                                    <div key={order.orderNumber} className={`order-card ${order.status.toLowerCase()}`}>
                                        <div className="order-header">
                                            <span className="order-num">#{order.orderNumber}</span>
                                            <span className="order-time">⏱️ {order.arrivalTime}</span>
                                            <span className="status-emoji">{getStatusEmoji(order.status)}</span>
                                        </div>
                                        <div className="order-customer">
                                            👤 {order.customerName}
                                        </div>
                                        <div className="order-info">
                                            <span className={`loyalty ${order.loyaltyStatus?.toLowerCase()}`}>
                                                {order.loyaltyStatus} {order.isRegular ? "(Old)" : "(New)"}
                                            </span>
                                        </div>
                                        <div className="order-drinks">
                                            ☕ {order.drinks?.join(", ")}
                                        </div>
                                        <div className="order-wait">
                                            ⏱️ Wait: {formatTime(order.waitTimeSeconds)}
                                        </div>
                                        {order.assignedBarista && (
                                            <div className="order-barista">
                                                👨‍🍳 Barista {order.assignedBarista}
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {summary && (
                        <div className="summary-section">
                            <h3>📈 Overall Summary</h3>
                            <div className="summary-grid">
                                <div className="summary-card">
                                    <span className="label">Total Test Cases</span>
                                    <span className="value">{summary.totalTestCases}</span>
                                </div>
                                <div className="summary-card">
                                    <span className="label">Total Orders</span>
                                    <span className="value">{summary.totalOrders}</span>
                                </div>
                                <div className="summary-card highlight">
                                    <span className="label">Avg Wait Time</span>
                                    <span className="value">
                                        {formatTime(summary.avgWaitTimeSeconds)}
                                    </span>
                                    <span className="target">Target: &lt;4 min</span>
                                </div>
                                <div className="summary-card">
                                    <span className="label">Total Abandoned</span>
                                    <span className={`value ${(summary.totalAbandoned || 0) > 0 ? "warning" : "success"}`}>
                                        {summary.totalAbandoned || 0}
                                    </span>
                                </div>
                                <div className="summary-card">
                                    <span className="label">Total Complaints</span>
                                    <span className={`value ${(summary.totalComplaints || 0) > 0 ? "warning" : "success"}`}>
                                        {summary.totalComplaints || 0}
                                    </span>
                                </div>
                                <div className="summary-card">
                                    <span className="label">Avg Abandon Rate</span>
                                    <span className={`value ${(summary.avgAbandonRate || 0) >= 3 ? "warning" : "success"}`}>
                                        {(summary.avgAbandonRate || 0).toFixed(2)}%
                                    </span>
                                    <span className="target">Target: &lt;3%</span>
                                </div>
                            </div>

                            <div className="barista-distribution">
                                <h4>👨‍🍳 Barista Workload Distribution</h4>
                                <div className="barista-bars">
                                    {[1, 2, 3].map(b => (
                                        <div className="barista-bar" key={b}>
                                            <span className="name">Barista {b}</span>
                                            <div className="bar-container">
                                                <div
                                                    className="bar"
                                                    style={{
                                                        width: `${(summary[`barista${b}Total`] / summary.totalOrders) * 100}%`,
                                                    }}
                                                ></div>
                                            </div>
                                            <span className="count">{summary[`barista${b}Total`]} orders</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}

export default memo(SimulationStats);
