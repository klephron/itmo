FROM node:25-slim AS deps

WORKDIR /opt/brainstop/frontend

COPY package.json package-lock.json ./

RUN --mount=type=cache,id=npm,target=/root/.npm \
    npm ci


FROM deps AS build

WORKDIR /opt/brainstop/frontend

COPY ./ ./

RUN npm run build


FROM nginx:stable-alpine

COPY --from=build /opt/brainstop/frontend/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
