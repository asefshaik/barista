import React, { useState } from "react";
import "../styles/OrderForm.css";

export default function OrderForm({ onOrderCreated }) {
  const [drinks, setDrinks] = useState(["COLD_BREW"]);
  const [customerName, setCustomerName] = useState("");
  const [isRegular, setIsRegular] = useState(false);
  const [loyaltyStatus, setLoyaltyStatus] = useState("NONE");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const drinkOptions = [
    { value: "COLD_BREW", label: "Cold Brew (1 min) - ₹120", time: 1 },
    { value: "ESPRESSO", label: "Espresso (2 min) - ₹150", time: 2 },
    { value: "AMERICANO", label: "Americano (2 min) - ₹140", time: 2 },
    { value: "CAPPUCCINO", label: "Cappuccino (4 min) - ₹180", time: 4 },
    { value: "LATTE", label: "Latte (4 min) - ₹200", time: 4 },
    {
      value: "SPECIALTY_MOCHA",
      label: "Specialty Mocha (6 min) - ₹250",
      time: 6,
    },
  ];

  const handleAddDrink = (drinkValue) => {
    if (!drinks.includes(drinkValue)) {
      setDrinks([...drinks, drinkValue]);
    }
  };

  const handleRemoveDrink = (index) => {
    setDrinks(drinks.filter((_, i) => i !== index));
  };

  const calculateTotalTime = () => {
    return drinks.reduce((total, drink) => {
      const option = drinkOptions.find((d) => d.value === drink);
      return total + (option ? option.time : 0);
    }, 0);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const orderData = {
        drinks,
        customerName: customerName || "Guest",
        isRegular,
        loyaltyStatus: loyaltyStatus, // Always send the loyalty status value
      };

      const response = await fetch("http://localhost:8081/api/orders/create", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(orderData),
      });

      if (!response.ok) {
        throw new Error("Failed to create order");
      }

      const order = await response.json();
      const estWait = order.estimatedWaitTime
        ? Math.ceil(order.estimatedWaitTime / 60)
        : order.totalPrepTime || 5;
      alert(`Order created! ID: ${order.id} | Estimated wait: ~${estWait} min`);

      // Reset form
      setCustomerName("");
      setDrinks(["COLD_BREW"]);
      setIsRegular(false);
      setLoyaltyStatus("NONE");

      if (onOrderCreated) {
        onOrderCreated(order);
      }
    } catch (err) {
      setError("Error creating order: " + err.message);
      console.error("Order creation error:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="order-form-container">
      <h2>📱 Place Order</h2>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Customer Name</label>
          <input
            type="text"
            value={customerName}
            onChange={(e) => setCustomerName(e.target.value)}
            placeholder="Your name (optional)"
          />
        </div>

        <div className="form-group">
          <label>Select Drinks</label>
          <div className="drink-selector">
            {drinkOptions.map((option) => (
              <button
                key={option.value}
                type="button"
                className={`drink-btn ${drinks.includes(option.value) ? "active" : ""
                  }`}
                onClick={() => handleAddDrink(option.value)}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>

        <div className="selected-drinks">
          <h4>Selected Drinks:</h4>
          {drinks.length === 0 ? (
            <p>No drinks selected</p>
          ) : (
            <div>
              {drinks.map((drink, index) => (
                <div key={index} className="drink-item">
                  <span>
                    {drinkOptions.find((d) => d.value === drink)?.label}
                  </span>
                  <button
                    type="button"
                    onClick={() => handleRemoveDrink(index)}
                    className="remove-btn"
                  >
                    ✕
                  </button>
                </div>
              ))}
              <div className="total-time">
                Total Prep Time: <strong>{calculateTotalTime()} minutes</strong>
              </div>
            </div>
          )}
        </div>

        <div className="form-group">
          <label>
            <input
              type="checkbox"
              checked={isRegular}
              onChange={(e) => setIsRegular(e.target.checked)}
            />
            Regular Customer
          </label>
        </div>

        <div className="form-group">
          <label>Loyalty Status</label>
          <select
            value={loyaltyStatus}
            onChange={(e) => setLoyaltyStatus(e.target.value)}
          >
            <option value="NONE">None</option>
            <option value="BRONZE">Bronze</option>
            <option value="SILVER">Silver</option>
            <option value="GOLD">Gold</option>
          </select>
        </div>

        {error && <div className="error-message">{error}</div>}

        <button
          type="submit"
          disabled={loading || drinks.length === 0}
          className="submit-btn"
        >
          {loading ? "Creating Order..." : "Place Order"}
        </button>
      </form>
    </div>
  );
}
