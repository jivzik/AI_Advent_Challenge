# B2B WebShop - API Documentation

> Version: 2.0  
> Base URL: `https://api.webshop.example.com`  
> Date: 2026-01-15

## Table of Contents
1. [Authentication](#authentication)
2. [Catalog API](#catalog-api)
3. [Order API](#order-api)
4. [Payment API](#payment-api)
5. [User API](#user-api)
6. [Error Handling](#error-handling)

---

## Authentication

### POST /api/v1/auth/login

Authenticate user and receive JWT token.

**Request:**
```json
{
  "email": "user@company.com",
  "password": "SecurePassword123"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "7d8f9a0b1c2d3e4f...",
  "expiresIn": 3600,
  "tokenType": "Bearer",
  "user": {
    "id": "uuid",
    "email": "user@company.com",
    "name": "John Doe",
    "role": "CUSTOMER_ADMIN",
    "tier": "GOLD"
  }
}
```

**Headers:**
All authenticated requests must include:
```
Authorization: Bearer {accessToken}
```

---

## Catalog API

### GET /api/v1/catalog/products

List products with pagination and filters.

**Query Parameters:**
- `page` (int, default: 0)
- `size` (int, default: 20, max: 100)
- `category` (string, optional)
- `search` (string, optional)
- `minPrice` (decimal, optional)
- `maxPrice` (decimal, optional)
- `sort` (string: "name", "price", "popularity")

**Example:**
```
GET /api/v1/catalog/products?page=0&size=20&category=electronics&sort=price
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "P001",
      "name": "Laptop Dell XPS 15",
      "sku": "DELL-XPS-15-2024",
      "category": "Electronics > Computers",
      "description": "High-performance laptop...",
      "basePrice": 1299.99,
      "yourPrice": 1169.99,
      "discountPercent": 10,
      "currency": "EUR",
      "stock": 45,
      "minOrderQuantity": 1,
      "imageUrl": "https://cdn.webshop.example.com/products/P001.jpg",
      "availability": "IN_STOCK"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

### GET /api/v1/catalog/products/{id}

Get detailed product information.

**Response (200 OK):**
```json
{
  "id": "P001",
  "name": "Laptop Dell XPS 15",
  "sku": "DELL-XPS-15-2024",
  "category": "Electronics > Computers",
  "description": "High-performance laptop for professionals...",
  "specifications": {
    "processor": "Intel Core i7-13700H",
    "ram": "16GB DDR5",
    "storage": "512GB NVMe SSD",
    "display": "15.6\" FHD IPS",
    "weight": "1.8 kg"
  },
  "pricing": {
    "basePrice": 1299.99,
    "tieredPrices": {
      "BRONZE": 1299.99,
      "SILVER": 1234.99,
      "GOLD": 1169.99,
      "PLATINUM": 1104.99
    },
    "volumeDiscounts": [
      {"from": 10, "to": 49, "discount": 5},
      {"from": 50, "to": 99, "discount": 10},
      {"from": 100, "to": null, "discount": 15}
    ]
  },
  "stock": {
    "available": 45,
    "reserved": 12,
    "total": 57,
    "warehouses": [
      {"location": "Berlin", "quantity": 30},
      {"location": "Munich", "quantity": 15}
    ]
  },
  "images": [
    "https://cdn.webshop.example.com/products/P001-1.jpg",
    "https://cdn.webshop.example.com/products/P001-2.jpg"
  ],
  "relatedProducts": ["P002", "P003"],
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2026-01-14T15:30:00Z"
}
```

### GET /api/v1/catalog/categories

List all product categories (tree structure).

**Response (200 OK):**
```json
{
  "categories": [
    {
      "id": "CAT001",
      "name": "Electronics",
      "slug": "electronics",
      "productCount": 1250,
      "children": [
        {
          "id": "CAT002",
          "name": "Computers",
          "slug": "computers",
          "productCount": 350,
          "children": []
        }
      ]
    }
  ]
}
```

---

## Order API

### POST /api/v1/orders

Create new order.

**Request:**
```json
{
  "items": [
    {
      "productId": "P001",
      "quantity": 10,
      "note": "Urgent delivery"
    }
  ],
  "deliveryAddress": {
    "companyName": "Tech Corp GmbH",
    "street": "Hauptstrasse 123",
    "city": "Berlin",
    "postalCode": "10115",
    "country": "DE",
    "contactPerson": "John Doe",
    "phone": "+49 30 12345678"
  },
  "paymentMethod": "INVOICE",
  "deliveryMethod": "STANDARD",
  "notes": "Please deliver before 15:00"
}
```

**Response (201 Created):**
```json
{
  "orderId": "ORD-2026-0001",
  "orderNumber": "ORD-2026-0001",
  "status": "NEW",
  "items": [
    {
      "productId": "P001",
      "productName": "Laptop Dell XPS 15",
      "quantity": 10,
      "unitPrice": 1169.99,
      "totalPrice": 11699.90
    }
  ],
  "subtotal": 11699.90,
  "discount": 584.995,
  "discountPercent": 5,
  "tax": 2226.89,
  "total": 13341.79,
  "currency": "EUR",
  "invoiceUrl": null,
  "estimatedDeliveryDate": "2026-01-20",
  "createdAt": "2026-01-15T16:00:00Z"
}
```

**Validation Rules:**
- Minimum order amount: 500 EUR
- Maximum items per order: 100
- Product must be in stock
- Customer credit limit not exceeded

### GET /api/v1/orders

List user's orders.

**Query Parameters:**
- `page` (int, default: 0)
- `size` (int, default: 20)
- `status` (string: NEW, CONFIRMED, PAID, PACKED, SHIPPED, DELIVERED, CANCELLED)
- `fromDate` (ISO date)
- `toDate` (ISO date)

**Response (200 OK):**
```json
{
  "content": [
    {
      "orderId": "ORD-2026-0001",
      "orderNumber": "ORD-2026-0001",
      "status": "PAID",
      "itemCount": 3,
      "totalAmount": 13341.79,
      "currency": "EUR",
      "createdAt": "2026-01-15T16:00:00Z",
      "estimatedDeliveryDate": "2026-01-20"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

### GET /api/v1/orders/{orderId}

Get order details.

**Response (200 OK):**
```json
{
  "orderId": "ORD-2026-0001",
  "orderNumber": "ORD-2026-0001",
  "status": "PAID",
  "statusHistory": [
    {"status": "NEW", "timestamp": "2026-01-15T16:00:00Z"},
    {"status": "CONFIRMED", "timestamp": "2026-01-15T16:05:00Z"},
    {"status": "PAID", "timestamp": "2026-01-15T17:30:00Z"}
  ],
  "items": [...],
  "deliveryAddress": {...},
  "invoiceUrl": "https://api.webshop.example.com/invoices/INV-2026-0001.pdf",
  "trackingNumber": "DHL123456789",
  "trackingUrl": "https://tracking.dhl.com/123456789",
  "estimatedDeliveryDate": "2026-01-20",
  "actualDeliveryDate": null,
  "createdAt": "2026-01-15T16:00:00Z",
  "updatedAt": "2026-01-15T17:30:00Z"
}
```

### PUT /api/v1/orders/{orderId}/cancel

Cancel order (only if status is NEW or CONFIRMED).

**Response (200 OK):**
```json
{
  "orderId": "ORD-2026-0001",
  "status": "CANCELLED",
  "cancelledAt": "2026-01-15T18:00:00Z",
  "cancellationReason": "Customer request"
}
```

---

## Payment API

### GET /api/v1/invoices/{invoiceId}

Download invoice PDF.

**Response (200 OK):**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="INV-2026-0001.pdf"

[PDF binary data]
```

### GET /api/v1/invoices

List invoices.

**Query Parameters:**
- `page` (int)
- `size` (int)
- `status` (UNPAID, PAID, OVERDUE)

**Response (200 OK):**
```json
{
  "content": [
    {
      "invoiceId": "INV-2026-0001",
      "invoiceNumber": "INV-2026-0001",
      "orderId": "ORD-2026-0001",
      "status": "PAID",
      "amount": 13341.79,
      "currency": "EUR",
      "dueDate": "2026-02-14",
      "paidDate": "2026-01-15T17:30:00Z",
      "downloadUrl": "/api/v1/invoices/INV-2026-0001",
      "createdAt": "2026-01-15T16:05:00Z"
    }
  ]
}
```

---

## User API

### GET /api/v1/users/profile

Get current user profile.

**Response (200 OK):**
```json
{
  "userId": "uuid",
  "email": "user@company.com",
  "name": "John Doe",
  "phone": "+49 30 12345678",
  "company": {
    "id": "COMP-123",
    "name": "Tech Corp GmbH",
    "taxId": "DE123456789",
    "address": {
      "street": "Hauptstrasse 123",
      "city": "Berlin",
      "postalCode": "10115",
      "country": "DE"
    },
    "tier": "GOLD",
    "creditLimit": 50000.00,
    "creditUsed": 15000.00,
    "discountPercent": 10
  },
  "preferences": {
    "language": "de",
    "currency": "EUR",
    "emailNotifications": true,
    "smsNotifications": false
  },
  "statistics": {
    "totalOrders": 145,
    "totalSpent": 458900.50,
    "averageOrderValue": 3165.18
  }
}
```

### PUT /api/v1/users/profile

Update user profile.

**Request:**
```json
{
  "name": "John Doe",
  "phone": "+49 30 12345678",
  "preferences": {
    "language": "de",
    "emailNotifications": true
  }
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile updated successfully"
}
```

---

## Error Handling

### Standard Error Response

All errors return:
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": "Additional details about the error",
    "timestamp": "2026-01-15T16:00:00Z",
    "path": "/api/v1/orders"
  }
}
```

### HTTP Status Codes

| Code | Description | Example |
|------|-------------|---------|
| 200 | Success | Request processed successfully |
| 201 | Created | Order created |
| 400 | Bad Request | Invalid input data |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Product out of stock |
| 422 | Unprocessable Entity | Validation failed |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |

### Common Error Codes

**Authentication Errors:**
- `AUTH_INVALID_CREDENTIALS` - Invalid email or password
- `AUTH_TOKEN_EXPIRED` - JWT token expired
- `AUTH_TOKEN_INVALID` - Invalid JWT token

**Validation Errors:**
- `VALIDATION_FAILED` - Input validation failed
- `PRODUCT_NOT_FOUND` - Product does not exist
- `INSUFFICIENT_STOCK` - Not enough items in stock
- `MINIMUM_ORDER_NOT_MET` - Order below minimum amount (500 EUR)

**Business Logic Errors:**
- `CREDIT_LIMIT_EXCEEDED` - Order exceeds credit limit
- `ORDER_CANNOT_BE_CANCELLED` - Order already shipped
- `PRODUCT_UNAVAILABLE` - Product discontinued

### Error Examples

**400 Bad Request:**
```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Validation failed for order creation",
    "details": {
      "items[0].quantity": "Must be at least 1",
      "deliveryAddress.postalCode": "Invalid postal code format"
    }
  }
}
```

**409 Conflict:**
```json
{
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "Insufficient stock for product P001",
    "details": "Requested: 100, Available: 45"
  }
}
```

---

## Rate Limiting

Rate limits are enforced per API key:

| Tier | Limit |
|------|-------|
| Bronze | 100 requests/hour |
| Silver | 500 requests/hour |
| Gold | 1,000 requests/hour |
| Platinum | 5,000 requests/hour |

**Rate Limit Headers:**
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1704070800
```

**Rate Limit Exceeded (429):**
```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "API rate limit exceeded",
    "details": "Limit: 1000 requests/hour. Try again in 30 minutes."
  }
}
```

---

## Webhooks

Configure webhooks to receive real-time notifications.

### Available Events:
- `order.created`
- `order.confirmed`
- `order.paid`
- `order.shipped`
- `order.delivered`
- `order.cancelled`
- `invoice.created`
- `payment.received`

### Webhook Payload:
```json
{
  "event": "order.shipped",
  "timestamp": "2026-01-20T10:00:00Z",
  "data": {
    "orderId": "ORD-2026-0001",
    "trackingNumber": "DHL123456789",
    "estimatedDelivery": "2026-01-22"
  }
}
```

---

## Testing

### Sandbox Environment
- **Base URL:** `https://sandbox-api.webshop.example.com`
- **Test Credentials:** test@company.com / TestPassword123

### Test Cards (Stripe):
- Success: `4242 4242 4242 4242`
- Declined: `4000 0000 0000 0002`

---

## Support

- **API Issues:** api-support@webshop.example.com
- **Documentation:** https://docs.webshop.example.com
- **Status Page:** https://status.webshop.example.com

**Last Updated:** 2026-01-15
