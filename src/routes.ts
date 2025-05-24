import { type RouteConfig, route } from "@react-router/dev/routes";

export default [
  route("index.html", "app.tsx"),
  route("*?", "catchall.tsx"),
] satisfies RouteConfig;
