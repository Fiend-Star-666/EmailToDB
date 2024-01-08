# Email to DB

Unleash the power of automation and data management with Email to DB, an advanced Java application leveraging the
robustness of Spring Boot and Maven. Designed to seamlessly fetch emails from a Gmail account and store them
meticulously in a PostgreSQL database, Email to DB is more than just a tool; it's your personal email archivist working
tirelessly behind the scenes.

## Electrifying Features

- **Relentless Email Fetching**: Harness the Gmail API to systematically retrieve new emails, ensuring you never miss a
  beat.
- **Comprehensive Storage Solution**: Not just emails, but their attachments are securely stored in PostgreSQL, offering
  a complete data preservation experience.
- **Duplicate Deterrence**: Employs a sophisticated system to identify and avoid redundancy, using unique email IDs and
  SHA-256 hashes.

## Embarking on the Journey

Get set to launch this powerhouse on your local machine for an unmatched development and testing experience.

### Essential Prerequisites

- Java 17 or higher for unmatched efficiency and performance.
- Maven, your trusty companion for impeccable dependency management.
- PostgreSQL, the bedrock of your data storage needs.
- Docker, for a seamless, containerized environment.

## Crafted With Passion

- **[Java](https://www.java.com/)** - The cornerstone programming language offering unmatched reliability.
- **[Spring Boot](https://spring.io/projects/spring-boot)** - The dynamic web framework powering our application's
  backbone.
- **[Maven](https://maven.apache.org/)** - The essential tool for streamlined dependency management.
- **[PostgreSQL](https://www.postgresql.org/)** - The robust database ensuring your data's integrity and security.
- **[Gmail API](https://developers.google.com/gmail/api)** - The key to unlocking and fetching your valuable emails.

Dive into the world of Email to DB, where efficiency meets data management, and take the first step towards
revolutionizing your email archival process!

## Getting Started

The application will start and begin fetching emails from the configured Gmail account.

### Running with Docker Compose

If you have Docker installed, you can use Docker Compose to run the application along with its PostgreSQL database in
separate Docker containers:

1. Build the Docker images:
   docker-compose build
2. Start the Docker containers:
   docker-compose up
3. The application and database will start in their own Docker containers. The application will begin fetching emails
   from the configured Gmail account.

