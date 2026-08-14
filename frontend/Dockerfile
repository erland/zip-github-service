FROM node:22-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG ZIP_GITHUB_VERSION=development
RUN VITE_ZIP_GITHUB_VERSION="${ZIP_GITHUB_VERSION}" npm run build

FROM nginx:1.27-alpine
ENV ZIP_GITHUB_NGINX_CLIENT_MAX_BODY_SIZE=200M
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80
