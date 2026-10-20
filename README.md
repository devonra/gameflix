## How to Run

### MySQL

```
CREATE DATABASE gameflix;
```
```
CREATE USER 'gameflix'@'localhost' IDENTIFIED BY 'gameflix-password';
```
```
GRANT ALL PRIVILEGES ON gameflix.* TO 'gameflix'@'localhost';
```

These commands create the database known as gameflix. Upon setup you must choose your own password for the database (placeholder "gameflix-password" in the above text).

Hibernate automatically creates the `users` table on the first run,  but this is the output of `SHOW CREATE TABLE users;`:
```
CREATE TABLE `users` (
  	`id` bigint NOT NULL AUTO_INCREMENT,
  	`created_at` datetime(6) NOT NULL,
  	`email` varchar(255) NOT NULL,
  	`password` varchar(255) NOT NULL,
  	`role` varchar(20) NOT NULL,
  	`username` varchar(50) NOT NULL,
  	PRIMARY KEY (`id`),
  	UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
```

 

### .properties

application-local.properties is included in .gitignore, meaning it will have to be set up manually by the administrator.
application.properties.example can be copy-pasted into a new application-local.properties file, at which time the username and password will need to be updated to match what was used in the MySQL setup, as well as an openssl rand -base64 48 key.

### Running

The main app is started by running GameFlixUserServiceApplication.java to launch the Spring Boot application.
Testing is done by running AuthIntegrationTest.java which executes 8 automated tests to demonstrate capabilities.

## Persistence

1. persistence through application startup/shutdown is maintained on a locally hosted MySQL database using Spring to modify the data through RESTful requests
2. password column is VARCHAR(255) NOT NULL to factor for hash length
3. email column has uk_users_email to enforce unique emails
4. db credentials are only stored in the Git uncommitted application-local.properties file (although you can also use environmental variables)
5. spring.jpa.hibernate.ddl-auto=update : create-drop would wipe data between tests so update was used for easier testing
6. schema is included in above MySQL section

## Security

1. bcrypt was used at a strength of 12. this will raise the security level by making every guess need 4x as much computing work compared to the default of 10 without adding *too* much strain on hardware resources. bcrypt was chosen since it ships with Spring
2. the signed JWT has a 30 minute expiration time, giving users a reasonable enough time to spend on the website before being logged out, without creating an unreasonably long session timer that is prone to token hijacking (since stateless JWT cannot be revoked). the validity of this expiration was manually tested (with a temporarily decreased expiration timer) (showcase included in word document)
3. the secret in application.properties uses ${JWT_SECRET} with the true key being stored in a local, uncommitted file
4. generic 401 error for failed login (showcase included in word document)
5. CorsConfigurationSource only allows ${gameflix.cors.allowed-origin} as specified in application.properties
6. token contains the issuer of the token, user ID, token creation time, token expiration time, and user role (showcase included in word document). nothing confidential

## Testing

- automated testing was done, using /userservice/AuthIntegrationTest.java
- checks: successful registration, duplicate emails, successful login returning a token, login with wrong password, protected call with valid token, protected call with missing token, protected call with tampered token, invalid input
- manually tested token expiration (on a reduced timer) and confirmed tokens successfully expire to block access
- manually checked MySQL database/table to ensure Spring POSTs were actually writing to the DB persistently
- word document shows the SELECT id, email, password FROM users; showcase of {bcrypt}$2a$12$ function

## Design-Change

The only noteworthy design changes from Module 3 are the solidification of RESTful routes and the solidification of architecture. /me replaced /users/{user_id}, and /register is new. These set up the overall design for future work more accurately to the vision for GameFlix. RESTful calls now go through /api/ paths (like POST /api/auth/login instead of POST /login). The "admin" boolean is now a "role" string (allowing for things like critics/journalists having special access to games early, division between financial administration and game database moderation, etc) and the subscription_tier column is missing as it was out of scope for this assignment.


## AI

AI was used quite heavily for the assignment. Claude Sonnet 5.5 was used to generate the initial project structure, with the input being my previous course deliverables, and the description for this assignment. Upon creation of the project, it was loaded into IntelliJ on my own computer, at which point I manually reviewed the generated code. 

Most of this code was the type of boiler-plate code I would be keen to reach to AI for, borrowing heavily from libraries. Some of the output was a bit extra for the assignment, such as a "dummy-hash timing defense" in AuthService.java, which was unneeded for the scope of the assignment, and since I didn't fully understand it, I removed it from the submission. (I believe what it was doing was spending the time required to do bcrypt hashing even if unnecessary in order to fib the response time to deter attackers.)

One mistake the code made was with bcrypt truncation, where the size validation looked at characters instead of bytes, meaning it was possible for part of the password to be silently truncated. Therefore, I added a byte size check in order to reject passwords that would be too long after encoding.




