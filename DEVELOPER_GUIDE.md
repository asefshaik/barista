# Developer Guide - Barista Queue System

## Getting Started with the Codebase

### Project Organization

The project uses a clean architecture pattern with clear separation of concerns:

```
Backend Architecture:
├── Model Layer      (Entities, DTOs)
├── Repository Layer (Data access)
├── Service Layer    (Business logic)
├── Algorithm Layer  (Priority queue, assignment)
├── Controller Layer (REST APIs)
└── Config Layer     (Spring configuration)
```

### Key Classes to Understand

#### 1. **PriorityScoreCalculator** (Core Algorithm)

```java
// Location: backend/src/main/java/com/barista/algorithm/
// Calculates order priority (0-100) based on:
// - Wait time (40%)
// - Complexity (25%)
// - Loyalty (10%)
// - Urgency (25%)
// + Bonuses/penalties

// Key method:
int calculatePriorityScore(Order order, List<Order> allOrders, List<Order> completed)
```

**To modify scoring weights:**

- Change constants at top of class
- Weights must sum to 100%
- Test with sample data

#### 2. **AssignmentAlgorithm** (Workload Balancing)

```java
// Location: backend/src/main/java/com/barista/algorithm/
// Assigns orders to best available barista considering:
// - Workload ratio (current/average)
// - Order complexity
// - Barista availability

// Key method:
Barista findBestBarista(Order order, List<Barista> baristas)
```

**To modify assignment strategy:**

- Adjust `calculateBaristaScore()` method
- Change workload thresholds (0.8x, 1.2x)
- Add new criteria to scoring

#### 3. **QueueManagementService** (Core Logic)

```java
// Location: backend/src/main/java/com/barista/service/
// Orchestrates the entire system:
// - Order creation & assignment
// - Barista workload updates
// - Event publishing (WebSocket)
// - Scheduled rebalancing

// Key methods:
createOrder()          // New order entry point
assignOrderToBarista() // Priority assignment
rebalanceQueue()       // Runs every 30 seconds
```

---

## Extending the System

### Adding a New Drink

1. **Update DrinkType enum:**

```java
// In backend/src/main/java/com/barista/model/DrinkType.java
public enum DrinkType {
    AFFOGATO("Affogato", 5, 0.12, 280);
    // Params: displayName, prepTime(min), frequency, price

    // Update frequency percentages to sum to 100%
}
```

2. **Update frontend drink list:**

```javascript
// In frontend/src/components/OrderForm.js
const drinkOptions = [
  { value: "AFFOGATO", label: "Affogato (5 min) - ₹280", time: 5 },
];
```

### Modifying Priority Weights

**Before:**

```
Wait Time: 40% | Complexity: 25% | Urgency: 25% | Loyalty: 10%
```

**To change (example):**

```java
// In PriorityScoreCalculator.java
private static final double WAIT_TIME_WEIGHT = 0.35;      // 35% (was 40%)
private static final double COMPLEXITY_WEIGHT = 0.30;     // 30% (was 25%)
private static final double LOYALTY_WEIGHT = 0.15;        // 15% (was 10%)
private static final double URGENCY_WEIGHT = 0.20;        // 20% (was 25%)
// Total = 100%
```

### Adding More Baristas

**Option 1: Database initialization:**

```java
// In QueueManagementService.java - initializeBaristas()
for (int i = 1; i <= 5; i++) {  // Change 3 to 5
    Barista barista = new Barista();
    barista.setName("Barista " + i);
    // ... rest of initialization
}
```

**Option 2: API endpoint (add to BaristaController):**

```java
@PostMapping
public ResponseEntity<Barista> addBarista(@RequestBody Barista barista) {
    barista.setTotalWorkTime(0);
    barista.setCompletedOrders(0);
    Barista saved = baristaRepository.save(barista);
    return ResponseEntity.ok(saved);
}
```

### Changing Rebalancing Interval

**Current: 30 seconds**

```java
// In QueueManagementService.java
@Scheduled(fixedRate = 30000)  // Change 30000 to desired milliseconds
public void rebalanceQueue() { ... }
```

**Examples:**

- 10 seconds: `10000`
- 1 minute: `60000`
- 5 minutes: `300000`

---

## Database Persistence

### Switching from H2 to PostgreSQL

**Step 1: Update pom.xml**

```xml
<!-- Remove H2 -->
<!-- <dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency> -->

<!-- Add PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.6.0</version>
</dependency>
```

**Step 2: Update application.properties**

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/barista_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

**Step 3: Install PostgreSQL**

```bash
# macOS
brew install postgresql@15

# Ubuntu
sudo apt-get install postgresql-15

# Windows: Download from https://www.postgresql.org/download/
```

---

## Adding Authentication

### Basic JWT Implementation

```java
// Add dependency to pom.xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.12.3</version>
</dependency>
```

```java
// Create JwtProvider.java
@Component
public class JwtProvider {
    private String jwtSecret = "your-secret-key";
    private int jwtExpirationMs = 86400000; // 24 hours

    public String generateToken(String username) {
        return Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(jwtSecret)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
}
```

---

## Testing

### Backend Unit Tests

```java
// Create src/test/java/com/barista/algorithm/PriorityTest.java
@SpringBootTest
class PriorityScoreCalculatorTest {

    @Test
    void testPriorityCalculationWithLongWait() {
        Order order = new Order();
        order.setCreatedAt(LocalDateTime.now().minusMinutes(9));
        order.setTotalPrepTime(1);

        int score = PriorityScoreCalculator.calculatePriorityScore(
            order, Collections.emptyList(), Collections.emptyList()
        );

        assertTrue(score > 50, "High wait time should increase priority");
    }
}
```

### Frontend Testing

```javascript
// Use Jest and React Testing Library
import { render, screen } from "@testing-library/react";
import OrderForm from "../OrderForm";

test("renders order form", () => {
  render(<OrderForm />);
  expect(screen.getByText(/Place Order/i)).toBeInTheDocument();
});
```

---

## Monitoring & Debugging

### Backend Logging

```java
// Enable debug logging in application.properties
logging.level.com.barista=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

// In your code:
private static final Logger logger = LoggerFactory.getLogger(QueueManagementService.class);
logger.debug("Order {} assigned to barista {}", order.getId(), barista.getId());
```

### Frontend Debugging

```javascript
// Enable React DevTools
// Chrome: Install "React Developer Tools" extension
// Firefox: Install "React Developer Tools" extension

// Console logging
console.log("Queue update:", orders);
console.table(statistics);

// Network debugging
// Open DevTools (F12) → Network tab
// Check all API calls and WebSocket messages
```

### Performance Monitoring

```java
// Add timing to key methods
long startTime = System.currentTimeMillis();
List<Order> rankedOrders = rankOrdersByPriority(waitingOrders, completedOrders);
long duration = System.currentTimeMillis() - startTime;
logger.info("Ranking took {} ms for {} orders", duration, waitingOrders.size());
```

---

## Deployment Checklist

### Before Production

- [ ] Change default passwords
- [ ] Disable H2 console
- [ ] Enable HTTPS/SSL
- [ ] Add authentication/authorization
- [ ] Set up proper logging
- [ ] Configure database persistence
- [ ] Load testing (JMeter)
- [ ] Security audit
- [ ] Add rate limiting
- [ ] Implement caching

### Docker Deployment

```dockerfile
# Backend
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/barista-queue-system-1.0.0.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build and run
docker build -t barista-backend .
docker run -p 8080:8080 \
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/barista \
    barista-backend
```

---

## Common Issues & Solutions

### Issue: Priority scores not updating

**Cause:** `rebalanceQueue()` not triggered
**Solution:** Check if scheduled task is enabled

```java
// Ensure @EnableScheduling in main class
@SpringBootApplication
@EnableScheduling
public class BaristaApplication { ... }
```

### Issue: WebSocket connection fails

**Cause:** CORS misconfiguration
**Solution:** Update WebSocketConfig.java

```java
registry.addEndpoint("/ws-queue")
    .setAllowedOrigins("http://localhost:3000", "http://yourfrontend.com")
    .withSockJS();
```

### Issue: Frontend shows stale data

**Cause:** WebSocket not connected
**Solution:** Check browser console

```javascript
// In Chrome DevTools Console:
// You should see "WebSocket connected" message
// If not, check backend is running and CORS is configured
```

---

## Performance Optimization Tips

1. **Reduce rebalancing frequency** if CPU usage too high
2. **Add caching** for barista workload calculations
3. **Batch database writes** for multiple orders
4. **Use connection pooling** (HikariCP, already included)
5. **Implement pagination** for large order lists

---

## Resources

- **Spring Boot Docs:** https://spring.io/projects/spring-boot
- **React Docs:** https://react.dev
- **WebSocket Guide:** https://stomp-js.github.io/stomp-websocket/
- **JWT Auth:** https://jwt.io
- **Docker Docs:** https://docs.docker.com

---

## Support

For questions about the code:

1. Check inline comments
2. Review API_DOCS.md
3. Look at similar implementations
4. Test with sample data using OrderSimulator utility

Happy coding! 🚀
