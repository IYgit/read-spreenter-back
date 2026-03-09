# Read Sprinter — План реалізації бекенду

> Дата: 2026-03-09 | Версія: 1.1 — **ЗАТВЕРДЖЕНО**
>
> Рішення узгоджені:
> - Порядок: Частина 1 (одиночний) → Частина 2 (груповий) ✅
> - Адаптація фронтенду в рамках завдання ✅
> - Swagger UI — додаємо ✅
> - Refresh Token — зберігати в localStorage ✅
> - .gitignore — застосовуємо запропонований варіант ✅

---

## Зміст
1. [Питання щодо запропонованого порядку](#1-питання-щодо-запропонованого-порядку)
2. [Рекомендований порядок реалізації](#2-рекомендований-порядок-реалізації)
3. [Детальний план по фазах](#3-детальний-план-по-фазах)
4. [Питання щодо .gitignore](#4-питання-щодо-gitignore)
5. [Рекомендований .gitignore](#5-рекомендований-gitignore)

---

## 1. Питання щодо запропонованого порядку

Ваш поділ на **"одиночний режим → груповий режим"** — логічний і правильний.
Він збігається з тим, що я описав у `backend-proposal.md` (Фаза 1–2 → Фаза 3–4).

**Єдине уточнення:** всередині "Частини 1" я пропоную дещо уточнити пріоритети,
щоб на кожному кроці мати щось робоче, а не "все або нічого".

---

## 2. Рекомендований порядок реалізації

### Частина 1 — Одиночний режим (фронтенд + бекенд)

| Крок | Що робимо | Результат |
|------|-----------|-----------|
| **1.1** | `.gitignore` + структура проекту | Чистий репозиторій |
| **1.2** | Flyway-міграції: `users`, `texts`, `questions`, `exercise_results` | БД готова |
| **1.3** | Seed-міграція: перенос текстів з `data/texts.ts` в БД | Тексти в PostgreSQL |
| **1.4** | `AuthController` (register/login) + JWT + Spring Security | Авторизація працює |
| **1.5** | `TextController` (GET /api/texts, GET /api/texts/{id}) | Фронтенд читає тексти з БД |
| **1.6** | Адаптація фронтенду: авторизація через API замість localStorage | Авторизація реальна |
| **1.7** | Адаптація фронтенду: завантаження текстів через API | Тексти з БД |
| **1.8** | `ResultController` (POST /api/results, GET /api/results/me) | Збереження результатів |
| **1.9** | Адаптація фронтенду: збереження статистики через API | Статистика в БД |
| **1.10** | `GET /api/results/me/summary` + дашборд прогресу | Повноцінна статистика |

### Частина 2 — Груповий режим (дуель)

| Крок | Що робимо | Результат |
|------|-----------|-----------|
| **2.1** | Flyway-міграції: `duel_sessions`, `duel_progress` | БД для дуелей |
| **2.2** | `DuelController` (CRUD сесій) + WebSocket (STOMP) | API дуелей |
| **2.3** | WebSocket-обробники: ready/progress/answer | Real-time синхронізація |
| **2.4** | Адаптація фронтенду: `DuelExercise` → реальний мультиплеєр | Дуель між різними пристроями |
| **2.5** | Лідерборд, стріки (опціонально) | Додатковий функціонал |

---

## 3. Детальний план по фазах

### Фаза 1 — Основа (Кроки 1.1–1.3)

**Завдання:**
- Налаштувати `.gitignore`
- Flyway-міграції V1–V3 (схема + seed-дані)

**Файли:**
```
.gitignore
src/main/resources/db/migration/
  V1__create_users.sql
  V2__create_texts_and_questions.sql
  V3__seed_texts.sql
  V4__create_exercise_results.sql
```

---

### Фаза 2 — Авторизація (Кроки 1.4–1.6)

**Завдання:**
- `JwtUtil`, `JwtAuthenticationFilter`
- `AuthController`: POST /api/auth/register, POST /api/auth/login
- Spring Security config: public endpoints + protected
- CORS для фронтенду

**Додати в pom.xml:**
```xml
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
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

**Адаптація фронтенду:**
- `lib/auth.ts` → замінити localStorage-авторизацію на POST /api/auth/login та /register
- Зберігати JWT в пам'яті (змінна), не в localStorage

---

### Фаза 3 — Тексти та результати (Кроки 1.5–1.10)

**Завдання:**
- `TextController`: GET /api/texts, GET /api/texts/{id}
- `ResultController`: POST /api/results, GET /api/results/me, GET /api/results/me/summary
- Swagger UI (опціонально, але зручно для тестування)

**Адаптація фронтенду:**
- `data/texts.ts` → замінити на API-виклик GET /api/texts
- `lib/exerciseStats.ts` → замінити localStorage на POST /api/results

---

### Фаза 4 — Груповий режим (Кроки 2.1–2.4)

**Завдання:**
- Flyway-міграції для дуелей
- `DuelController` + WebSocket (STOMP over SockJS)
- Адаптація `DuelExercise.tsx`

**Додати в pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

---

## 4. Питання щодо .gitignore

### Чи потрібні в git файли `mvnw`, `mvnw.cmd`, `maven-wrapper.properties`, `.gitattributes`?

**Так, ці файли потрібні в git і їх НЕ треба ігнорувати.** Ось чому:

| Файл | Чи потрібен? | Пояснення |
|------|-------------|-----------|
| `mvnw` | ✅ Так | Maven Wrapper — дозволяє будувати проект без локально встановленого Maven (`./mvnw clean install`). Критично для CI/CD та для колег, у яких нема Maven. |
| `mvnw.cmd` | ✅ Так | Те саме для Windows (`.cmd` — Windows-версія mvnw). |
| `.mvn/wrapper/maven-wrapper.properties` | ✅ Так | Конфігурація Maven Wrapper: яку версію Maven завантажити. Без цього файлу `mvnw` не знає, що завантажувати. |
| `.gitattributes` | ✅ Так | Налаштування git: наприклад, `mvnw` позначається як `text eol=lf`, щоб не ламати перенос рядків між Windows та Linux. Якщо ігнорувати — `mvnw` може не запуститись на Linux/Mac після клонування з Windows. |

**Висновок:** Spring Initializr генерує ці файли саме для того, щоб вони були в git. Не видаляйте їх.

---

## 5. Рекомендований .gitignore

### Поточна проблема
Наразі в git потрапляють усі файли, включно з:
- `target/` (скомпільовані класи, JAR)
- `node_modules/` (npm-залежності фронтенду)
- `.idea/` (налаштування IDE)
- `*.class`, `*.jar`
- Можливо, файли з секретами (`application-local.properties`)

### Пропонований `.gitignore`

```gitignore
# ============================================================
# Maven — бекенд
# ============================================================
target/
!.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

# ============================================================
# IDE — IntelliJ IDEA
# ============================================================
.idea/
*.iws
*.iml
*.ipr
out/
!**/src/main/**/out/
!**/src/test/**/out/

# ============================================================
# IDE — Eclipse
# ============================================================
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

# ============================================================
# IDE — VS Code
# ============================================================
.vscode/

# ============================================================
# Node.js — фронтенд (speed-reader-front)
# ============================================================
speed-reader-front/node_modules/
speed-reader-front/dist/
speed-reader-front/.next/
speed-reader-front/coverage/
speed-reader-front/.env.local
speed-reader-front/.env.development.local
speed-reader-front/.env.test.local
speed-reader-front/.env.production.local

# ============================================================
# Секрети та локальні налаштування
# ============================================================
application-local.properties
application-local.yml
application-secret.properties
*.env
.env

# ============================================================
# OS-специфічні файли
# ============================================================
.DS_Store
Thumbs.db
*.swp
*.swo

# ============================================================
# Логи
# ============================================================
*.log
logs/
spring.log

# ============================================================
# Тимчасові файли
# ============================================================
*.tmp
*.bak
*.orig
```

### Що залишається в git (навмисно НЕ ігнорується)
- `mvnw`, `mvnw.cmd` — Maven Wrapper
- `.mvn/wrapper/maven-wrapper.properties` — конфіг Maven Wrapper
- `.gitattributes` — налаштування git для коректних перенесень рядків
- `src/` — весь вихідний код
- `pom.xml` — залежності Maven
- `speed-reader-front/src/` — вихідний код фронтенду
- `speed-reader-front/package.json`, `bun.lockb` — залежності фронтенду

---

## Питання для узгодження

Перед тим як починати реалізацію, хотів би уточнити:

1. **Порядок:** Ви погоджуєтесь з запропонованим порядком (Частина 1 → Частина 2)?
2. **Фронтенд:** Адаптацію фронтенду (кроки 1.6, 1.7, 1.9) також робимо в рамках цього завдання, чи тільки бекенд?
3. **Swagger UI:** Додавати чи ні?
4. **Refresh Token:** Зберігати в HttpOnly Cookie (безпечніше) чи спростити і зберігати в localStorage (простіше для фронтенду)?
5. **`.gitignore`:** Застосовуємо запропонований варіант?
```

