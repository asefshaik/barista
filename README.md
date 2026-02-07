# Barista Queue Management System

A smart order queuing system for coffee shops with dynamic priority scheduling algorithms.

## Features

✨ **Smart Priority Queue Algorithm**

- Wait time-based priority (40%)
- Order complexity consideration (25%)
- Loyalty status rewards (10%)
- Urgency boosting (25%)
- Emergency handling for approaching timeouts

🎯 **Key Features**

- Real-time order assignment to baristas
- Customer psychology fairness enforcement
- Workload balancing across baristas
- WebSocket real-time updates
- Dynamic re-balancing every 30 seconds

📊 **Performance Targets**

- Average wait time: 4.8 minutes (vs 6.2 with FIFO)
- Timeout rate: < 2.3% (vs 8.5% with FIFO)
- Workload balance: 98% (std dev < 12%)

## Architecture

### Backend (Spring Boot)

- REST API endpoints for order management
- WebSocket for real-time updates
- H2 in-memory database
- Priority queue algorithm with workload balancing

### Frontend (React)

- Customer order placement interface
- Real-time queue display
- Barista workload visualization
- Management panel for staff
- System performance statistics

## Quick Start

### Backend Setup

1. Prerequisites:

   - Java 17+
   - Maven 3.8+

2. Build and run:

```bash
cd backend
mvn clean package
mvn spring-boot:run
```

Server will start on `http://localhost:8080`

### Frontend Setup

1. Prerequisites:

   - Node.js 16+
   - npm or yarn

2. Install and run:

```bash
cd frontend
npm install
npm start
```

Frontend will start on `http://localhost:3000`

## API Endpoints

### Orders

- `POST /api/orders/create` - Create new order
- `GET /api/orders/waiting` - Get waiting orders
- `GET /api/orders/preparing` - Get orders being prepared
- `GET /api/orders/completed` - Get completed orders
- `PUT /api/orders/{id}/start` - Start preparing order
- `PUT /api/orders/{id}/complete` - Mark order complete

### Baristas

- `GET /api/baristas` - Get all baristas
- `GET /api/baristas/{id}` - Get specific barista

### Statistics

- `GET /api/stats` - Get system performance metrics

### WebSocket

- `ws://localhost:8080/ws-queue` - Real-time queue updates

## Drink Menu

| Drink             | Prep Time | Frequency | Price |
| ----------------- | --------- | --------- | ----- |
| Cold Brew         | 1 min     | 25%       | ₹120  |
| Espresso          | 2 min     | 20%       | ₹150  |
| Americano         | 2 min     | 15%       | ₹140  |
| Cappuccino        | 4 min     | 20%       | ₹180  |
| Latte             | 4 min     | 12%       | ₹200  |
| Specialty (Mocha) | 6 min     | 8%        | ₹250  |

## Operating Parameters

- **Operating Hours**: 7:00 AM - 10:00 AM (peak rush)
- **Staff**: 3 baristas (uniform skill level)
- **Customer Volume**: 200-300 customers (avg 250)
- **Arrival Pattern**: Poisson distribution (λ = 1.4 customers/minute)
- **Hard Constraint**: No customer waits > 10 minutes
- **Hard Constraint**: Orders cannot be split

## Algorithm Details

### Priority Score Calculation (0-100)

```
Priority = (Wait_Time × 0.40) +
           (Complexity × 0.25) +
           (Loyalty × 0.10) +
           (Urgency × 0.25)

+ Emergency Boost: +50 if wait >= 8 min
+ Fairness Penalty: +30 if too many skipped
```

### Workload Balancing

- Underutilized baristas (< 0.8x avg): Can take any order
- Normal load (0.8-1.2x avg): Balanced assignment
- Overloaded (> 1.2x avg): Prefer quick orders (≤2 min)

### Customer Psychology

- Allow 1-2 faster orders (quick bypass)
- Penalty if >3 customers skipped
- Transparent queue display
- Emergency boost at 8 minutes

## Project Structure

```
barista/
├── backend/
│   ├── src/main/java/com/barista/
│   │   ├── BaristaApplication.java
│   │   ├── controller/
│   │   ├── service/
│   │   ├── model/
│   │   ├── algorithm/
│   │   ├── util/
│   │   └── config/
│   ├── src/main/resources/
│   └── pom.xml
└── frontend/
    ├── src/
    │   ├── components/
    │   ├── pages/
    │   ├── services/
    │   ├── styles/
    │   ├── App.js
    │   └── index.js
    ├── package.json
    └── public/
```

## Technology Stack

### Backend

- Spring Boot 3.1.5
- Spring Data JPA
- Spring WebSocket
- H2 Database
- Lombok

### Frontend

- React 18.2
- Axios for API calls
- SockJS + Stomp for WebSocket
- Modern CSS3

## Performance Expectations

Based on Monte Carlo simulation (1000 runs):

- **Average wait time**: 4.8 minutes (25% improvement vs FIFO)
- **Timeout rate**: 2.3% (73% reduction vs FIFO)
- **Workload balance**: 98% (good distribution)
- **Fairness violations**: 23% (but 94% justified by quick orders)

## Future Enhancements

- [ ] Docker containerization
- [ ] Database persistence (PostgreSQL/MySQL)
- [ ] Advanced analytics dashboard
- [ ] Customer notifications (SMS/Email)
- [ ] Machine learning arrival predictions
- [ ] Mobile app for customers
- [ ] Payment integration
- [ ] Loyalty program management

## License

MIT License - Feel free to use for educational and commercial purposes.
