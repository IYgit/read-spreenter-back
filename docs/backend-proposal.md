# Read Sprinter — Пропозиція бекенду

## Зміст
1. [Поточний стан фронтенду](#1-поточний-стан-фронтенду)
2. [Загальна архітектура бекенду](#2-загальна-архітектура-бекенду)
3. [Модель даних (БД PostgreSQL)](#3-модель-даних-бд-postgresql)
4. [REST API](#4-rest-api)
5. [WebSocket — груповий режим](#5-websocket--груповий-режим)
6. [Аутентифікація та авторизація](#6-аутентифікація-та-авторизація)
7. [Функціонал, не реалізований на фронтенді](#7-функціонал-не-реалізований-на-фронтенді)
8. [Додаткові можливості](#8-додаткові-можливості)
9. [Технологічний стек](#9-технологічний-стек)
10. [Flyway-міграції](#10-flyway-міграції)
11. [Поетапний план реалізації](#11-поетапний-план-реалізації)

---

## 1. Поточний стан фронтенду

### Що вже реалізовано
| Компонент | Опис |
|---|---|
| **AuthPage** | Реєстрація та вхід — повністю на `localStorage` (без бекенду) |
| **8 вправ** | Numbers, Word Pairs, Word Search, RSVP, Syntagm Reading, Schulte Table, Letter Search, Duel |
| **DuelExercise** | Локальний дуель — два гравці по черзі на одному пристрої |
| **GameScreen** | Читання тексту + відповіді на запитання, підрахунок балів |
| **ResultsScreen** | Відображення результатів двох гравців |
| **ExerciseStatsChart** | Графік прогресу — зберігається в `localStorage` |

### Ключові спостереження
- Аутентифікація — лише `localStorage`, пароль зберігається у відкритому вигляді
- Статистика прив'язана до `localStorage` конкретного браузера, між пристроями не синхронізується
- Дуель — локальна: обидва гравці на одному пристрої по черзі, реального мультиплеєра немає
- Тексти та запитання — хардкод у `data/texts.ts`
- Бали рахуються за формулою: `100 (за правильну відповідь) + max(0, (10 - час_відповіді) * 10)`

---

## 2. Загальна архітектура бекенду

```
┌─────────────────────────────────────────────────────────┐
│                     React Frontend                       │
└────────────┬───────────────────────┬────────────────────┘
             │ REST API (HTTP/HTTPS)  │ WebSocket (STOMP)
┌────────────▼───────────────────────▼────────────────────┐
│               Spring Boot Application                    │
│  ┌──────────────┐  ┌────────────────┐  ┌─────────────┐  │
│  │  Auth Module │  │  Exercise API  │  │  Duel/WS    │  │
│  │  (JWT)       │  │  (REST)        │  │  Module     │  │
│  └──────────────┘  └────────────────┘  └─────────────┘  │
│  ┌──────────────┐  ┌────────────────┐  ┌─────────────┐  │
│  │  User API    │  │  Stats API     │  │  Text API   │  │
│  └──────────────┘  └────────────────┘  └─────────────┘  │
└─────────────────────────────┬───────────────────────────┘
                              │ JPA / Flyway
┌─────────────────────────────▼───────────────────────────┐
│                   PostgreSQL Database                    │
└─────────────────────────────────────────────────────────┘
```

---

## 3. Модель даних (БД PostgreSQL)

### Таблиця `users`
```sql
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  UNIQUE NOT NULL,
    email       VARCHAR(255) UNIQUE,
    password    VARCHAR(255) NOT NULL,  -- BCrypt
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

### Таблиця `texts`
```sql
CREATE TABLE texts (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    difficulty  VARCHAR(20)  NOT NULL,  -- easy | medium | hard
    language    VARCHAR(10)  NOT NULL DEFAULT 'uk',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

### Таблиця `questions`
```sql
CREATE TABLE questions (
    id           BIGSERIAL PRIMARY KEY,
    text_id      BIGINT       NOT NULL REFERENCES texts(id) ON DELETE CASCADE,
    question     TEXT         NOT NULL,
    options      JSONB        NOT NULL,  -- ["варіант1", "варіант2", ...]
    correct_idx  SMALLINT     NOT NULL,
    sort_order   SMALLINT     NOT NULL DEFAULT 0
);
```

### Таблиця `exercise_results`
```sql
CREATE TABLE exercise_results (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id),
    exercise_type   VARCHAR(50) NOT NULL,  -- numbers | rsvp | word-pairs | ...
    text_id         BIGINT      REFERENCES texts(id),
    score           INT         NOT NULL DEFAULT 0,
    correct_answers SMALLINT,
    total_questions SMALLINT,
    reading_time_ms INT,                   -- час читання в мс
    wpm             INT,                   -- слів на хвилину (для RSVP, syntagm)
    completed_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    extra_data      JSONB                  -- додаткові метрики (специфічні для вправи)
);
```

### Таблиця `duel_sessions`
```sql
CREATE TABLE duel_sessions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    text_id         BIGINT      NOT NULL REFERENCES texts(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'WAITING',
                    -- WAITING | READING | QUESTIONS | FINISHED | CANCELLED
    player1_id      BIGINT      NOT NULL REFERENCES users(id),
    player2_id      BIGINT      REFERENCES users(id),
    player1_result_id BIGINT    REFERENCES exercise_results(id),
    player2_result_id BIGINT    REFERENCES exercise_results(id),
    winner_id       BIGINT      REFERENCES users(id),
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    finished_at     TIMESTAMP
);
```

### Таблиця `duel_progress` (real-time стан дуелі)
```sql
CREATE TABLE duel_progress (
    id              BIGSERIAL PRIMARY KEY,
    session_id      UUID        NOT NULL REFERENCES duel_sessions(id) ON DELETE CASCADE,
    user_id         BIGINT      NOT NULL REFERENCES users(id),
    read_progress   SMALLINT    NOT NULL DEFAULT 0,  -- % прочитаного (0-100)
    current_score   INT         NOT NULL DEFAULT 0,
    phase           VARCHAR(20) NOT NULL DEFAULT 'READING',
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (session_id, user_id)
);
```

### Схема зв'язків
```
users ──< exercise_results
users ──< duel_sessions (player1, player2, winner)
texts ──< questions
texts ──< exercise_results
texts ──< duel_sessions
duel_sessions ──< duel_progress
```

---

## 4. REST API

### 4.1 Аутентифікація `/api/auth`
| Метод | URL | Опис | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Реєстрація нового користувача | ❌ |
| POST | `/api/auth/login` | Вхід, отримання JWT | ❌ |
| POST | `/api/auth/refresh` | Оновлення access-токена | ❌ (refresh token) |
| POST | `/api/auth/logout` | Анулювання refresh-токена | ✅ |

**POST /api/auth/register**
```json
// Request
{ "username": "john", "password": "secret123" }

// Response 201
{ "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

**POST /api/auth/login**
```json
// Request
{ "username": "john", "password": "secret123" }

// Response 200
{ "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

---

### 4.2 Користувачі `/api/users`
| Метод | URL | Опис | Auth |
|---|---|---|---|
| GET | `/api/users/me` | Профіль поточного користувача | ✅ |
| PUT | `/api/users/me` | Оновлення імені/пошти | ✅ |
| GET | `/api/users/search?q=` | Пошук гравця для дуелі | ✅ |
| GET | `/api/users/{id}/stats` | Публічна статистика користувача | ✅ |

---

### 4.3 Тексти `/api/texts`
| Метод | URL | Опис | Auth |
|---|---|---|---|
| GET | `/api/texts` | Список усіх текстів | ✅ |
| GET | `/api/texts?difficulty=easy` | Фільтрація за складністю | ✅ |
| GET | `/api/texts/{id}` | Отримати текст з питаннями | ✅ |
| POST | `/api/texts` | Додати текст (ADMIN) | ✅ ADMIN |
| PUT | `/api/texts/{id}` | Оновити текст (ADMIN) | ✅ ADMIN |
| DELETE | `/api/texts/{id}` | Видалити текст (ADMIN) | ✅ ADMIN |

**GET /api/texts/{id} — Response 200**
```json
{
  "id": 1,
  "title": "Прокрастинація",
  "content": "У психології...",
  "difficulty": "medium",
  "language": "uk",
  "questions": [
    {
      "id": 1,
      "text": "Що таке прокрастинація?",
      "options": ["варіант 1", "варіант 2", "варіант 3", "варіант 4"],
      "correctIndex": 1
    }
  ]
}
```

---

### 4.4 Результати вправ `/api/results`
| Метод | URL | Опис | Auth |
|---|---|---|---|
| POST | `/api/results` | Зберегти результат вправи | ✅ |
| GET | `/api/results/me` | Всі результати поточного користувача | ✅ |
| GET | `/api/results/me?exerciseType=rsvp` | Результати по типу вправи | ✅ |
| GET | `/api/results/me/summary` | Агрегована статистика (макс/серед/кількість) | ✅ |

**POST /api/results — Request**
```json
{
  "exerciseType": "rsvp",
  "textId": 1,
  "score": 250,
  "correctAnswers": 3,
  "totalQuestions": 3,
  "readingTimeMs": 45000,
  "wpm": 220,
  "extraData": { "syntagmWidth": 2, "displayTimeMs": 300 }
}
```

---

### 4.5 Дуелі `/api/duels`
| Метод | URL | Опис | Auth |
|---|---|---|---|
| POST | `/api/duels` | Створити сесію дуелі | ✅ |
| POST | `/api/duels/{id}/join` | Приєднатися до сесії | ✅ |
| GET | `/api/duels/{id}` | Стан сесії | ✅ |
| GET | `/api/duels/me` | Мої дуелі (з результатами) | ✅ |
| DELETE | `/api/duels/{id}` | Скасувати сесію | ✅ |

---

## 5. WebSocket — груповий режим

### Вибір технології: Spring WebSocket + STOMP over SockJS

**Чому STOMP:**
- Підтримка pub/sub топіків
- Spring має нативну підтримку (`@MessageMapping`, `SimpMessagingTemplate`)
- Легко інтегрується з JWT-аутентифікацією
- SockJS дає fallback для браузерів без WS

### Потоки повідомлень

```
Client → Server (публікація):
  /app/duel/{sessionId}/ready          -- гравець готовий
  /app/duel/{sessionId}/progress       -- оновлення прогресу читання
  /app/duel/{sessionId}/answer         -- відповідь на запитання

Server → Client (підписка):
  /topic/duel/{sessionId}              -- broadcast стан сесії всім учасникам
  /user/queue/duel/{sessionId}         -- персональні повідомлення (результат відповіді)
```

### Типи повідомлень

**Клієнт → Сервер: оновлення прогресу**
```json
{
  "type": "PROGRESS_UPDATE",
  "readProgress": 45,
  "currentScore": 0
}
```

**Сервер → Клієнт (broadcast): стан сесії**
```json
{
  "type": "SESSION_STATE",
  "sessionId": "uuid",
  "status": "READING",
  "players": [
    { "userId": 1, "username": "john", "readProgress": 45, "score": 0, "phase": "READING" },
    { "userId": 2, "username": "jane", "readProgress": 72, "score": 0, "phase": "READING" }
  ]
}
```

**Сервер → Клієнт (broadcast): завершення**
```json
{
  "type": "DUEL_FINISHED",
  "winnerId": 2,
  "results": [
    { "userId": 1, "score": 320, "correctAnswers": 2, "readingTimeMs": 52000 },
    { "userId": 2, "score": 415, "correctAnswers": 3, "readingTimeMs": 48000 }
  ]
}
```

### Сценарій дуелі (flow)

```
Player1                    Backend                    Player2
  |                           |                           |
  |-- POST /api/duels ------> |                           |
  |<- { sessionId, code } --- |                           |
  |                           |                           |
  |-- WS connect -----------> |                           |
  |-- /app/duel/{id}/ready -> |                           |
  |                           | <-- POST /api/duels/{id}/join --
  |                           | <-- WS connect -----------|
  |                           | <-- /app/duel/{id}/ready -|
  |                           |                           |
  |<-- SESSION_STATE (READING)|-- SESSION_STATE (READING)->
  |                           |                           |
  |-- PROGRESS_UPDATE ------> | <-- PROGRESS_UPDATE ------|
  |<-- SESSION_STATE -------> |---> SESSION_STATE ------->|
  |  (обидва бачать прогрес)  |                           |
  |                           |                           |
  |-- ANSWER_SUBMITTED -----> | <-- ANSWER_SUBMITTED -----|
  |<-- ANSWER_RESULT -------> |---> ANSWER_RESULT ------->|
  |                           |                           |
  |<-- DUEL_FINISHED -------> |---> DUEL_FINISHED ------->|
```

---

## 6. Аутентифікація та авторизація

### Поточна проблема фронтенду
Наразі паролі зберігаються у відкритому вигляді в `localStorage`. Це критична вразливість, яку бекенд повністю вирішує.

### Запропоноване рішення: JWT (Access + Refresh Token)

```
Access Token:  15 хвилин  (передається в заголовку Authorization: Bearer <token>)
Refresh Token: 30 днів    (HttpOnly Cookie або окрема таблиця в БД)
```

### Зберігання токенів на фронтенді
- **Access Token** — `memory` (змінна React/Zustand), не `localStorage`
- **Refresh Token** — `HttpOnly Cookie` (захист від XSS)

### Spring Security конфігурація
- `JwtAuthenticationFilter` — перехоплює кожен запит, валідує Bearer-токен
- `UserDetailsService` — завантажує користувача з БД
- Ролі: `ROLE_USER`, `ROLE_ADMIN`
- CORS налаштований для фронтенд-домену

---

## 7. Функціонал, не реалізований на фронтенді

### 7.1 Вибір режиму: одиночний / груповий
**Де:** головна сторінка або сторінка вправи "Дуель"

**Backend потреби:**
- Наявний API дуелей (`POST /api/duels`, WebSocket) покриває груповий режим
- Одиночний режим — просто `POST /api/results` після завершення

**Пропозиція для фронтенду:**
```
[ Одиночний режим ]  [ Груповий режим ]
  ↓                    ↓
POST /api/results     POST /api/duels → отримати код → поділитися з другом
```

### 7.2 Вибір гравця для групового режиму
**Поточний стан:** гравці вводять довільні імена локально без зв'язку з акаунтами.

**Backend потреби:**
- `GET /api/users/search?q=john` — пошук реального акаунту
- Можливість запросити конкретного користувача або грати за кодом сесії

**Два варіанти приєднання:**
1. **За кодом** — Player1 створює сесію, отримує 6-символьний код, ділиться ним з Player2
2. **За запрошенням** — Player1 знаходить Player2 через пошук і надсилає invite (через WebSocket або polling)

### 7.3 Синхронізація екранів у груповому режимі
Детально описано в [розділі 5](#5-websocket--груповий-режим).

**Ключова відмінність від поточної реалізації:**
- Зараз: два гравці грають по черзі на одному пристрої
- Майбутнє: два гравці на різних пристроях одночасно, бачать прогрес один одного в реальному часі

### 7.4 Збереження результатів та статистики
**Поточний стан:** `localStorage` — втрачається при очищенні браузера, не синхронізується між пристроями.

**Backend покриває:**
- Постійне зберігання в PostgreSQL
- Доступність з будь-якого пристрою після логіну
- Агрегована статистика: кількість вправ, середній бал, максимальний бал, WPM
- Порівняння з іншими гравцями (лідерборд)

---

## 8. Додаткові можливості

### 8.1 Лідерборд
```
GET /api/leaderboard?exerciseType=rsvp&period=week&limit=10
```
Рейтинг гравців за балами або WPM (слова/хв) за тиждень/місяць/весь час.

### 8.2 Розрахунок WPM (Words Per Minute)
Бекенд може розраховувати реальну швидкість читання:
```
WPM = (кількість_слів_у_тексті / час_читання_в_хвилинах)
```
Це головна метрика для додатку швидкочитання. Відстежувати динаміку WPM у часі — ключова цінність для користувача.

### 8.3 Управління текстами (адмін-панель)
- CRUD для текстів і питань через API (`ROLE_ADMIN`)
- Можливість завантажувати нові тексти без деплою фронтенду
- Зараз тексти хардкодяться в `data/texts.ts`

### 8.4 Щоденні досягнення / стріки
```sql
CREATE TABLE user_streaks (
    user_id       BIGINT PRIMARY KEY REFERENCES users(id),
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_activity  DATE
);
```
Нотифікація: "Ти тренуєшся 7 днів поспіль! 🔥"

### 8.5 Профіль прогресу
`GET /api/users/me/progress` — агреговані дані для відображення на дашборді:
- Загальна кількість вправ
- Поточний WPM по типах вправ
- Динаміка WPM за останні 30 днів
- Порівняння з середнім по всіх користувачах

---

## 9. Технологічний стек

| Шар | Технологія | Обґрунтування |
|---|---|---|
| Framework | Spring Boot 4 | вже обрано в проекті |
| REST | Spring Web MVC | вже в `pom.xml` |
| Security | Spring Security + JWT (jjwt) | стандарт для stateless API |
| WebSocket | Spring WebSocket + STOMP | нативна підтримка, pub/sub |
| ORM | Spring Data JPA + Hibernate | вже в `pom.xml` |
| БД | PostgreSQL | вже обрано |
| Міграції | Flyway | вже в `pom.xml` |
| Lombok | Lombok | вже в `pom.xml` |
| Тести | JUnit 5, Testcontainers (PostgreSQL) | вже в `pom.xml` |
| Документація API | SpringDoc OpenAPI (Swagger UI) | зручно для інтеграції з фронтендом |

### Залежності для додавання в `pom.xml`
```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Swagger UI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.6</version>
</dependency>
```

---

## 10. Flyway-міграції

Файли в `src/main/resources/db/migration/`:

```
V1__create_users.sql
V2__create_texts_and_questions.sql
V3__seed_texts.sql          ← перенести дані з data/texts.ts
V4__create_exercise_results.sql
V5__create_duel_sessions.sql
V6__create_user_streaks.sql
```

---

## 11. Поетапний план реалізації

### Фаза 1 — Основа (MVP)
- [ ] Flyway-міграції: `users`, `texts`, `questions`, `exercise_results`
- [ ] Перенесення текстів з `data/texts.ts` в БД (V3 seed-міграція)
- [ ] `AuthController`: реєстрація, логін, JWT
- [ ] `TextController`: GET список, GET за id
- [ ] `ResultController`: POST результат, GET моя статистика
- [ ] Spring Security: захист ендпоінтів, CORS

### Фаза 2 — Статистика та профіль
- [ ] `GET /api/results/me/summary` — агрегована статистика
- [ ] `GET /api/users/me/progress` — дашборд прогресу
- [ ] Розрахунок WPM, збереження в `exercise_results.wpm`
- [ ] Адаптація фронтенду: замінити `localStorage` на API-запити

### Фаза 3 — Реальний мультиплеєр
- [ ] Flyway-міграція: `duel_sessions`, `duel_progress`
- [ ] `DuelController`: CRUD сесій, приєднання
- [ ] WebSocket конфігурація (STOMP + SockJS)
- [ ] `DuelWebSocketHandler`: обробка подій ready/progress/answer
- [ ] Адаптація фронтенду: `DuelExercise` → реальний мультиплеєр

### Фаза 4 — Додатковий функціонал
- [ ] Лідерборд
- [ ] Стріки / досягнення
- [ ] Адмін-панель (управління текстами)
- [ ] Swagger UI документація

---

*Дата документа: 2026-03-08*
*Версія: 1.0 — пропозиція, очікує на затвердження*

