FROM node:14.21.3 as react-build
WORKDIR /app
COPY package.json .
# COPY package-lock.json .
RUN npm i
RUN rm -rf ./node_module/@uiw/react-codemirror/node_module
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
