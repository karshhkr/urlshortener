# URL Shortener

A production-ready URL Shortener built with Spring Boot, MySQL, and JWT Authentication.

## Live Demo
🔗 [Coming Soon - Railway Deployment]

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2
- **Database:** MySQL 8 + Spring Data JPA
- **Security:** Spring Security + JWT (JJWT)
- **Cache:** Caffeine Cache
- **Migrations:** Flyway
- **Connection Pool:** HikariCP
- **Build:** Maven

## Features

- Shorten long URLs instantly
- Custom alias support
- URL expiry (configurable days)
- Click count tracking
- JWT-based Register & Login
- Caffeine in-memory caching for fast redirects
- Flyway DB migrations
- Multi-profile config (dev/prod)
- Global exception handling
- Docker ready

## API Endpoints

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/register | Register new user |
| POST | /api/auth/login | Login and get JWT token |

### URL Shortener
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/shorten | Shorten a URL |
| GET | /{shortCode} | Redirect to original URL |
| GET | /api/stats/{shortCode} | Get URL statistics |
| DELETE | /api/urls/{shortCode} | Deactivate a URL |

### Health
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /actuator/health | App health check |

## How to Run Locally

### Prerequisites
- Java 17+
- MySQL 8+
- Maven 3.8+

### Setup

**1. Clone the repo**
```bash
git clone https://github.com/YOUR_USERNAME/urlshortener.git
cd urlshortener
```

**2. Create MySQL database**
```sql
CREATE DATABASE url_shortener;
```

**3. Configure application-dev.properties**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/url_shortener?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=yourpassword
```

**4. Run the app**
```bash
mvn clean spring-boot:run
```

**5. Open browser**
