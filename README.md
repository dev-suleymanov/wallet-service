# 💰 Wallet Service

REST API для управления балансом кошельков.
Поддерживает операции DEPOSIT и WITHDRAW, обеспечивает корректную обработку при высокой конкурентной нагрузке (до 1000 RPS по одному кошельку).

## 🚀 Стек технологий
- Java 17
- Spring Boot 3.5.7
- PostgreSQL
- Liquibase
- Docker & Docker Compose
- Spring Boot Test (JUnit + MockMvc)

## ⚙️ Функциональность
- POST /api/v1/wallet — пополнение или списание средств
- GET /api/v1/wallets/{walletId} — получение текущего баланса
- Защита от гонок и корректная работа при параллельных операциях
- Миграции базы данных через Liquibase
- Валидация входных данных и понятные ошибки

## 📦 Примеры запросов

### POST /api/v1/wallet
Тело запроса:
```json
{
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "operationType": "DEPOSIT",
  "amount": 1000
}
```

Ответ:
```json
{
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "balance": 2000
}
```

### GET /api/v1/wallets/{walletId}
Пример запроса:
```
GET http://localhost:8080/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000
```

Ответ:
```json
{
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "balance": 2000
}
```

## 🌍 Переменные окружения
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=wallet_db
DB_USER=wallet_user
DB_PASSWORD=wallet_password
APP_PORT=8080
```

## 🐳 Запуск через Docker Compose
```
docker compose up -d
```
