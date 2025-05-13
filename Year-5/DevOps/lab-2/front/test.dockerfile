FROM oven/bun:1.2.8-debian AS generator

WORKDIR /app

# Generate yarn lock file because bun is unable to run vitest inside container
COPY package.json bun.lock ./
RUN bun install --yarn


FROM node:23-alpine AS runner

WORKDIR /app

COPY --from=generator /app/package.json /app/yarn.lock ./
RUN yarn install

COPY vite.config.ts tsconfig.json ./

CMD ["yarn", "run", "test"]
