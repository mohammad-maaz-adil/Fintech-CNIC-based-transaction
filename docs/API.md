# FinTech CNIC Transaction System - API Documentation

All endpoints are prefixed with `/api`. Protected endpoints require `Authorization: Bearer <JWT>` header.

---

## Authentication

### POST /api/auth/signup
Register a new user.

**Request:**
```json
{
  "cnic": "35202-1234567-1",
  "fullName": "Muhammad Ahmed Khan",
  "password": "securepassword"
}
```

**Response (201):**
```json
{
  "token": "eyJhbGci...",
  "cnic": "35202-1234567-1",
  "fullName": "Muhammad Ahmed Khan",
  "message": "Authentication successful"
}
```

**Errors:**
- `409 Conflict` – CNIC already registered
- `400 Bad Request` – Validation failure (invalid CNIC, weak password)

---

### POST /api/auth/login
Authenticate with CNIC + password.

**Request:**
```json
{
  "cnic": "35202-1234567-1",
  "password": "securepassword"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGci...",
  "cnic": "35202-1234567-1",
  "fullName": "Muhammad Ahmed Khan",
  "message": "Authentication successful"
}
```

**Errors:**
- `401 Unauthorized` – Invalid CNIC or password

---

## Dashboard

### GET /api/dashboard/summary 🔒
Returns account balance and recent transactions.

**Response (200):**
```json
{
  "fullName": "Muhammad Ahmed Khan",
  "cnic": "35202-1234567-1",
  "accountNumber": "PKF-00100001",
  "balance": 75000.00,
  "currency": "PKR",
  "recentTransactions": [
    {
      "id": 1,
      "amount": 5000.00,
      "description": "Transfer to Ayesha Siddiqui",
      "createdAt": "2024-01-01T10:00:00",
      "type": "debit"
    }
  ]
}
```

---

## Transactions

### POST /api/transactions/validate-recipient 🔒
Validates a recipient's CNIC before sending money.

**Request:**
```json
{ "cnic": "35202-7654321-2" }
```

**Response (200):**
```json
{
  "recipientName": "Ayesha Siddiqui",
  "accountId": 2,
  "accountNumber": "PKF-00100002"
}
```

**Errors:**
- `404` – Recipient not found or account inactive
- `400` – Cannot send to yourself

---

### POST /api/transactions/send 🔒
Processes a money transfer. OTP verification is required.

**Request:**
```json
{
  "recipientCNIC": "35202-7654321-2",
  "amount": 5000.00,
  "otp": "123456"
}
```

**Response (200):**
```json
{
  "referenceNumber": "TXN-20240101-A1B2C3D4",
  "amount": 5000.00,
  "recipientName": "Ayesha Siddiqui",
  "newBalance": 70000.00,
  "status": "SUCCESS"
}
```

**Errors:**
- `401` – Invalid/expired OTP
- `400` – Insufficient balance, invalid amount
- `404` – Recipient not found

---

### GET /api/transactions/history 🔒
Returns paginated transaction history.

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 20, max: 100)

**Response (200):**
```json
{
  "transactions": [...],
  "totalPages": 3
}
```

---

## OTP

### POST /api/otp/send 🔒
Generates and sends an OTP (simulation: returns OTP in response).

**Request:**
```json
{ "transactionRef": "optional-ref" }
```

**Response (200):**
```json
{
  "message": "OTP sent successfully",
  "otp": "123456",
  "expiresInMinutes": 5
}
```

> ⚠️ The `otp` field is returned in the response for simulation/testing only. Remove in production.

---

### POST /api/otp/verify 🔒
Verifies an OTP.

**Request:**
```json
{ "otp": "123456" }
```

**Response (200):**
```json
{ "valid": true, "message": "OTP verified successfully" }
```

**Error (400):**
```json
{ "valid": false, "message": "Invalid, expired, or exceeded OTP" }
```

---

## Profile

### GET /api/profile 🔒
Returns the authenticated user's profile.

**Response (200):**
```json
{
  "fullName": "Muhammad Ahmed Khan",
  "cnic": "35202-1234567-1",
  "status": "ACTIVE",
  "accountNumber": "PKF-00100001",
  "balance": 75000.00,
  "currency": "PKR",
  "createdAt": "2024-01-01T09:00:00"
}
```

---

## Error Format

All error responses follow:
```json
{ "message": "Human-readable error description" }
```

## Security Notes

- JWT tokens expire after 24 hours (configurable)
- CNIC format validated on all inputs: `\d{5}-\d{7}-\d`
- Passwords stored as BCrypt hashes
- OTP codes stored as BCrypt hashes, never plain text
- OTPs expire after 5 minutes, max 3 attempts
- Pessimistic write locks prevent double-spend race conditions
