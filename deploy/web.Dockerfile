# tea-saas 前端+反代镜像（nginx 托管 dist 并反代 API/WS/OSS）
# dist/ 与 nginx.conf 由 build.ps1 复制到本目录
FROM nginx:stable-alpine

COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
