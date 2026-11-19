FROM node:24-alpine AS build
WORKDIR /app
COPY webStorefront/package.json webStorefront/package-lock.json ./
RUN npm ci --ignore-scripts
COPY webStorefront/ ./
RUN npm test && npm run build

FROM node:24-alpine
ENV NODE_ENV=production PORT=8080
WORKDIR /app
COPY --from=build --chown=node:node /app/dist ./dist
COPY --from=build --chown=node:node /app/server.cjs ./server.cjs
USER node
EXPOSE 8080
CMD ["node", "server.cjs"]
