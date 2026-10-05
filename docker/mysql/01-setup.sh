#!/bin/bash

MYSQL_PWD="${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD saknas}" mysql --protocol=socket --user=root <<EOSQL
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'webshop'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, REFERENCES
    ON webshop.* TO 'webshop'@'%';
EOSQL
