# E2E / smoke: Android Emulator → Ktor → PostgreSQL

Ручной чеклист цепочки **Docker (PostgreSQL) → сервер Ktor → эмулятор Android**, плюс **curl-smoke API**, **конкурентный расход**, **роли** и **безопасность**.  
Учётные данные демо и порты: **`LOCAL_RUN.md`**. Даты в отчётах: **`API_DATE_RANGE.md`**. Детали атомарного списания: **`STOCK_CONCURRENCY_SMOKE.md`**.

**Предусловия:** Docker Desktop, JDK 17+, Android Studio; **БД не очищать** специально для чеклиста (не выполнять `docker compose down -v`).

---

## Шаг 1 — PostgreSQL

Из корня репозитория перейдите в модуль сервера и поднимите контейнер:

```bash
cd Warehouse_accounting_server
docker compose up -d
docker ps
```

Ожидается контейнер **`warehouse-postgres`**, порт хоста **5433 → 5432** внутри контейнера.

Проверка синтаксиса Compose (без запуска):

```bash
docker compose config
```

---

## Шаг 2 — сервер

Из каталога **`Warehouse_accounting_server`**:

**Windows (PowerShell / cmd):**

```bat
.\gradlew.bat run
```

**Linux / macOS:**

```bash
./gradlew run
```

Для локальной разработки **`JWT_SECRET` можно не задавать** (fallback в `application.conf`). Для имитации production см. **`LOCAL_RUN.md`**, раздел JWT.

---

## Шаг 3 — health

```bash
curl http://localhost:8080/api/health
```

Ожидается **HTTP 200** и JSON вида:

```json
{"status":"ok","database":"ok"}
```

(При проблемах с БД возможен **503** — см. **`LOCAL_RUN.md`**.)

---

## Шаг 4 — Android Emulator

1. Запустите AVD в Android Studio.
2. Установите **debug**-сборку приложения (см. **`Warehouse_accounting_app/README.md`**).

---

## Шаг 5 — базовый URL API на эмуляторе

Для эмулятора хост ПК — **`http://10.0.2.2:8080`**.  
В **`Warehouse_accounting_app/local.properties`** для debug по умолчанию можно не задавать строку (используется это значение) или явно:

```properties
api.base.url=http://10.0.2.2:8080
```

Подробнее: **`LOCAL_RUN.md`**, раздел «Android Emulator → сервер».

---

## Шаг 6 — вход под администратором

В приложении:

| Поле     | Значение              |
|----------|------------------------|
| Email    | `admin@warehouse.local` |
| Password | `admin123`             |

Только **локальный демо-пароль** из сида (`InitialDataSeed`).

---

## Шаг 7 — Dashboard (ADMIN)

После входа откройте **Dashboard**: экран открывается без «вечной» загрузки, виден набор разделов для роли **ADMIN**.

---

## Шаг 8 — разделы приложения (ADMIN)

Пройдите по пунктам (достаточно открыть экран и убедиться, что данные грузятся или форма доступна; без глубокой бизнес-валидации):

| Раздел           | Ожидание (ADMIN)                          |
|------------------|---------------------------------------------|
| **Users**        | Экран списка пользователей доступен.        |
| **Categories**   | Список категорий.                           |
| **Products**     | Список товаров.                             |
| **StockBalances**| Остатки по складам.                         |
| **LowStock**     | Низкие остатки (операционный список).       |
| **Receipt**      | Форма прихода доступна.                     |
| **Issue**        | Форма расхода доступна.                     |
| **WriteOff**     | Форма списания доступна.                    |
| **Inventory**    | Форма инвентаризации доступна.              |
| **OperationHistory** | История операций.                       |
| **Reports**      | Отчёты (сводка, операции, др.).             |
| **Profile**      | Профиль текущего пользователя.              |

Если пункт скрыт guard при другой роли — см. раздел **«Проверка ролей»** ниже.

---

## Шаг 9 — выход (logout)

Выполните **Logout** в приложении: возврат на экран входа, повторный запрос к защищённым API без нового логина не должен использовать старый токен.

---

## Curl-smoke API (после логина)

Подставьте **`TOKEN`** — строка JWT из поля **`token`** ответа **`POST /api/auth/login`**.

### Bash / zsh (пример с сохранением токена)

```bash
cd Warehouse_accounting_server
TOKEN="$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@warehouse.local","password":"admin123"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
# Если есть jq: TOKEN="$(curl -s ... | jq -r .token)"

curl -s http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"

curl -s http://localhost:8080/api/categories -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/api/products?activeOnly=true" -H "Authorization: Bearer $TOKEN"

curl -s http://localhost:8080/api/stock/balances -H "Authorization: Bearer $TOKEN"

curl -s http://localhost:8080/api/stock/low -H "Authorization: Bearer $TOKEN"

curl -s http://localhost:8080/api/operations -H "Authorization: Bearer $TOKEN"

curl -s http://localhost:8080/api/reports/stock-summary -H "Authorization: Bearer $TOKEN"
```

### Windows PowerShell

```powershell
$loginBody = '{"email":"admin@warehouse.local","password":"admin123"}'
$login = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$TOKEN = $login.token
$headers = @{ Authorization = "Bearer $TOKEN" }

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -Headers $headers
Invoke-RestMethod -Uri "http://localhost:8080/api/categories" -Headers $headers
Invoke-RestMethod -Uri "http://localhost:8080/api/products?activeOnly=true" -Headers $headers
Invoke-RestMethod -Uri "http://localhost:8080/api/stock/balances" -Headers $headers
Invoke-RestMethod -Uri "http://localhost:8080/api/stock/low" -Headers $headers
Invoke-RestMethod -Uri "http://localhost:8080/api/operations" -Headers $headers
Invoke-RestMethod -Uri "http://localhost:8080/api/reports/stock-summary" -Headers $headers
```

### POST приход и расход (подставьте `warehouseId`, `productId`, `price`)

Значения возьмите из **`GET /api/stock/balances`** или из БД после миграций/сида. Пример тела — валидный JSON:

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:8080/api/stock/receipt \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"warehouseId":1,"productId":1,"quantity":"10","price":"100.00","supplier":"smoke","comment":null}'
```

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:8080/api/stock/issue \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"warehouseId":1,"productId":1,"quantity":"1","reason":null,"comment":"smoke"}'
```

Ожидается **HTTP 201 Created** при успехе.

---

## Сценарий: двойной расход (конкуренция)

**Условие:** по выбранной паре **склад + товар** остаток ровно **5** (задайте приходом или инвентаризацией в UI / через API).

**Действие:** два HTTP-клиента отправляют **расход (issue)** на **4** единицы **почти одновременно** (два окна терминала, Postman, или скрипт с параллельным запуском).

**Ожидается:**

- один запрос успешен (**201**);
- второй — **409 Conflict**, сообщение вроде **«Недостаточно товара на складе»**;
- итоговый остаток **не отрицательный** (для сценария 5 − 4 остаётся **1**);
- **нет** «полу-сохранённой» операции: либо операция создана целиком, либо отказ без изменения остатка.

**PowerShell (два окна):** в обоих задайте `$TOKEN` как после логина admin (или кладовщика). В каждом окне выполните одну и ту же команду **`issue`** с одинаковыми `warehouseId` / `productId` / `quantity":"4"` и нажмите Enter **почти одновременно**:

```powershell
curl.exe -s -w "`nHTTP %{http_code}`n" -X POST http://localhost:8080/api/stock/issue `
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" `
  -d "{\"warehouseId\":1,\"productId\":1,\"quantity\":\"4\"}"
```

Затем проверьте **`GET /api/stock/balances`** и **`GET /api/operations`**.

Подробности реализации: **`STOCK_CONCURRENCY_SMOKE.md`**, **`LOCAL_RUN.md`** (раздел про конкурентные расходы).

---

## Проверка ролей (UI и/или API)

Подготовка: учётные записи **STOREKEEPER** и **MANAGER** можно получить через **саморегистрацию** (`/api/auth/register`) и **подтверждение ADMIN** (`Users` в приложении) либо создать администратором — см. API пользователей. Для проверки нужен **активный** пользователь с нужной ролью.

| Ожидание | ADMIN | STOREKEEPER | MANAGER |
|----------|:-----:|:-----------:|:-------:|
| Видит **Users** | да | нет* | нет* |
| Склад: остатки, история, формы Receipt/Issue/WriteOff/Inventory (мутации) | да | да | нет* (мутации) |
| **LowStock** (`GET /api/stock/low`) | да | да | да |
| **Reports** (`/api/reports/*`) | да | нет* | да |

\*Если пункт скрыт в Android — это ожидаемо по **`RolePermissions`**; сервер всё равно должен отвечать **403** на запрещённые маршруты для JWT этой роли.

Кратко:

- **ADMIN** — полный доступ к перечисленным разделам и отчётам.
- **STOREKEEPER** — складские операции и просмотр (в т.ч. **LowStock**); **Users** и **Reports** недоступны.
- **MANAGER** — **Reports**, **LowStock**, просмотр остатков/истории; **создание прихода/расхода/списания/инвентаризации** через API не допускается (**403**).

---

## Безопасность (smoke)

Выполните после старта сервера.

| Проверка | Как | Ожидание |
|----------|-----|----------|
| Запрос **без** `Authorization` к защищённому ресурсу | `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/categories` | **401** |
| **MANAGER**: складская мутация | `POST /api/stock/issue` с JWT пользователя **MANAGER** | **403** |
| **STOREKEEPER**: отчёт | `GET /api/reports/stock-summary` с JWT **STOREKEEPER** | **403** |

Пример без токена:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/stock/balances
```

Для **403** сначала получите два токена (MANAGER и STOREKEEPER) и подставьте в `Authorization: Bearer …`.

---

## Фильтры дат (отчёт по операциям и история)

Контракт query **`dateFrom`** / **`dateTo`**: см. **`API_DATE_RANGE.md`**.

| Сценарий | Ожидание |
|----------|----------|
| Отчёты → период пустой, обновить | Операции **без** фильтра по датам. |
| Только **от** / только **до** / один день | См. таблицу в **`API_DATE_RANGE.md`**. |
| История операций | Те же параметры к **`GET /api/operations`**. |
| «От» позже «до» в приложении | Сообщение валидации; запрос с некорректным диапазоном не уходит. |

---

## См. также

- **`LOCAL_RUN.md`** — порты, Postgres, JWT, эмулятор, `local.properties`.
- **`API_DATE_RANGE.md`** — формат дат и timezone.
- **`STOCK_CONCURRENCY_SMOKE.md`** — конкурентное списание.
- **`MIGRATIONS_NOTES.md`** — миграции и сиды.
