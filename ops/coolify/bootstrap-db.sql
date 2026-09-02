-- Run once as a PostgreSQL administrator after replacing CHANGE_ME.
-- Keep the password out of source control.

CREATE ROLE zip_github
    LOGIN
    PASSWORD 'CHANGE_ME';

CREATE DATABASE zip_github
    OWNER zip_github;
