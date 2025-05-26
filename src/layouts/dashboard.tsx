import { Outlet } from "react-router";

import Header from "@components/header";
import Sidebar from "@components/sidebar";

import styles from "./dashboard.module.scss";
import Logo from "@components/logo";

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
    </>
  );
}
