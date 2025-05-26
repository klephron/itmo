import { type RouteConfig, index, layout, route } from "@react-router/dev/routes";

export default [
  layout("layouts/authenticated.tsx", [
    layout("layouts/dashboard.tsx", [
      index("pages/index.tsx", { id: "/" }),
      route("index.html", "pages/index.tsx"),
    ]),
  ]),

  route("login", "pages/login.tsx"),
  route("refresh", "pages/refresh.tsx"),

  route("*?", "pages/catchall.tsx"),
] satisfies RouteConfig;
