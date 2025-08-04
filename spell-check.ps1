# Spell Check Script for SecureSms
# Run this script to check spelling locally

Write-Host "SecureSms Local Spell Check" -ForegroundColor Green
Write-Host "=========================" -ForegroundColor Green

# Check if Node.js is installed
try {
    $nodeVersion = node --version
    Write-Host "Node.js version: $nodeVersion" -ForegroundColor Yellow
} catch {
    Write-Host "Error: Node.js is not installed. Please install Node.js from https://nodejs.org/" -ForegroundColor Red
    exit 1
}

# Check if npm packages are installed
if (!(Test-Path "node_modules")) {
    Write-Host "Installing spell check dependencies..." -ForegroundColor Yellow
    npm install
}

Write-Host "Running spell check..." -ForegroundColor Yellow
npm run spell-check

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Spell check passed!" -ForegroundColor Green
} else {
    Write-Host "✗ Spell check found issues. Review the output above." -ForegroundColor Red
    Write-Host "Tip: You can add technical terms to cspell.json words array if they are correct." -ForegroundColor Yellow
}
