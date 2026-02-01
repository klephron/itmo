import { Link } from "react-router";
import clsx from "clsx";
import { useNotifications } from "@state/notifications";
import { useEffect, useState } from "react";
import styles from "./index.module.scss";
import { useUser } from "@state/user";
import { usePermissions, hasPermission } from "@state/permissions";

export default function Sidebar({ className }: { className?: string }) {
  const notifications = useNotifications((state) => state.unread);
  const user = useUser((state) => state.user);
  const permissions = usePermissions((state) => state.permissions);

  const [unread, setUnread] = useState(0);
  const canViewLogs = permissions ? hasPermission("LOGS:*:READ") : false;

  useEffect(() => {
    if (notifications) {
      notifications.values().filter((e) => e.readAt === null);
      setUnread(notifications.size);
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
        {canViewLogs ? (
          <Link to="/logs" className={styles.element}>
            Logs
          </Link>
        ) : null}
        <Link to="/notifications" className={styles.element}>
          Notifications {unread ? ` (${unread})` : ""}
        </Link>
        <Link to={`/users/${user.id}`} className={styles.element}>
          Profile
        </Link>
        <Link to="/logout" className={styles.element}>
          Log Out
        </Link>
      </div>
    </>
  );
}
