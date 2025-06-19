#!/bin/sh

cat <<EOF > /usr/share/nginx/html/env.js
window.ENV = {
  AUTH_URL: "${AUTH_URL}",
  REALM: "${REALM}",
  CLIENT_ID: "${CLIENT_ID}",
  API_URL: "${API_URL}",
  PLATFORM_NAME: "${PLATFORM_NAME}",
};
EOF

exec "$@"
