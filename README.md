# fdm-pack-loader

Сервис **FDM Package Loader** (BeeAtlas): приём и обработка «пакетов» данных через REST и RabbitMQ, хранение состояния в PostgreSQL, вызовы внешних HTTP-сервисов (capabilities, products, techradar, auth SSO).

- **Стек:** Java 17, Spring Boot 2.7, Spring Data JPA, Flyway, PostgreSQL, RabbitMQ, Springfox (Swagger 2), OpenTelemetry (опционально), Actuator (Prometheus).
- **Артефакт:** `fdm-pack-loader-<version>.jar` (версия задаётся в `pom.xml`).

## Требования

- JDK 17, Maven 3.8+
- PostgreSQL (схема миграций: `pack_loader`)
- RabbitMQ
- Доступные URL интеграций (см. ниже) — строки подставляются при старте; для чисто локального подъёма без остальных сервисов в `docker-compose` заданы заглушки по умолчанию.

## Сборка и запуск локально

```bash
mvn clean package -DskipTests
java -jar target/fdm-pack-loader-*.jar
```

Перед запуском задайте как минимум подключение к БД, RabbitMQ и интеграции (через переменные окружения или профиль), иначе Spring не поднимет контекст.

Порт HTTP по умолчанию: **8080** (если не переопределён `server.port`).

## Docker Compose

Поднимает PostgreSQL, RabbitMQ и приложение в одной сети.

```bash
docker compose up --build
```

Полезные переменные окружения (можно положить в `.env` рядом с `docker-compose.yml`):

| Переменная | По умолчанию | Назначение |
|------------|----------------|------------|
| `PACK_LOADER_APP_PORT` | `8080` | порт приложения на хосте |
| `PACK_LOADER_POSTGRES_PORT` | `5433` | порт PostgreSQL на хосте |
| `PACK_LOADER_POSTGRES_DB` | `fdm_pack_loader` | имя БД |
| `PACK_LOADER_POSTGRES_USER` / `PACK_LOADER_POSTGRES_PASSWORD` | `postgres` / `postgres` | учётные данные БД |
| `PACK_LOADER_RABBITMQ_AMQP_PORT` | `5672` | AMQP |
| `PACK_LOADER_RABBITMQ_UI_PORT` | `15672` | UI управления RabbitMQ |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | `guest` / `guest` | учётная запись брокера |
| `RABBITMQ_EXCHANGE` / `RABBITMQ_ROUTING_KEY` | `pack.loader.exchange` / `pack.loader.routing` | exchange и routing key для шаблона `RabbitTemplate` (очередь tech capability привязывается в `RabbitConfig`) |
| `INTEGRATION_*_SERVER_URL` | см. `docker-compose.yml` | базовые URL внешних сервисов |

Проверка здоровья приложения: `GET http://localhost:<PACK_LOADER_APP_PORT>/actuator/health`.

## Конфигурация

### База данных и Flyway

- Схема: `pack_loader` (`spring.jpa.properties.hibernate.default_schema`, `spring.flyway.default-schema`).
- Миграции: `src/main/resources/db/migration/`.
- Пример URL: `jdbc:postgresql://localhost:5432/fdm_pack_loader`.

### RabbitMQ

Свойства Spring: `spring.rabbitmq.*` (хост, порт, пользователь, пароль, virtual host, `spring.rabbitmq.template.exchange`, `spring.rabbitmq.template.routing-key`).

Очереди, на которые подписаны слушатели (имена из `application.properties`, при необходимости переопределяются):

| Свойство | Значение по умолчанию |
|----------|------------------------|
| `queue.tech-capability.name` | `tech_capability_queue` |
| `queue.business-capability.name` | `business_capability_queue` |
| `queue.package.name` | `package_queue` |
| `queue.product.name` | `product_queue` |
| `queue.tech-product-relation.name` | `TECH_PRODUCT_RELATION` |

В коде явно объявляется (через `@Bean`) очередь для tech capability и привязка к direct exchange; остальные очереди в типичном окружении должны существовать на брокере или быть объявлены вашей инфраструктурой.

### Интеграции (HTTP)

Обязательные для старта приложения свойства (Spring relaxed binding → переменные окружения `INTEGRATION_*`):

| Свойство | Описание |
|----------|----------|
| `integration.capability-server-url` | сервис capabilities |
| `integration.products-server-url` | сервис продуктов |
| `integration.authsso-server-url` | Auth SSO |
| `integration.techradar-server-url` | TechRadar |

### Прочее

- `app.ambassador-auth` — режим аутентификации к RabbitMQ через токен (`AuthSSOClient`); в `application.properties` по умолчанию `false`.
- Actuator: `health`, `info`, `metrics`, `prometheus` (`management.endpoints.web.exposure.include`).
- OpenTelemetry: в `application.properties` заданы endpoint и `otel.sdk.disabled=true`; в compose для контейнера дополнительно выставлен `OTEL_SDK_DISABLED=true`.

## HTTP API

- `GET /` — приветствие с именем и версией приложения.
- **v1** (`/api/v1`): `POST /package`, `GET /packages-list`, `GET /package/{id}` (детали пакета с проверкой доступа администратора).
- **v2** (`/api/v2`): `GET /packages-list` — список пакетов в формате v2.

Swagger UI (Springfox 3): обычно **`/swagger-ui/index.html`** (при необходимости проверьте редирект с `/swagger-ui/`).

## Структура проекта (кратко)

- `ru.beeline.fdmpackloader.controller` — REST и перехватчик заголовков.
- `ru.beeline.fdmpackloader.сonsumer` — `@RabbitListener` для очередей capability / package / product / tech-product-relation.
- `ru.beeline.fdmpackloader.service` — бизнес-логика пакетов.
- `ru.beeline.fdmpackloader.client` — HTTP-клиенты к внешним сервисам.
- `ru.beeline.fdmpackloader.config` — RabbitMQ, Swagger, MVC, трассировка.

## Сборка образа без Compose

```bash
docker build -t fdm-pack-loader:local .
```

Финальный образ основан на `eclipse-temurin:17-jre-jammy`, точка входа: `java -jar app.jar`, порты **8080** (и при необходимости **8090**, **10260** по `EXPOSE` в Dockerfile).

## Лицензия и владелец

Код помечен копирайтом PJSC VimpelCom (см. заголовки в исходниках). Условия распространения уточняйте у владельца репозитория.
