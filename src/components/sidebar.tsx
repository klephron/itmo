import { Link } from "react-router";

import styles from "./sidebar.module.scss";
import clsx from "clsx";

export default function Sidebar({ className }: { className?: string }) {
  return (
    <>
      <div className={clsx(styles.sidebar, className)}>
        <Link to="/incidents" className={styles.element}>
          Incidents
        </Link>
        <Link to="/cases" className={styles.element}>
          Cases
        </Link>
        <Link to="/users" className={styles.element}>
          Users
        </Link>
        <Link to="/notifications" className={styles.element}>
          Notifications
        </Link>
        <Link to="/profile" className={styles.element}>
          Profile
        </Link>
        <Link to="/logout" className={styles.element}>
          Logout
        </Link>
      </div>
    </>
  );
}
