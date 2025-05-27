import { type RouteConfig, index, layout, route } from "@react-router/dev/routes";

export default [
  layout("./layouts/authenticated/index.tsx", [
    layout("./layouts/dashboard/index.tsx", [
      index("./pages/index/index.tsx", { id: "/" }),
      route("index.html", "./pages/index/index.tsx"),

      route("incidents", "./pages/incidents/index.tsx"),
      route("cases", "./pages/cases/index.tsx"),
      route("users", "./pages/users/index.tsx"),
      route("notifications", "./pages/notifications/index.tsx"),
      route("notifications/:id", "./pages/notification/index.tsx"),
      route("profile", "./pages/profile/index.tsx"),

      route("logout", "./pages/logout/index.tsx"),
    ]),
  ]),

  route("login", "./pages/login/index.tsx"),
  route("refresh", "./pages/refresh/index.tsx"),

  route("*?", "./pages/catchall/index.tsx"),
] satisfies RouteConfig;
