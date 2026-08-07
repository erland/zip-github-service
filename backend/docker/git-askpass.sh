#!/bin/sh
case "$1" in
  *Username*) printf '%s\n' 'x-access-token' ;;
  *) printf '%s\n' "$ZIP_GITHUB_GIT_TOKEN" ;;
esac
