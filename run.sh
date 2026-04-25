#!/bin/bash
# LearnHub Backend QuickStart Script for Linux/macOS
# Requires: Java 17+, Maven 3+

# Colors for output
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}🚀 Starting LearnHub Backend...${NC}"

# 1. Clean and Compile
echo -e "${YELLOW}📦 Compiling project...${NC}"
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed! Check your Java/Maven installation.${NC}"
    exit $?
fi

# 2. Run Application
echo -e "${GREEN}🔥 Launching Spring Boot...${NC}"
mvn spring-boot:run
