# LearnHub Backend QuickStart Script
# Requires: Java 17+, Maven 3+

Write-Host "🚀 Starting LearnHub Backend..." -ForegroundColor Cyan

# 1. Clean and Compile
Write-Host "📦 Compiling project..." -ForegroundColor Yellow
mvn clean install -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed! Check your Java/Maven installation." -ForegroundColor Red
    exit $LASTEXITCODE
}

# 2. Run Application
Write-Host "🔥 Launching Spring Boot..." -ForegroundColor Green
mvn spring-boot:run
