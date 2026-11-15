# University Management System

A small Spring Boot university management web application for the CSE 3230 fourth lab task.

## Features
- Three roles: Admin, Teacher and Student.
- Session-based login and logout using Spring Security.
- BCrypt password hashing.
- Role-based access control for all REST endpoints.
- CRUD APIs returning JSON and standard HTTP status codes.
- Light-theme dashboard with add, edit, delete and search.
- PostgreSQL database and Docker Compose setup.

## Requirements
- Docker Desktop with Docker Compose, or Java 25+, Maven 3.9+ and PostgreSQL 16.

## Run with Docker (recommended)
Open a terminal in this project folder and run:

```bash
docker compose up --build
```

Open http://localhost:8080. Stop with `Ctrl+C`; run `docker compose down` to stop and remove containers. The database volume remains. To remove the database data too, run `docker compose down -v`.

## Demo login accounts
| Role | Email | Password |
|---|---|---|
| Admin | admin@university.com | admin123 |
| Teacher | teacher@university.com | teacher123 |
| Student | student@university.com | student123 |

These are demonstration credentials. Change them before using the application outside a lab/demo environment.

## Run without Docker
1. Create a PostgreSQL database named `university_db`.
2. Set database username/password, or use the defaults in `application.properties` (`university` / `university123`).
3. Run `mvn spring-boot:run`.
4. Open http://localhost:8080.

## REST API
All endpoints require login and a valid CSRF token for POST, PUT and DELETE requests.

| Resource | Endpoint | Allowed roles |
|---|---|---|
| Admins | `/api/admins` | ADMIN |
| Teachers | `/api/teachers` | ADMIN, TEACHER |
| Students | `/api/students` | ADMIN, STUDENT |

Each resource supports:
- `GET /api/{resource}` — list records (200 OK)
- `GET /api/{resource}/{id}` — get one record (200 OK; 404 if missing)
- `POST /api/{resource}` — create record (201 Created)
- `PUT /api/{resource}/{id}` — update record (200 OK)
- `DELETE /api/{resource}/{id}` — delete record (204 No Content)

Example JSON for creating a teacher or student:

```json
{
  "name": "Ayesha Rahman",
  "email": "ayesha@university.com",
  "password": "pass123",
  "department": "Computer Science"
}
```

The role is determined by the endpoint, not by user-supplied JSON. Passwords are never included in API responses.

## Simple explanation for the lab teacher
1. `UserAccount` is the database entity. Its `role` field identifies whether a record is an ADMIN, TEACHER or STUDENT.
2. `UserRepository` communicates with PostgreSQL through Spring Data JPA.
3. `UserService` contains the CRUD logic and hashes new or changed passwords with BCrypt.
4. `UserApiController` exposes REST endpoints and returns JSON.
5. `SecurityConfig` loads users from the database and restricts endpoint access by role.
6. `PageController` displays the login page and dashboard. The dashboard calls the REST APIs using JavaScript.
7. Docker Compose starts the Spring Boot application and PostgreSQL database together.

## Important access-control detail
The application checks permissions on the server. Hiding a menu item in the browser is only a user-interface convenience; it is not the security mechanism. A teacher who manually requests `/api/students` receives HTTP 403, and a student requesting `/api/teachers` receives HTTP 403.
