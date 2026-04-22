# FinTech CNIC Transaction System

A full-stack financial transaction web application where all critical frontend flows are managed using **Finite State Machines (XState)** for predictable, secure user interactions.

## Stack

| Layer    | Technology                                      |
|----------|-------------------------------------------------|
| Frontend | Vue 3 + Composition API + Vite                  |
| FSM      | XState v5 + @xstate/vue                         |
| HTTP     | Axios                                           |
| Backend  | Quarkus 3 (Java 17)                             |
| Auth     | SmallRye JWT (RS256)                            |
| Database | MySQL 8                                         |
| ORM      | Hibernate ORM + Hibernate Validator             |

## Project Structure

```
├── frontend/          Vue 3 + XState frontend
│   └── src/
│       ├── machines/  FSM definitions (auth, transaction, dashboard, history)
│       ├── views/     Page components
│       ├── components/ Shared UI components
│       ├── services/  Axios API client
│       └── router/    Vue Router with auth guards
├── backend/           Quarkus REST API
│   └── src/main/java/com/fintech/
│       ├── entity/    JPA entities
│       ├── dto/       Request/response DTOs
│       ├── service/   Business logic
│       └── resource/  REST endpoints
├── database/          SQL schema + seed data
└── docs/              API docs + FSM diagrams
```

## Prerequisites

- Node.js 18+
- Java 17+
- Maven 3.9+
- MySQL 8+

## Quick Start

### 1. Database Setup

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p fintech_db < database/seed.sql
```

Create MySQL user:
```sql
CREATE USER 'fintech'@'localhost' IDENTIFIED BY 'fintech123';
GRANT ALL PRIVILEGES ON fintech_db.* TO 'fintech'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Backend Setup

Generate RSA keys for JWT:
```bash
cd backend
openssl genrsa -out src/main/resources/privateKey.pem 2048
openssl rsa -pubout -in src/main/resources/privateKey.pem -out src/main/resources/publicKey.pem
```

Configure database (optional - defaults work for local MySQL):
```bash
export DB_URL="jdbc:mysql://localhost:3306/fintech_db?serverTimezone=UTC&useSSL=false"
export DB_USER="fintech"
export DB_PASS="fintech123"
```

Start Quarkus in dev mode:
```bash
cd backend
./mvnw quarkus:dev
```

Backend runs on: `http://localhost:8080`

### 3. Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on: `http://localhost:5173`

> The Vite dev server proxies `/api` requests to `http://localhost:8080`

## FSM Architecture

All critical UI flows use XState v5 state machines:

| Machine               | States                                                                              |
|-----------------------|-------------------------------------------------------------------------------------|
| `authMachine`         | idle → enteringCredentials → validating → success \| error                         |
| `transactionMachine`  | idle → enteringRecipientCNIC → validatingRecipient → enteringAmount → confirmingTransaction → OTPVerification → processing → success \| failure |
| `dashboardMachine`    | loading → loaded \| error, loaded → refreshing                                     |
| `historyMachine`      | fetching → success \| empty \| error                                               |

See [`docs/FSM.md`](docs/FSM.md) for detailed state diagrams.

## API Reference

See [`docs/API.md`](docs/API.md) for full endpoint documentation.

## Security

- JWT (RS256) with 24-hour expiry
- CNIC format validated server-side: `\d{5}-\d{7}-\d`
- Passwords stored as BCrypt hashes
- OTPs stored as BCrypt hashes, expire in 5 minutes, max 3 attempts
- Pessimistic write locks prevent double-spend race conditions
- CORS restricted to `localhost:5173` in development

## Test Accounts (Seed Data)

All seed accounts use password: `password123`

| CNIC              | Name                  | Balance   |
|-------------------|-----------------------|-----------|
| 42101-1234567-1   | Mohammad Maaz Adil    | PKR 75,000 |
| 35202-7654321-2   | Ayesha Siddiqui       | PKR 32,500 |
| 42301-9876543-3   | Ali Hassan Malik      | PKR 120,000 |
| 31202-1111111-4   | Fatima Zahra          | PKR 15,000 |

## Development Notes

### OTP Simulation

In development, the `POST /api/otp/send` endpoint returns the OTP in the response body for testing convenience. In production, remove the `otp` field from the response and integrate with an SMS gateway.

### Database Migrations

The backend uses `quarkus.hibernate-orm.database.generation=validate` in production. For schema updates, modify `database/schema.sql` and apply manually. For development, you can use `update` or `drop-and-create`.

## License

MIT
