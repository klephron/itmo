import { Outlet } from "react-router";
import Header from "@components/header";
import Sidebar from "@components/sidebar";
import Logo from "@components/logo";
import NotificationsLive from "@components/notifications-live";
import styles from "./index.module.scss";

export default function DashboardLayout() {
  return (
    <>
      <div className={styles.dashboard}>
        <Logo className={styles.topleft} />
        <Header className={styles.topright} />
        <Sidebar className={styles.bottomleft} />
        <div className={styles.bottomright}>
          <Outlet />
        </div>
      </div>
      <NotificationsLive />
    </>
  );
}
