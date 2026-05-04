# 📚 Electronic Diary — Система управления образовательным процессом

> Высоконагруженное SaaS-приложение класса Enterprise для автоматизации административных, учебных и коммуникационных процессов в школах.

**Стек:** Java 17 · Spring Boot 3.5.7 · Spring Security + JWT · PostgreSQL · Flyway · Thymeleaf · Docker

---

## 📋 Содержание

- [Требования](#-требования)
- [Быстрый старт](#-быстрый-старт)
- [Настройка переменных окружения](#-настройка-переменных-окружения)
- [Запуск базы данных](#-запуск-базы-данных)
- [Запуск приложения](#-запуск-приложения)
- [Проверка работы](#-проверка-работы)
- [Первоначальная настройка школы](#-первоначальная-настройка-школы)
- [Структура проекта](#-структура-проекта)
- [Конфигурация](#-конфигурация)
- [Роли и доступ](#-роли-и-доступ)
- [API документация](#-api-документация)
- [Устранение ошибок](#-устранение-ошибок)
- [Деплой в продакшен](#-деплой-в-продакшен)

---

## 🛠 Требования

Перед запуском убедитесь, что установлено следующее программное обеспечение:

| Инструмент | Версия | Зачем нужен |
|---|---|---|
| **JDK** | 17 или выше | Компиляция и запуск приложения |
| **Maven** | 3.6+ (или встроенный Wrapper) | Сборка проекта |
| **Docker** | 20.10+ | Запуск PostgreSQL в контейнере |
| **Docker Compose** | 2.0+ | Оркестрация контейнеров |
| **Git** | Любая | Клонирование репозитория |

### Проверка версий

```bash
java -version       # должно быть >= 17
mvn -version        # или ./mvnw -version
docker -version
docker compose version
```

> **Примечание:** если Maven не установлен глобально — используйте Maven Wrapper (`./mvnw` на Linux/macOS или `mvnw.cmd` на Windows), он уже входит в репозиторий.

---

## 🚀 Быстрый старт

```bash
# 1. Клонировать репозиторий
git clone <URL_РЕПОЗИТОРИЯ>
cd school

# 2. Создать файл с переменными окружения
cp env.example .env       # или создать вручную (см. следующий раздел)

# 3. Запустить PostgreSQL
docker compose up -d

# 4. Собрать и запустить приложение
./mvnw spring-boot:run    # Linux / macOS
mvnw.cmd spring-boot:run  # Windows

# 5. Открыть в браузере
# Лендинг:     http://localhost:8088
# Swagger UI:  http://localhost:8088/swagger-ui.html
```

---

## 🔑 Настройка переменных окружения

Приложение использует файл `.env` в корне проекта. Создайте его на основе шаблона:

```bash
cp env.example .env
```

Откройте `.env` и заполните значения:

```dotenv
# Пароль пользователя developer для PostgreSQL
DB_PASSWORD=ВАШ_СЛОЖНЫЙ_ПАРОЛЬ

# Секретный ключ для подписи JWT-токенов (минимум 32 символа, рекомендуется 64+)
JWT_SECRET=очень-длинный-секретный-ключ-минимум-32-символа

# Промокод для бесплатного доступа к платформе (опционально)
PROMOTIONAL_CODE=FREE_SCHOOL_2025
```

> ⚠️ **Важно:** файл `.env` добавлен в `.gitignore` — никогда не коммитьте его в репозиторий. Для продакшена используйте переменные окружения операционной системы или секреты CI/CD.

### Минимальный рабочий `.env`

```dotenv
DB_PASSWORD=MySecurePass123!
JWT_SECRET=myJwtSecretKeyThatIsAtLeast32CharactersLongForSecurity
```

---

## 🐘 Запуск базы данных

### Запуск PostgreSQL через Docker Compose

```bash
# Запустить контейнер в фоновом режиме
docker compose up -d

# Проверить статус
docker compose ps

# Посмотреть логи (если нужно)
docker compose logs postgres
```

После запуска PostgreSQL доступен на порту **5434** (нестандартный, чтобы не конфликтовать с локальной установкой):

- **Хост:** `localhost`
- **Порт:** `5434`
- **База данных:** `school_management_db`
- **Пользователь:** `developer`
- **Пароль:** значение `DB_PASSWORD` из `.env`

### Что происходит при первом запуске приложения

Flyway автоматически применяет все **27 миграций** при старте Spring Boot. Вам ничего делать не нужно — схема базы данных создастся сама:

```
V1  — инициализация схемы (users, roles, groups, marks, ...)
V2  — V9  — правки схемы, типов, полей
V10 — таблица schools (Multi-Tenancy)
V11 — поле access_password_hash для активации школы
V14 — таблица user_schools (связь пользователей со школами)
...
V27 — настройки системы оценивания
```

### Подключение к PostgreSQL напрямую

```bash
# Зайти в контейнер
docker exec -it school-management-system psql -U developer -d school_management_db

# Или через psql на хост-машине (если установлен)
psql -h localhost -p 5434 -U developer -d school_management_db
```

### Остановка и удаление контейнера

```bash
# Остановить (данные сохраняются)
docker compose stop

# Остановить и удалить контейнеры (данные сохраняются в volume)
docker compose down

# Полная очистка, включая данные БД (ВНИМАНИЕ: все данные будут удалены)
docker compose down -v
```

---

## ▶️ Запуск приложения

### Способ 1 — Maven Wrapper (рекомендуется)

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

### Способ 2 — собрать JAR и запустить

```bash
# Собрать (пропустить тесты для ускорения)
./mvnw clean package -DskipTests

# Запустить
java -jar target/school-0.0.1-SNAPSHOT.jar
```

### Способ 3 — с явной передачей переменных окружения

Если `.env` не подхватывается автоматически:

```bash
# Linux / macOS
DB_PASSWORD=MyPass JWT_SECRET=MySecretKey ./mvnw spring-boot:run

# Или загрузить из файла вручную
export $(cat .env | xargs) && ./mvnw spring-boot:run

# Windows (PowerShell)
$env:DB_PASSWORD="MyPass"; $env:JWT_SECRET="MySecretKey"; .\mvnw.cmd spring-boot:run
```

### Способ 4 — через IDE (IntelliJ IDEA)

1. Открыть проект: `File → Open → выбрать папку school`
2. Дождаться индексации и загрузки зависимостей Maven
3. Открыть `src/main/java/com/education/school/SchoolManagementSystemApplication.java`
4. Нажать зелёную кнопку ▶️ рядом с методом `main`
5. В настройках Run Configuration добавить переменные окружения (Edit Configurations → Environment variables)

---

## ✅ Проверка работы

После запуска приложение доступно на порту **8088**.

| URL | Описание |
|---|---|
| `http://localhost:8088` | Публичная главная страница (лендинг) |
| `http://localhost:8088/swagger-ui.html` | Интерактивная документация REST API |
| `http://localhost:8088/v3/api-docs` | OpenAPI спецификация в формате JSON |
| `http://localhost:8088/{schoolName}/login` | Страница входа для конкретной школы |
| `http://localhost:8088/pricing` | Калькулятор стоимости подписки |

### Проверка через curl

```bash
# Проверить, что сервер отвечает
curl -I http://localhost:8088

# Получить OpenAPI спецификацию
curl http://localhost:8088/v3/api-docs | head -20

# Попробовать войти (после создания школы и пользователя)
curl -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@school.com","password":"password123"}'
```

---

## 🏫 Первоначальная настройка школы

После запуска приложения нужно создать школу и первого пользователя. Это двухэтапный процесс.

### Шаг 1 — Регистрация школы

Перейдите на страницу `http://localhost:8088/pricing` или отправьте запрос напрямую:

```bash
curl -X POST http://localhost:8088/api/purchase \
  -H "Content-Type: application/json" \
  -d '{
    "schoolName": "SchoolA",
    "contactEmail": "director@schoola.ru",
    "studentCount": 200,
    "months": 12
  }'
```

В ответе или в логах приложения появится **временный пароль активации**.

> 💡 Посмотреть пароль в логах: `./mvnw spring-boot:run 2>&1 | grep -i "activation\|password\|пароль"`

### Шаг 2 — Активация школы и создание директора

```bash
curl -X POST http://localhost:8088/api/purchase/register \
  -H "Content-Type: application/json" \
  -d '{
    "schoolName": "SchoolA",
    "accessPassword": "ВРЕМЕННЫЙ_ПАРОЛЬ_ИЗ_ШАГА_1",
    "firstName": "Иван",
    "secondName": "Иванов",
    "email": "director@schoola.ru",
    "password": "DirectorPass123!"
  }'
```

### Шаг 3 — Вход в систему

```bash
curl -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "director@schoola.ru",
    "password": "DirectorPass123!"
  }'
```

Ответ вернёт JWT-токен:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaXJlY3RvckBzY2hvb2xhLnJ1..."
}
```

### Шаг 4 — Использование токена

```bash
# Сохранить токен
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# Запрос с авторизацией
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8088/api/SchoolA/teachers
```

### Шаг 5 — Вход через браузер

Перейдите на `http://localhost:8088/SchoolA/login` и войдите с данными директора. После входа вы попадёте в панель управления `/SchoolA/director`.

### Шаг 6 — Создание администратора

Через Swagger UI или запросом от имени директора создайте администратора, который займётся настройкой школы:

```bash
curl -X POST http://localhost:8088/api/SchoolA/admins \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Анна",
    "secondName": "Смирнова",
    "email": "admin@schoola.ru",
    "password": "AdminPass123!"
  }'
```

### Шаг 7 — Настройка учебного процесса

После создания администратора войдите как администратор и последовательно:

1. Создайте **предметы** (Математика, Физика, История…) — `/SchoolA/admin/discipline`
2. Создайте **учебные группы** (9А, 10Б…) — `/SchoolA/admin/group`
3. Создайте **учителей** с привязкой к предметам — `/SchoolA/admin/teacher`
4. Назначьте кураторов для групп
5. Создайте **учеников** с привязкой к группам — `/SchoolA/admin/student`
6. Создайте **родителей** и привяжите к детям — `/SchoolA/admin/parent`
7. Составьте **расписание** — `/SchoolA/admin/schedule`
8. Настройте **систему оценивания** (четверть/семестр/год) — `/SchoolA/admin/grading`

---

## 📁 Структура проекта

```
school/
├── .env                          # Переменные окружения (НЕ коммитить!)
├── env.example                   # Шаблон переменных окружения
├── docker-compose.yml            # PostgreSQL контейнер
├── pom.xml                       # Maven зависимости
├── mvnw / mvnw.cmd               # Maven Wrapper
│
└── src/
    └── main/
        ├── java/com/education/school/
        │   ├── config/           # SecurityConfig, JwtService, JwtFilter
        │   ├── controller/       # 22 REST-контроллера
        │   ├── service/          # Бизнес-логика (18+ сервисов)
        │   ├── repository/       # Spring Data JPA репозитории
        │   ├── entity/           # JPA сущности (20 классов)
        │   ├── dto/              # DTO объекты (35+ классов)
        │   └── handler/          # GlobalExceptionHandler
        │
        └── resources/
            ├── application.properties    # Конфигурация приложения
            ├── templates/               # 30+ Thymeleaf HTML-шаблонов
            ├── static/
            │   ├── js/                  # 35 JavaScript-модулей
            │   └── css/                 # site.css
            └── db/migration/            # 27 Flyway SQL-миграций
```

---

## ⚙️ Конфигурация

### `src/main/resources/application.properties`

```properties
# Порт сервера
server.port=8088

# Подключение к PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5434/school_management_db
spring.datasource.username=developer
spring.datasource.password=${DB_PASSWORD}

# Hibernate — только валидация схемы (Flyway управляет структурой)
spring.jpa.hibernate.ddl-auto=validate

# Flyway миграции
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Логирование SQL (для отладки, отключить в продакшене)
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```

### `docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:17
    container_name: school-management-system
    environment:
      POSTGRES_DB: school_management_db
      POSTGRES_USER: developer
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5434:5432"    # хост:контейнер
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

volumes:
  postgres_data:
```

### Изменение порта или хоста БД

Если PostgreSQL запущен не в Docker, или на другом порту, измените в `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://ВАШ_ХОСТ:ВАШ_ПОРТ/school_management_db
```

---

## 👥 Роли и доступ

| Роль | Описание | URL панели |
|---|---|---|
| `ADMIN` | Полное управление школой | `/{school}/admin` |
| `DIRECTOR` | Статистика, жалобы, персонал | `/{school}/director` |
| `TEACHER` | Оценки, расписание, курсы | `/{school}/teacher` |
| `STUDENT` | Оценки, расписание, новости | `/{school}/student` |
| `PARENT` | Успеваемость детей | `/{school}/parent` |
| `MINISTRY` | Сводные данные (резерв) | — |

### Формат URL (Multi-Tenancy)

Все URL имеют префикс `/{schoolName}/`, где `schoolName` — уникальное название школы, указанное при регистрации. Например:

```
http://localhost:8088/SchoolA/admin
http://localhost:8088/SchoolA/teacher
http://localhost:8088/SchoolA/student
```

---

## 📖 API документация

### Swagger UI

После запуска откройте: **`http://localhost:8088/swagger-ui.html`**

Здесь можно:
- Просматривать все эндпоинты сгруппированные по контроллерам
- Тестировать запросы прямо в браузере
- Авторизоваться с JWT-токеном (кнопка **Authorize** → ввести `Bearer <TOKEN>`)

### Основные группы эндпоинтов

| Группа | Базовый URL | Доступ |
|---|---|---|
| Аутентификация | `/api/auth/*` | Публичный |
| Регистрация школы | `/api/purchase/*` | Публичный |
| Администраторы | `/api/{school}/admins` | ADMIN |
| Учителя | `/api/{school}/teachers` | Auth |
| Ученики | `/api/{school}/students` | Auth |
| Родители | `/api/{school}/parents` | Auth |
| Группы | `/api/{school}/groups` | Auth |
| Предметы | `/api/{school}/disciplines` | Auth |
| Расписание | `/api/{school}/schedules` | Auth |
| Оценки | `/api/{school}/marks` | TEACHER |
| Посещаемость | `/api/{school}/attendance` | TEACHER |
| Курсы | `/api/{school}/courses` | Auth |
| Экзамены | `/api/{school}/exams` | Auth |
| Новости | `/api/{school}/news` | Auth |
| Опросы | `/api/{school}/polls` | Auth |
| Жалобы | `/api/{school}/complaints` | Auth |
| Уведомления | `/api/{school}/notifications` | Auth |
| Столовая | `/api/{school}/canteen` | Auth |
| Настройки оценок | `/api/{school}/admin/grading` | ADMIN |
| Профиль | `/api/{school}/profile` | Auth |

---

## 🔧 Устранение ошибок

### Ошибка: `Connection refused` к PostgreSQL

```
Failed to obtain JDBC Connection: Connection refused (localhost:5434)
```

**Решение:**
```bash
# Проверить статус контейнера
docker compose ps

# Если контейнер не запущен — запустить
docker compose up -d

# Проверить занятость порта
lsof -i :5434   # Linux/macOS
netstat -an | findstr 5434  # Windows
```

---

### Ошибка: `DB_PASSWORD` не определён

```
Failed to load ApplicationContext: Could not resolve placeholder 'DB_PASSWORD'
```

**Решение:**
```bash
# Убедиться, что файл .env существует
ls -la .env

# Создать .env если его нет
echo "DB_PASSWORD=YourPassword123" > .env
echo "JWT_SECRET=YourJwtSecretKeyAtLeast32Characters" >> .env

# Или экспортировать переменные вручную
export DB_PASSWORD=YourPassword123
export JWT_SECRET=YourJwtSecretKeyAtLeast32Characters
```

---

### Ошибка Flyway: `Migration checksum mismatch`

```
FlywayException: Validate failed: Migration V1 - checksum mismatch
```

**Решение** — сброс Flyway (только для разработки):
```sql
-- Подключиться к БД и удалить таблицу Flyway
DROP TABLE flyway_schema_history;
```
Затем перезапустить приложение — Flyway применит все миграции заново.

> ⚠️ Никогда не делайте это в продакшене!

---

### Ошибка: `Port 8088 is already in use`

```
Web server failed to start. Port 8088 was already in use.
```

**Решение:**
```bash
# Найти процесс на порту 8088
lsof -i :8088       # Linux/macOS
netstat -ano | findstr 8088  # Windows

# Убить процесс (подставить PID)
kill -9 <PID>       # Linux/macOS
taskkill /PID <PID> /F  # Windows

# Или сменить порт в application.properties
server.port=8090
```

---

### JWT токен не принимается

```
403 Forbidden — JWT token is invalid
```

**Возможные причины:**
1. Токен истёк (по умолчанию — 1 час)
2. `JWT_SECRET` изменился после выдачи токена — нужно войти заново
3. Токен передаётся без префикса `Bearer `

**Правильный формат заголовка:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

### Ошибка компиляции: `Java 17 required`

```
Source option 17 is not supported. Use --release 21 or higher.
```

**Решение:** установите JDK 17 или выше и убедитесь, что `JAVA_HOME` указывает на правильную версию:
```bash
java -version
echo $JAVA_HOME   # Linux/macOS
echo %JAVA_HOME%  # Windows
```

---

### Логи для отладки

```bash
# Запуск с подробным логированием
./mvnw spring-boot:run --debug 2>&1 | tee app.log

# Смотреть SQL-запросы в реальном времени
tail -f app.log | grep -E "Hibernate:|SQL:"

# Логи Docker-контейнера PostgreSQL
docker compose logs -f postgres
```

---

## 🌐 Деплой в продакшен

### Подготовка JAR-файла

```bash
# Сборка без тестов
./mvnw clean package -DskipTests

# Запуск JAR напрямую
java -jar target/school-0.0.1-SNAPSHOT.jar \
  --DB_PASSWORD=ProdPassword \
  --JWT_SECRET=ProdJwtSecret
```

### Через переменные окружения (рекомендуется)

```bash
export DB_PASSWORD=ProdSecurePassword
export JWT_SECRET=ProdJwtSecretKeyThatIsVeryLongAndSecure
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db-host:5432/school_management_db

java -jar target/school-0.0.1-SNAPSHOT.jar
```

### Рекомендации для продакшена

```properties
# Отключить отображение SQL
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=WARN

# Изменить порт если нужно
server.port=80

# Настроить HTTPS (рекомендуется через reverse proxy — nginx/caddy)
```

### Docker Compose для продакшена

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: school_management_db
      POSTGRES_USER: developer
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: always

  app:
    image: eclipse-temurin:17-jre
    working_dir: /app
    volumes:
      - ./target/school-0.0.1-SNAPSHOT.jar:/app/app.jar
    command: java -jar app.jar
    ports:
      - "8088:8088"
    environment:
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/school_management_db
    depends_on:
      - postgres
    restart: always

volumes:
  postgres_data:
```

---

## 📝 Полезные команды

```bash
# Сброс и пересоздание только БД
docker compose down -v && docker compose up -d

# Пересборка без кэша Maven
./mvnw clean install -DskipTests

# Посмотреть примененные Flyway миграции
docker exec -it school-management-system \
  psql -U developer -d school_management_db \
  -c "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank;"

# Количество пользователей в БД
docker exec -it school-management-system \
  psql -U developer -d school_management_db \
  -c "SELECT r.name, COUNT(u.id) FROM users u JOIN roles r ON u.role_id = r.id GROUP BY r.name;"
```

---

## 📄 Лицензия

Проприетарная лицензия. Все права защищены.

---

*Документация составлена на основе версии проекта v1.0.0 · Апрель 2026*
