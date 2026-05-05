# E2E / smoke: Android Emulator → Ktor → PostgreSQL

Ручной чеклист для проверки, что связка **Docker (Postgres) → сервер → эмулятор** работает. Только локальная разработка; учётные данные демо — см. `LOCAL_RUN.md`.

**Предусловия:** установлены Docker Desktop, JDK 17, Android Studio; проект склонирован.

---

## Шаг 1 — Postgres

```bash
cd Warehouse_accounting_server
docker compose up -d
docker ps
```

Ожидается контейнер `warehouse-postgres`, порт **5433 → 5432**.

Проверка синтаксиса compose (без запуска контейнеров):

```bash
docker compose config
```

---

## Шаг 2 — сервер

Из `Warehouse_accounting_server`:

```bash
./gradlew run
```

Windows:

```bat
.\gradlew.bat run
```

Для **локальной** разработки **`JWT_SECRET` можно не задавать** — будет fallback из `application.conf` (только для dev). Для **production/staging** нужны **`APP_ENV`** и **`JWT_SECRET`** в окружении — см. `LOCAL_RUN.md`, раздел JWT.

---

## Шаг 3 — health

```bash
curl -s -w "\nHTTP %{http_code}\n" http://localhost:8080/api/health
```

Ожидается **200** и JSON вида:

```json
{"status":"ok","database":"ok"}
```

При проблемах с БД возможен **503** (см. `LOCAL_RUN.md`, раздел `/api/health`).

---

## Шаг 4–6 — Android

1. Запустить AVD в Android Studio.
2. Убедиться, что **debug** API base URL указывает на хост: **`http://10.0.2.2:8080`** (см. `Warehouse_accounting_app/local.properties` и `LOCAL_RUN.md`).
3. Установить и открыть приложение (debug-сборка).

**Вход:** `admin@warehouse.local` / `admin123` (только локальный демо-пароль).

---

## Шаг 7–9 — UI (ADMIN)

После входа под ролью **ADMIN** проверить навигацию (без глубокой валидации бизнес-правил):

| Экран / раздел      | Ожидание                          |
|---------------------|-----------------------------------|
| Dashboard           | открывается, нет «вечного» лупинга |
| Users               | список / экран доступен           |
| Categories          | список                            |
| Products            | список                            |
| StockBalances       | остатки                           |
| Receipt / Issue / WriteOff / Inventory | формы открываются (роли см. приложение) |
| OperationHistory    | история                           |
| Reports             | отчёты (роль MANAGER/ADMIN)       |
| Profile             | данные пользователя               |
| Logout              | выход на экран входа              |

Точные права ролей не менялись — если пункт недоступен роли, это ожидаемо.

---

## Дополнительно: curl после логина

Подставьте токен из ответа `login` в `TOKEN` (PowerShell / bash).

**Логин:**

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@warehouse.local\",\"password\":\"admin123\"}"
```

**Профиль:**

```bash
curl -s http://localhost:8080/api/auth/me -H "Authorization: Bearer TOKEN"
```

**Категории:**

```bash
curl -s http://localhost:8080/api/categories -H "Authorization: Bearer TOKEN"
```

**Товары:**

```bash
curl -s "http://localhost:8080/api/products?activeOnly=true" -H "Authorization: Bearer TOKEN"
```

**Остатки:**

```bash
curl -s http://localhost:8080/api/stock/balances -H "Authorization: Bearer TOKEN"
```

**Отчёты (пример — сводка):**

```bash
curl -s http://localhost:8080/api/reports/stock-summary -H "Authorization: Bearer TOKEN"
```

---

## См. также

- **`LOCAL_RUN.md`** — порты, безопасность, Android release URL, переменные окружения.
- **`MIGRATIONS_NOTES.md`** — стратегия V8 и новых сидов.
