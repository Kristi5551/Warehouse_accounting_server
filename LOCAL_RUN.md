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

```bash
cd Warehouse_accounting_server
./gradlew run
```

---

## 5. Проверка работоспособности

```
GET http://localhost:8080/api/health
```

Ожидаемый ответ:

```json
{"status": "ok"}
```

Пока сервер подключается к БД (Flyway-миграции), возвращается `503 Service Unavailable`.  
Как только `/api/health` возвращает `200 {"status":"ok"}`, сервер готов.

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

В `Warehouse_accounting_app/local.properties` по умолчанию:

```properties
# api.base.url=http://10.0.2.2:8080   ← значение по умолчанию, строку можно не добавлять
```

Если запускаете на реальном устройстве (не эмуляторе), добавьте:

```properties
api.base.url=http://IP_ВАШЕГО_ПК:8080
```

---

## 9. Переменные окружения (справочник)

| Переменная    | По умолчанию                                              | Назначение          |
|---------------|-----------------------------------------------------------|---------------------|
| `JDBC_URL`    | `jdbc:postgresql://localhost:5433/warehouse_db?...`       | JDBC URL для Postgres |
| `DB_USER`     | `warehouse_user`                                          | Пользователь БД     |
| `DB_PASSWORD` | `warehouse_password`                                      | Пароль БД           |
| `JWT_SECRET`  | `change-this-secret-for-local-development`                | Секрет для JWT      |
