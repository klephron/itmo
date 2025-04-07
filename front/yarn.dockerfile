# Generate lock file from bun for compatibility
FROM oven/bun:1.2.8-debian AS generator

WORKDIR /opt/build

COPY package.json bun.lock ./
RUN bun install --yarn


FROM node:23-alpine AS runner

WORKDIR /opt/build

COPY --from=generator /opt/build/yarn.lock ./

CMD ["tail", "-f", "/dev/null"]
