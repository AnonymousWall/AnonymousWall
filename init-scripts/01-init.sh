#!/bin/bash
# Oracle Database initialization script
# This runs automatically when the Oracle container starts for the first time
# Ensures the database is properly configured for the application

set -e

echo "Starting Oracle Database initialization..."

# Wait for Oracle to be fully ready
sleep 10

# The application will handle schema creation via Liquibase
# This script is here for any custom Oracle-specific setup if needed

echo "Oracle Database initialization complete."
echo "Schema will be created automatically by Liquibase on first application start."
