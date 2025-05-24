import { type RouteConfig, route } from "@react-router/dev/routes";

export default [
  route("index.html", "pages/app.tsx"),
  route("*?", "pages/catchall.tsx"),
] satisfies RouteConfig;
