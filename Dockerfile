# ==========================================
# Estágio 1: Build da aplicação Spring Boot
# ==========================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Cache de dependências Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compilação e empacotamento da aplicação
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Estágio 2: Imagem final de execução (JRE leve)
# ==========================================
FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

# Criação de usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring

# Copia o artefato gerado no estágio anterior
COPY --from=builder /build/target/*.jar app.jar
RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
