import { type RouteConfig, index, layout, route } from "@react-router/dev/routes";

export default [
  layout("./layouts/authenticated.tsx", [
    layout("./layouts/dashboard.tsx", [
      index("./pages/index.tsx", { id: "/" }),
      route("index.html", "./pages/index.tsx"),

      route("incidents", "./pages/incidents.tsx"),
      route("cases", "./pages/cases.tsx"),
      route("users", "./pages/users.tsx"),
      route("notifications", "./pages/notifications.tsx"),
      route("profile", "./pages/profile.tsx"),

      route("logout", "./pages/logout.tsx"),
    ]),
  ]),

  route("login", "./pages/login.tsx"),
  route("refresh", "./pages/refresh.tsx"),

  route("*?", "./pages/catchall.tsx"),
] satisfies RouteConfig;
