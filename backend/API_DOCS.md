# Backend API Documentation

## Overview

The backend provides RESTful APIs for managing the barista queue system with real-time updates via WebSocket.

## Base URL

```
http://localhost:8080/api
```

## Endpoints

### Orders

#### Create Order

```
POST /orders/create
Content-Type: application/json

{
  "drinks": ["COLD_BREW", "CAPPUCCINO"],
  "customerName": "John Doe",
  "isRegular": true,
  "loyaltyStatus": "GOLD"
}

Response:
{
  "id": 1,
  "drinks": ["COLD_BREW", "CAPPUCCINO"],
  "customerName": "John Doe",
  "totalPrepTime": 5,
  "status": "WAITING",
  "priorityScore": 45,
  "createdAt": "2024-02-07T09:15:00"
}
```

#### Get Waiting Orders

```
GET /orders/waiting

Response:
[
  {
    "id": 1,
    "drinks": ["COLD_BREW"],
    "customerName": "John",
    "totalPrepTime": 1,
    "status": "WAITING",
    "priorityScore": 35,
    "assignedToBarista": 1,
    "createdAt": "2024-02-07T09:15:00"
  }
]
```

#### Get Preparing Orders

```
GET /orders/preparing

Response: Same as waiting orders but with status "PREPARING"
```

#### Get Completed Orders

```
GET /orders/completed

Response: Same format, status "COMPLETED"
```

#### Start Order Preparation

```
PUT /orders/{orderId}/start

Response: 200 OK
```

#### Complete Order

```
PUT /orders/{orderId}/complete

Response: 200 OK
```

#### Get Order Details

```
GET /orders/{orderId}

Response:
{
  "id": 1,
  "drinks": ["COLD_BREW"],
  "customerName": "John",
  "totalPrepTime": 1,
  "status": "WAITING",
  "priorityScore": 35,
  "createdAt": "2024-02-07T09:15:00",
  "startedAt": null,
  "completedAt": null,
  "assignedToBarista": 1,
  "estimatedWaitTime": 120,
  "displayPosition": 2,
  "hoursBeatenBy": 1,
  "loyaltyStatus": "NONE"
}
```

### Baristas

#### Get All Baristas

```
GET /baristas

Response:
[
  {
    "id": 1,
    "name": "Barista 1",
    "currentOrderId": 5,
    "busyUntil": "2024-02-07T09:16:30",
    "totalWorkTime": 45,
    "completedOrders": 12,
    "workloadRatio": 1.05
  }
]
```

#### Get Barista by ID

```
GET /baristas/{id}

Response: Single barista object (same format as above)
```

### Statistics

#### Get System Statistics

```
GET /stats

Response:
{
  "totalOrders": 50,
  "completedOrders": 35,
  "waitingOrders": 15,
  "avgWaitTimeSeconds": 288,
  "timeoutCount": 1,
  "timeoutRate": 2.0,
  "workloadBalance": 96.5
}
```

## WebSocket Events

### Connection

```
ws://localhost:8080/ws-queue
```

### Subscribe to Queue Updates

```javascript
stompClient.subscribe("/topic/queue-update", (message) => {
  const data = JSON.parse(message.body);
  console.log("Updated waiting orders:", data.waitingOrders);
  console.log("Updated baristas:", data.baristas);
});
```

## Drink Types

```
COLD_BREW        - 1 min - ₹120 - 25% frequency
ESPRESSO         - 2 min - ₹150 - 20% frequency
AMERICANO        - 2 min - ₹140 - 15% frequency
CAPPUCCINO       - 4 min - ₹180 - 20% frequency
LATTE            - 4 min - ₹200 - 12% frequency
SPECIALTY_MOCHA  - 6 min - ₹250 - 8% frequency
```

## Order Status

- `WAITING` - Order received, waiting for assignment
- `PREPARING` - Barista is making the order
- `READY` - Order is ready for pickup
- `COMPLETED` - Order has been picked up
- `ABANDONED` - Customer abandoned the order

## Loyalty Statuses

- `NONE` - Regular customer
- `BRONZE` - Bronze member
- `SILVER` - Silver member
- `GOLD` - Gold member (highest priority)

## Error Handling

All errors return appropriate HTTP status codes:

- `200` - Success
- `400` - Bad Request
- `404` - Not Found
- `500` - Internal Server Error

## Rate Limiting

No rate limiting implemented for MVP. Consider implementing in production.

## Authentication

No authentication implemented for MVP. Add JWT or OAuth2 in production.

## CORS

CORS is enabled for `http://localhost:3000` to allow frontend requests.
