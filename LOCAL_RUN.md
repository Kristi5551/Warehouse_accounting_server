# Локальный запуск WarehouseAccounting Server

## Схема работы

```
Android Emulator
      ↓  http://10.0.2.2:8080
Ktor Server (порт 8080, запускается локально на ПК)
      ↓  localhost:5433
PostgreSQL (Docker-контейнер, хост-порт 5433 → контейнер 5432)
```

> **Почему порт 5433, а не 5432?**  
> Порт 5432 — стандартный порт PostgreSQL. На большинстве машин разработчика  
> уже запущен **локальный** PostgreSQL именно на этом порту. Если Docker-контейнер  
> попытается занять 5432, он не запустится (port conflict), и Ktor ошибочно  
> подключится к **локальному** серверу вместо Docker-контейнера — это приводит к  
> ошибке аутентификации (`warehouse_user` не существует в локальном PostgreSQL).  
> Порт 5433 решает эту проблему без дополнительной настройки.

---

## 1. Требования

- **Docker Desktop** — для PostgreSQL-контейнера  
- **JDK 17+** — для Ktor-сервера  
- **IntelliJ IDEA** (рекомендуется) или Gradle из командной строки  
- **Android Studio + эмулятор** — для запуска мобильного приложения  

### Версии инструментов (два независимых Gradle-проекта)

Репозиторий **WarehouseAccounting** содержит **отдельные** Gradle-проекты `Warehouse_accounting_app` и `Warehouse_accounting_server` **без общего Kotlin-модуля**. Версии Kotlin и Gradle в них **не обязаны совпадать** и **не конфликтуют**, пока нет общего `shared`-модуля. При появлении общего кода версии Kotlin/сериализации нужно будет **синхронизировать**.

**Android (`Warehouse_accounting_app`):**

| Компонент | Версия (по состоянию репозитория) |
|-----------|-----------------------------------|
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.13.2 |
| Gradle (wrapper) | 8.13 |
| Целевая JVM для app-модуля | 11 |
| Compose | через BOM `2025.05.00` (см. `gradle/libs.versions.toml`) |

**Server (`Warehouse_accounting_server`):**

| Компонент | Версия (по состоянию репозитория) |
|-----------|-----------------------------------|
| Kotlin | 2.1.10 |
| Gradle (wrapper) | 8.10 |
| JVM toolchain | 17 |
| Ktor | 3.0.3 |

Ручной сквозной сценарий «Docker → сервер → эмулятор», **curl-smoke**, конкурентный расход, роли и проверки безопасности см. **`E2E_CHECKLIST.md`**.

---

## 2. Запуск PostgreSQL

Перейдите в папку `Warehouse_accounting_server` и выполните:

```bash
docker compose up -d
```

**Проверить, что контейнер запущен:**

```bash
docker ps
```

Ожидаемый вывод (container `warehouse-postgres`, статус `healthy` или `Up`):

```
CONTAINER ID   IMAGE                COMMAND                  PORTS                    NAMES
xxxxxxxxxxxx   postgres:16-alpine   "docker-entrypoint.s…"   0.0.0.0:5433->5432/tcp   warehouse-postgres
```

**Остановить контейнер** (данные сохраняются в named volume):

```bash
docker compose down
```

**Полный сброс БД** (удалить данные):

```bash
docker compose down -v
```

---

## 3. Подключение к базе данных

**Через psql внутри контейнера:**

```bash
docker exec -it warehouse-postgres psql -U warehouse_user -d warehouse_db
```

**Через любой внешний клиент** (DBeaver, DataGrip, pgAdmin):

| Параметр | Значение           |
|----------|--------------------|
| Host     | localhost          |
| Port     | **5433**           |
| Database | warehouse_db       |
| User     | warehouse_user     |
| Password | warehouse_password |

---

## 4. Запуск сервера

### Из IntelliJ IDEA (рекомендуется)

1. Откройте папку `Warehouse_accounting_server` как Gradle-проект.
2. Запустите конфигурацию `run` (Gradle) или напрямую класс `MainKt`.
3. Сервер стартует на `http://0.0.0.0:8080`.

### Через Gradle

Из каталога `Warehouse_accounting_server`:

```bash
cd Warehouse_accounting_server
./gradlew run
```

В **Windows (PowerShell / cmd)**:

```bat
cd Warehouse_accounting_server
.\gradlew.bat run
```

---

## 5. Проверка работоспособности (`/api/health`)

Эндпоинт **не требует авторизации**. Удобно проверить из терминала:

```bash
curl -s http://localhost:8080/api/health
```

**Успешный ответ** (сервер проинициализирован, **пинг БД прошёл**), HTTP **200**:

```json
{
  "status": "ok",
  "database": "ok"
}
```

**База данных недоступна** (или проверка БД после старта не удалась), HTTP **503**:

```json
{
  "status": "unavailable",
  "database": "unavailable",
  "message": "База данных недоступна"
}
```

**Промежуточное состояние:** пока приложение ещё не пометило себя как полностью готовое (`ServerReadiness`), но соединение с БД уже удаётся, возможен **503** с телом без поля `database`, например `"message": "Сервис временно недоступен"` — это нормально при коротком окне после старта; повторите запрос через несколько секунд.

Пока остальные маршруты `/api/*` (кроме `/api/health`) могут отвечать **503** «сервер запускается» — см. логику готовности в `Application.kt`.

**Локальный демо-вход в API** (тот же пользователь, что и в приложении после `InitialDataSeed`):  
`admin@warehouse.local` / `admin123` (только для локальной разработки; см. раздел **«Локальная безопасность»** ниже).

---

## 6. Начальные данные

Подробно о легаси V8, checksum и правилах новых сидов: **[MIGRATIONS_NOTES.md](MIGRATIONS_NOTES.md)**.

Flyway применяет все миграции из `src/main/resources/db/migration/` по возрастанию версии (см. папку; актуальный набор — **V1 и выше**).

| Что создаётся  | Источник         |
|----------------|-----------------|
| Структура таблиц | Flyway V1–V7    |
| Легаси демо: склад (как в V8), категории, товары, остатки | Flyway **V8** (+ **V9** переименование склада; далее — инкрементальные миграции) |
| Учётная запись администратора; при отсутствии — склад «Основной склад» | Kotlin `InitialDataSeed` |

### Администратор системы

| Поле     | Значение               |
|----------|------------------------|
| Email    | admin@warehouse.local  |
| Password | admin123               |
| Роль     | ADMIN                  |
| Статус   | ACTIVE                 |

`admin123` здесь — **только локальный пароль демо-сида**; см. также раздел **«Локальная безопасность»** ниже.

---

## 7. Используется нестандартный порт. Как изменить

Проект по умолчанию использует порт **5433** на хосте, чтобы не конфликтовать  
с локальным PostgreSQL (если он установлен на машине).

Если нужно изменить порт (например, 5433 тоже занят), исправьте `docker-compose.yml`:

```yaml
ports:
  - "5434:5432"   # любой свободный хост-порт
```

И передайте переменную окружения при запуске сервера:

```bash
JDBC_URL="jdbc:postgresql://localhost:5434/warehouse_db?connectTimeout=10&socketTimeout=30" ./gradlew run
```

Или создайте файл `.env` в папке `Warehouse_accounting_server/`:

```
JDBC_URL=jdbc:postgresql://localhost:5434/warehouse_db?connectTimeout=10&socketTimeout=30
```

---

## 8. Android Emulator → сервер

Эмулятор обращается к серверу на ПК по адресу `10.0.2.2:8080`.  
Это стандартный IP-адрес хоста для Android AVD.

**Cleartext (HTTP):** в манифесте `android:usesCleartextTraffic` задаётся через placeholder **`cleartextTraffic`**:  
**debug** = `true` (разрешён HTTP, в т.ч. `http://10.0.2.2:8080`); **release** = `false` (только **HTTPS** к API — см. `api.base.url` и задачу `validateReleaseApiBaseUrl` в `app/build.gradle.kts`).

**HTTP** (`http://…`) в приложении разрешён **только для debug-сборок** (удобный локальный цикл с эмулятором).  
Для **production** на стороне клиента и сервера нужен **HTTPS**.

В `Warehouse_accounting_app/local.properties` для **debug** по умолчанию:

```properties
# api.base.url=http://10.0.2.2:8080   ← значение по умолчанию, строку можно не добавлять
```

Если запускаете на реальном устройстве (не эмуляторе), добавьте:

```properties
api.base.url=http://IP_ВАШЕГО_ПК:8080
```

**Release-сборка приложения:** в `local.properties` обязательно задайте **HTTPS** базовый URL API, например:

```properties
api.base.url=https://api.ваш-домен.example
```

Без этого Gradle не соберёт `release` (cleartext в release отключён; фиктивный production URL в репозиторий не зашивается).

---

## 9. Локальная безопасность, CORS и секреты

### Демо-администратор

| Поле     | Локально по умолчанию     |
|----------|---------------------------|
| Email    | `admin@warehouse.local`  |
| Password | `admin123`               |

Пароль **`admin123` — только для локального демо-сида** (`InitialDataSeed`). В **production** этим паролем пользоваться нельзя.  
При необходимости задайте другой пароль для сида через переменную окружения **`ADMIN_PASSWORD`** (см. таблицу ниже) или не полагайтесь на автоматический сид в production.

### JWT

- **Локально и учебный запуск:** если **`APP_ENV` не задан** (или не `production` / `prod` / `staging`), можно не задавать `JWT_SECRET` — подставится значение по умолчанию из `application.conf`. Это **не боевой** секрет, его знают все, у кого есть репозиторий; подходит только для dev.
- **Production / staging:** задайте **`APP_ENV=production`** (или `prod` / `staging`) и обязательно **`JWT_SECRET`** в окружении процесса. Иначе сервер при старте завершится с ошибкой (см. `AppConfig`). Боевой секрет **не храните** в git и не кладите в `application.conf`.
- Подстановка: в `application.conf` строка `secret = ${?JWT_SECRET}` — при наличии переменной используется она.

**Пример (PowerShell, локально с явным секретом):**

```powershell
$env:JWT_SECRET = "ваш-случайный-длинный-секрет"
.\gradlew.bat run
```

**Пример (PowerShell, имитация production — без JWT_SECRET процесс не стартует):**

```powershell
$env:APP_ENV = "production"
# обязательно:
$env:JWT_SECRET = "криптостойкая-случайная-строка"
.\gradlew.bat run
```

**Пример (bash):**

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
./gradlew run
```

### CORS (Ktor)

Плагин CORS на сервере настроен для **браузерных** клиентов при локальной разработке (ограниченный список `localhost` / `127.0.0.1` и портов).  
Нативный **Android** к CORS не привязан. Для **production** список origin в коде нужно сузить до реального домена вашего фронтенда.

---

## 10. Переменные окружения (справочник)

| Переменная       | По умолчанию                                              | Назначение          |
|------------------|-----------------------------------------------------------|---------------------|
| `APP_ENV`        | *(не задана)*                                             | Если `production`, `prod` или `staging`, **обязателен** `JWT_SECRET` (иначе старт упадёт) |
| `JDBC_URL`       | `jdbc:postgresql://localhost:5433/warehouse_db?...`       | JDBC URL для Postgres |
| `DB_USER`        | `warehouse_user`                                          | Пользователь БД     |
| `DB_PASSWORD`    | `warehouse_password`                                      | Пароль БД           |
| `JWT_SECRET`     | `change-this-secret-for-local-development` *(только если не задана env; только local/dev)* | Секрет подписи JWT (**в production/staging только из env**, не из репозитория) |
| `ADMIN_PASSWORD` | *(не задана → как у демо-сида локально)* `admin123`        | Пароль учётки из `InitialDataSeed` при первом создании |

---

## 11. Отчёты: период дат и smoke конкурентных расходов

### Период в отчётах и истории

Полный контракт (формат, открытые интервалы, timezone, список эндпоинтов): **[API_DATE_RANGE.md](API_DATE_RANGE.md)**.

Кратко: параметры **`dateFrom`** / **`dateTo`** — **`yyyy-MM-dd`**. Нижняя граница **включительно** (`>= dateFrom.atStartOfDay()`), верхняя **`dateTo` — весь день включительно** через верхнюю границу **исключающую полуночь следующего дня** (`created_at < dateTo.plusDays(1).atStartOfDay()`). **Timezone:** локальная JVM сервера; в БД `created_at` без таймзоны — та же семантика «настенного календаря». Реализация: `ReportDateBounds`, разбор query: `parseDateQuery` в **`OperationQueryParsers.kt`**.

### Smoke: два параллельных расхода при остатке 5

Проверка **потери остатка при гонке** (один успех, второй отказ, остаток ≥ 0):

1. Убедитесь, что у выбранного товара на складе остаток **5** (приход или инвентаризация).
2. Дважды почти одновременно вызовите **расход (issue)** этого товара на **4** шт. (два HTTP-клиента, скрипт или две вкладки с автоматизацией).
3. **Ожидание:** один ответ **200**, второй **409** с текстом вроде «Недостаточно товара на складе»; итоговый остаток **1** (не отрицательный).

Сервер использует атомарное `UPDATE … WHERE quantity >= :qty` до записи операции; при нуле обновлённых строк операция не создаётся.
