## Micronaut 4.10.7 Documentation

- [User Guide](https://docs.micronaut.io/4.10.7/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.10.7/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.10.7/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)

---

- [Micronaut Maven Plugin documentation](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/)

## Feature email-javamail documentation

- [Micronaut Javamail Email documentation](https://micronaut-projects.github.io/micronaut-email/latest/guide/index.html#javamail)

- [https://jakartaee.github.io/mail-api/](https://jakartaee.github.io/mail-api/)

## Feature test-resources documentation

- [Micronaut Test Resources documentation](https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/)

## Feature openapi documentation

- [Micronaut OpenAPI Support documentation](https://micronaut-projects.github.io/micronaut-openapi/latest/guide/index.html)

- [https://www.openapis.org](https://www.openapis.org)

## Feature validation documentation

- [Micronaut Validation documentation](https://micronaut-projects.github.io/micronaut-validation/latest/guide/)

## Feature mockito documentation

- [https://site.mockito.org](https://site.mockito.org)

## Feature serialization-jackson documentation

- [Micronaut Serialization Jackson Core documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)

## Feature management documentation

- [Micronaut Management documentation](https://docs.micronaut.io/latest/guide/index.html#management)

## Feature security-jwt documentation

- [Micronaut Security JWT documentation](https://micronaut-projects.github.io/micronaut-security/latest/guide/index.html)

## Feature liquibase documentation

- [Micronaut Liquibase Database Migration documentation](https://micronaut-projects.github.io/micronaut-liquibase/latest/guide/index.html)

- [https://www.liquibase.org/](https://www.liquibase.org/)

## Feature jul-to-slf4j documentation

- [https://www.slf4j.org/legacy.html#jul-to-slf4jBridge](https://www.slf4j.org/legacy.html#jul-to-slf4jBridge)

## Feature guice documentation

- [Micronaut Guice documentation](https://micronaut-projects.github.io/micronaut-guice/latest/guide/index.html)

## Feature maven-enforcer-plugin documentation

- [https://maven.apache.org/enforcer/maven-enforcer-plugin/](https://maven.apache.org/enforcer/maven-enforcer-plugin/)

## Feature micronaut-aot documentation

- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)

## Feature data-jdbc documentation

- [Micronaut Data JDBC documentation](https://micronaut-projects.github.io/micronaut-data/latest/guide/index.html#jdbc)

## Feature reactor documentation

- [Micronaut Reactor documentation](https://micronaut-projects.github.io/micronaut-reactor/snapshot/guide/index.html)

## Feature jdbc-hikari documentation

- [Micronaut Hikari JDBC Connection Pool documentation](https://micronaut-projects.github.io/micronaut-sql/latest/guide/index.html#jdbc)

## Feature jms-core documentation

- [Micronaut JMS documentation](https://micronaut-projects.github.io/micronaut-jms/snapshot/guide/index.html)

## Feature http-client documentation

- [Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#nettyHttpClient)


| 场景                     | API                                                 | 请求/返回说明                                                         |
| ---------------------- | --------------------------------------------------- | --------------------------------------------------------------- |
| **1️⃣ 注册（邮箱+验证码）**     | `POST /auth/email/code` → `POST /auth/email/verify` | 用户输入邮箱，后台发送验证码；验证验证码后返回 `accessToken + UserDTO`（用户状态可标记是否已设置密码） |
| **2️⃣ 邮箱+验证码登录（快速登录）** | `POST /auth/email/code` → `POST /auth/email/verify` | 用户之前已注册，但忘记密码或使用快捷登录，输入邮箱拿验证码即可登录，返回 `accessToken + UserDTO`    |
| **3️⃣ 邮箱+密码登录**        | `POST /auth/login/password`                         | 用户输入邮箱+密码登录，返回 `accessToken + UserDTO`                          |
| **4️⃣ 邮箱+验证码之后设置密码**   | `POST /auth/password`                               | 用户第一次注册后，或通过验证码登录后设置密码，返回更新后的 `UserDTO`                         |
| **5️⃣ 登陆后修改密码**        | `POST /auth/password`（需 `bearerAuth`）               | 已登录用户修改密码，可提供 `oldPassword` 或通过 token 修改，返回更新后的 `UserDTO`       |
