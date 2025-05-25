import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
  index("pages/index.tsx"),
  route("login", "pages/login.tsx"),
  route("*?", "pages/catchall.tsx"),
] satisfies RouteConfig;
