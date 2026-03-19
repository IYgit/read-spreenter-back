# RSVP Дуель — Аналіз проблеми "Waiting..."

## Спостереження

Обидва гравці бачать екран "Шукаємо суперника..." і ніколи не переходять до відліку.

---

## Чому обидва бачать "Waiting..." навіть при помилці

Ключовий факт: `setPhase('waiting')` викликається **синхронно та негайно** — ще **до** того,
як `joinQueue` HTTP-запит завершиться (він є асинхронним):

```typescript
// DuelExercise.tsx
const handleStartSearch = useCallback((req: JoinQueueRequest) => {
  connect(req);       // ← WebSocket + joinQueue запускаються асинхронно
  setPhase('waiting'); // ← але цей стан встановлюється ВЖЕ ЗАРАЗ
}, [connect]);
```

І якщо `joinQueue` завершується з помилкою 500 — є лише логування, **без переходу назад**:

```typescript
// useDuelWebSocket.ts
duelApi.joinQueue(req).catch((err) => {
  console.error('Failed to join queue:', err);
  onQueueError?.(err); // ← onQueueError НЕ передається у DuelExercise.tsx!
});
```

**Висновок: користувач назавжди залишається у "Waiting...", навіть якщо backend повернув 500.**

---

## Причина 1 (Основна): Виняток у `MatchmakingDbService` для RSVP

### `textRepository.findRandom()` — ризик несумісності JPQL

У `TextRepository.java` новий метод:
```java
@Query("SELECT t FROM Text t ORDER BY RAND()")
List<Text> findRandom(Pageable pageable);
```

`ORDER BY RAND()` у JPQL є **нестандартним розширенням Hibernate (HQL)** і може поводитися
по-різному залежно від версії Hibernate 6 та діалекту бази даних:

- PostgreSQL: `RAND()` → `RANDOM()` (Hibernate має транслювати, але не завжди)
- Якщо трансляція не спрацює → `QueryException` або `SQLSyntaxErrorException`

Цей виняток кидається **всередині `@Transactional` методу `tryCreateMatch()`**, що призводить
до rollback транзакції та HTTP 500 у відповіді. Frontend ігнорує помилку → обидва чекають.

---

## Причина 2 (Додаткова): TypeScript помилки у не-RSVP лоббі

В `api.ts`, нові поля визначені як **обов'язкові**:
```typescript
export interface JoinQueueRequest {
  ...
  rsvpSyntagmWidth: number;  // required
  rsvpDisplayTime: number;   // required
}
```

Але `DuelLobby`, `DuelNumbersLobby`, `DuelWordPairsLobby` їх **не передають**.
Vite у dev-режимі не блокує збірку через TypeScript-помилки, але це потенційний ризик.

---

## Схема проблеми

```
User1: [Find opponent] → setPhase('waiting') → joinQueue(rsvp) → OK → saved to queue
User2: [Find opponent] → setPhase('waiting') → joinQueue(rsvp) → findRandom() THROWS 500
                                                                       ↓
                                              Frontend: console.error (тихо!)
                                              Фаза залишається: 'waiting' ← forever
```

---

## Рішення

### 1. Backend: замінити `ORDER BY RAND()` на надійніший підхід

Замість нестандартного JPQL, вибираємо випадковий текст в Java:

**`TextRepository.java`** — замінити `findRandom()`:
```java
// Замість ORDER BY RAND() — просто отримати всі ID
@Query("SELECT t.id FROM Text t")
List<Long> findAllIds();
```

**`MatchmakingDbService.java`** — в RSVP блоці:
```java
// Замість: textRepository.findRandom(PageRequest.of(0, 1))
List<Long> ids = textRepository.findAllIds();
if (ids.isEmpty()) throw new RuntimeException("No texts available for RSVP duel");
Long randomId = ids.get(RANDOM.nextInt(ids.size()));
Text text = textRepository.findById(randomId).orElseThrow();
```

Переваги: не залежить від SQL-діалекту, Hibernate версії, `RAND()` vs `RANDOM()`.

---

### 2. Frontend: обробка помилки `joinQueue`

**`DuelExercise.tsx`** — додати `onQueueError` обробник:
```typescript
const handleQueueError = useCallback(() => {
  disconnect();
  setPhase('lobby');  // повертаємось до лоббі
}, [disconnect]);

// Передати до useDuelWebSocket:
const { connect, disconnect, ... } = useDuelWebSocket({
  onMatchFound: handleMatchFound,
  onDuelEvent: handleDuelEvent,
  onQueueError: handleQueueError,  // ← ДОДАТИ
  sessionId: matchInfo?.sessionId ?? null,
});
```

---

### 3. Frontend: зробити RSVP поля опціональними в `JoinQueueRequest`

**`api.ts`** — щоб не-RSVP лоббі не мали TypeScript-помилок:
```typescript
export interface JoinQueueRequest {
  ...
  rsvpSyntagmWidth?: number;  // optional
  rsvpDisplayTime?: number;   // optional
}
```

---

## Зведена таблиця змін

| Файл | Зміна |
|---|---|
| `TextRepository.java` | Замінити `findRandom(Pageable)` на `findAllIds()` |
| `MatchmakingDbService.java` | Вибір випадкового тексту через Java-рандом |
| `DuelExercise.tsx` | Додати `onQueueError` → повернення до лоббі |
| `api.ts` | `rsvpSyntagmWidth?`, `rsvpDisplayTime?` — зробити опціональними |

