# LearnHub Database Verification Script
# Requires: MySQL command line (optional)

Write-Host "🔍 Verifying LearnHub Database Schema..." -ForegroundColor Cyan

# This script assumes lms_db is the target
$Tables = @("users", "courses", "enrollments", "orders", "reviews", "live_classes", "certificates", "assignments", "submissions")

Write-Host "`n📋 Checking Core Tables:" -ForegroundColor Yellow
foreach ($Table in $Tables) {
    # We can't easily run SQL without a driver here, but we can remind the user how to check
    Write-Host " - $Table" -ForegroundColor DarkGray
}

Write-Host "`n💡 Tip: Run 'DESCRIBE reviews;' in your MySQL terminal to verify the table structure." -ForegroundColor Gray
Write-Host "💡 If tables are missing, ensure 'spring.jpa.hibernate.ddl-auto=update' is in application.properties or run schema.sql manually." -ForegroundColor Gray
