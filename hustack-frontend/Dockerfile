FROM node:14.21.3 as react-build
WORKDIR /app
COPY package.json .
# COPY package-lock.json .
RUN npm i
# Sửa fetch__default["default"] thành globalThis.fetch
RUN find node_modules/@react-pdf -type f -name '*.js' -exec sed -i 's/fetch__default\["default"\](/(typeof fetch === "function" ? fetch : require("cross-fetch"))(/g' {} +
COPY . ./
RUN npm run build

FROM leanhtuan/nginx-with-brotli:latest
COPY default.conf /etc/nginx/nginx.conf
COPY --from=react-build /app/build /usr/share/nginx/html
COPY docker-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
