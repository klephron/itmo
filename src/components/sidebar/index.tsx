import { Link } from "react-router";
import clsx from "clsx";
import { useNotifications } from "@state/notifications";
import { useEffect, useState } from "react";
import styles from "./index.module.scss";

export default function Sidebar({ className }: { className?: string }) {
  const notifications = useNotifications((state) => state.notifications);
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    if (notifications) {
      setUnread(notifications.length);
    }
  }, [notifications]);

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
          Notifications {unread ? ` (${unread})` : ""}
        </Link>
        <Link to="/profile" className={styles.element}>
          Profile
        </Link>
        <Link to="/logout" className={styles.element}>
          Log Out
        </Link>
      </div>
    </>
  );
}
