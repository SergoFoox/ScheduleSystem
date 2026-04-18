# GEMINI.md - ASMS V3 Instructional Context

## Project Overview
**ASMS V3 (Academic Schedule Management System)** is a modern web application designed for automated creation and management of academic schedules in universities. It leverages intelligent optimization to reduce manual workload and minimize scheduling conflicts.

- **Primary Goal:** Automated schedule generation and efficient substitution management.
- **Core Technologies:** 
    - **Backend:** Java 21, Spring Boot 4.x (Parent)
    - **Frontend:** Vaadin 25.1.2 (using Aura theme and React integration)
    - **Optimization Engine:** Timefold Solver 1.33.0
    - **Database:** PostgreSQL (recommended)
- **Status:** Project Initialization. Core structure is set up, but domain models and business logic are in the design phase.

## Project Structure
- `src/main/java/com/sergofoox/Application.java`: Main entry point and Spring Boot configuration.
- `src/main/resources/application.properties`: Application configuration (server port, Vaadin settings).
- `memory-bank/`: Contains comprehensive project documentation:
    - `productContext.md`: Vision, target users, and core goals.
    - `techContext.md`: Technical stack and architecture details.
    - `progress.md`: Current development status and next steps.
    - `systemPatterns.md`: Design patterns and architectural decisions.
- `pom.xml`: Maven configuration with dependencies for Vaadin and Timefold.
- `package.json`: Frontend dependencies, including React and Vaadin React components.

## Building and Running

### Development Mode
To start the application in development mode:
```bash
./mvnw
```
The application will be available at `http://localhost:8080` (or the port defined in `application.properties`).

### Production Build
To build the application for production:
```bash
./mvnw package -Pproduction
```

### Docker
To build a Docker image:
```bash
docker build -t schedule-system:latest .
```

## Development Conventions
- **Memory Bank:** Always update the `memory-bank/` files when making significant architectural or progress changes.
- **Java Version:** Use Java 21 features where appropriate.
- **Vaadin 25:** Follow modern Vaadin best practices, utilizing the Aura theme and React-based components as configured in `package.json`.
- **Timefold:** Use Timefold for all scheduling and optimization logic. Focus on defining clear constraints and efficient domain models.

## Future Milestones (from `memory-bank/progress.md`)
1. Define Domain Model (Teacher, Subject, Lesson).
2. Create Spring Boot entities and JPA repositories.
3. Implement Timefold solver logic and initial constraints.
4. Develop Vaadin dashboards for schedule visualization.
