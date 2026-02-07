import React, { useState, useEffect } from "react";

// Countdown timer that updates locally without causing re-renders of parent
export default function CountdownTimer({ endTime, label = "Time left" }) {
    const [timeLeft, setTimeLeft] = useState("");

    useEffect(() => {
        const calculateTimeLeft = () => {
            if (!endTime) return "0:00";

            const end = new Date(endTime);
            if (isNaN(end.getTime())) return "0:00"; // Handle invalid date strings

            const now = new Date();
            const diffMs = end - now;

            if (diffMs <= 0) return "Ready!";

            const diffSeconds = Math.floor(diffMs / 1000);
            const minutes = Math.floor(diffSeconds / 60);
            const seconds = Math.floor(diffSeconds % 60);

            if (isNaN(minutes) || isNaN(seconds)) return "0:00";

            return `${minutes}:${seconds.toString().padStart(2, "0")}`;
        };

        // Initial calculation
        setTimeLeft(calculateTimeLeft());

        // Update every second locally
        const timer = setInterval(() => {
            setTimeLeft(calculateTimeLeft());
        }, 1000);

        return () => clearInterval(timer);
    }, [endTime]);

    const isReady = timeLeft === "Ready!";

    return (
        <div className={`countdown-timer ${isReady ? "ready" : ""}`}>
            <span className="timer-label">{label}:</span>
            <span className="timer-value">{timeLeft}</span>
        </div>
    );
}
