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
## Sample API Usage

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"pass123"}'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"pass123"}'
```

### Shorten URL
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.google.com"}'
```

### Project Structure

src/main/java/com/urlshortener/

├── config/          # Security, JWT, Cache config
├── controller/      # REST controllers
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities
├── exception/       # Global exception handler
├── repository/      # JPA repositories
└── service/         # Business logic

## Author
**Utkarsh Kumar Dabgarwal**
- GitHub: [username](https://github.com/karshhkr)
- LinkedIn: [linkedin](www.linkedin.com/in/utkarshkumardabgarwal-9001571b1)

 
 ## Live Demo
 
🔗 [URL Shortener Live](https://urlshortener-production-62d6.up.railway.app/index.html)
