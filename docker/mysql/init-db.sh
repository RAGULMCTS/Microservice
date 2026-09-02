#!/bin/bash
set -euo pipefail

# Runs once, automatically, on first container start (mysql image convention:
# anything in /docker-entrypoint-initdb.d/ is executed against a fresh datadir).
# Values come only from environment variables injected by docker-compose from
# ".env" — nothing here is a hardcoded secret.

mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<-SQL
    CREATE DATABASE IF NOT EXISTS renthub_user;
    CREATE DATABASE IF NOT EXISTS renthub_property;

    CREATE USER IF NOT EXISTS '${MYSQL_APP_USER}'@'%' IDENTIFIED BY '${MYSQL_APP_PASSWORD}';
    GRANT ALL PRIVILEGES ON renthub_user.* TO '${MYSQL_APP_USER}'@'%';
    GRANT ALL PRIVILEGES ON renthub_property.* TO '${MYSQL_APP_USER}'@'%';
    FLUSH PRIVILEGES;
SQL
