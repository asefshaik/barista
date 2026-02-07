# Setup & Getting Started Guide

## System Requirements

### Backend

- **Java**: 17 or higher
- **Maven**: 3.8.0 or higher
- **RAM**: 2GB minimum

### Frontend

- **Node.js**: 16.x or higher
- **npm**: 8.x or higher
- **RAM**: 1GB minimum

## Installation Steps

### Step 1: Install Prerequisites

#### Windows

1. Install Java 17:

   - Download from https://www.oracle.com/java/technologies/downloads/
   - Or use `choco install openjdk17` if Chocolatey is installed

2. Install Maven:

   - Download from https://maven.apache.org/download.cgi
   - Add to PATH

3. Install Node.js:
   - Download from https://nodejs.org (LTS version)
   - npm will be installed automatically

#### Linux/Mac

```bash
# Ubuntu/Debian
sudo apt-get install openjdk-17-jdk maven nodejs npm

# macOS (using Homebrew)
brew install openjdk@17 maven node
```

### Step 2: Backend Setup

1. Navigate to backend folder:

```bash
cd backend
```

2. Generate keys/configs:

```bash
# On Windows
setup.bat

# On Linux/Mac
chmod +x setup.sh
./setup.sh
```

**Or manually:**

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run
```

3. Verify backend is running:

```bash
curl http://localhost:8080/api/baristas
```

You should see: `[]` (empty list initially)

### Step 3: Frontend Setup

**In a new terminal**, navigate to frontend folder:

```bash
cd frontend
```

1. Run setup:

```bash
# On Windows
setup.bat

# On Linux/Mac
chmod +x setup.sh
./setup.sh
```

**Or manually:**

```bash
npm install
npm start
```

2. Application will automatically open in browser at `http://localhost:3000`

## Verification Checklist

After startup, verify:

- [ ] Backend running on port 8080 (http://localhost:8080)
- [ ] Frontend running on port 3000 (http://localhost:3000)
- [ ] No console errors in either terminal
- [ ] Can place orders via frontend
- [ ] Queue updates in real-time
- [ ] Barista status shows (3 baristas initialized)

## Common Issues & Solutions

### Issue: "Port 8080 already in use"

```bash
# Linux/Mac: Find and kill process
lsof -ti:8080 | xargs kill -9

# Windows: Use different port in application.properties
# Change server.port=8080 to 8081
```

### Issue: "Java not found"

```bash
# Verify Java is installed
java -version

# Add to PATH if needed
# Windows: Control Panel > System > Advanced > Environment Variables
# Linux/Mac: Add to ~/.bashrc or ~/.zshrc
export PATH="/path/to/java:$PATH"
```

### Issue: "npm install fails"

```bash
# Clear cache and retry
npm cache clean --force
npm install

# Or use yarn
npm install -g yarn
yarn install
```

### Issue: "WebSocket connection fails"

- Ensure backend is running first
- Check if firewall is blocking port 8080
- Verify CORS is enabled (should be by default)

## Development Workflow

### Terminal 1 - Backend

```bash
cd backend
mvn spring-boot:run
```

### Terminal 2 - Frontend

```bash
cd frontend
npm start
```

### Terminal 3 - Testing (Optional)

```bash
# Test API endpoints
curl -X POST http://localhost:8080/api/orders/create \
  -H "Content-Type: application/json" \
  -d '{
    "drinks": ["COLD_BREW"],
    "customerName": "Test",
    "isRegular": false,
    "loyaltyStatus": "NONE"
  }'
```

## Usage Guide

### Customer View (Tab 1)

1. Enter your name (optional)
2. Select drinks from the menu
3. Select loyalty status if applicable
4. Click "Place Order"
5. Note the order ID and estimated wait time

### Queue Monitor (Tab 2)

- See all waiting orders
- View priority scores (0-100)
- Check wait times for each customer
- Click to expand order details

### Barista View (Tab 3)

- See real-time workload for each barista
- Check what drink they're currently making
- Monitor time until they're available
- View completed orders count

### Management Panel (Tab 4)

- **Waiting**: Click "Start Making" to begin preparation
- **Preparing**: Shows order being made by barista
- **Ready**: Orders ready for pickup (shows ☕ badge)

### Statistics (Tab 5)

- Monitor system performance
- View key metrics (wait times, timeout rate, workload)
- Check priority scoring breakdown
- See constraints met status

## API Testing with cURL

### Create Order

```bash
curl -X POST http://localhost:8080/api/orders/create \
  -H "Content-Type: application/json" \
  -d '{
    "drinks": ["COLD_BREW", "CAPPUCCINO"],
    "customerName": "Alice",
    "isRegular": true,
    "loyaltyStatus": "GOLD"
  }'
```

### Get Waiting Orders

```bash
curl http://localhost:8080/api/baristas
```

### Start Order

```bash
curl -X PUT http://localhost:8080/api/orders/1/start
```

### Get Stats

```bash
curl http://localhost:8080/api/stats
```

## Database

- Using H2 in-memory database (no external DB needed)
- Data persists while application is running
- Resets on restart
- For persistence, modify `pom.xml` to use PostgreSQL/MySQL

## Production Deployment

### Docker

```bash
cd backend
docker build -t barista-queue .
docker run -p 8080:8080 barista-queue
```

### Cloud Services

- AWS: Use EC2 + RDS + S3
- Azure: Use App Service + Azure SQL
- Google Cloud: Use App Engine + Cloud SQL
- Heroku: `git push heroku main`

## Performance Tips

1. Close unused applications to free RAM
2. Use Chrome DevTools for frontend debugging
3. Check backend logs for API errors:
   - Look for `ERROR` in console
   - Check database query times
4. Monitor network tab in browser DevTools
5. Use System Monitor to check resource usage

## Security Notes

- Current version has NO authentication (MVP)
- Add JWT/OAuth2 before production
- Add HTTPS/SSL certificates
- Implement rate limiting
- Add input validation for all APIs
- Use environment variables for secrets

## Support & Troubleshooting

1. Check README.md in root and backend folders
2. Review API_DOCS.md for endpoint details
3. Check console/logs for error messages
4. Verify all ports are accessible: 3000, 8080
5. Ensure backend starts before frontend

## Next Steps

1. Run a basic test order flow
2. Test with multiple concurrent orders
3. Monitor performance metrics
4. Explore the UI and different tabs
5. Consider customizations for your needs

For more details, see:

- [Backend API Documentation](backend/API_DOCS.md)
- [Main README](README.md)
