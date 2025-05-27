import { Link } from "react-router";
import { useClient } from "@state/client";
import { useNotifications } from "@state/notifications";
import styles from "./index.module.scss";
import Checkbox from "@components/checkbox";
import { useEffect, useState } from "react";
import { NotificationDto, NotificationsControllerStatus } from "@api/gen/data-contracts";

function sliceRemaining(text: string | null | undefined, max: number) {
  if (text) {
    const len = text.length;
    if (len > max) {
      return text.slice(0, max).trim() + " ...";
    }
    return text.slice(0, max).trim();
  }
}

function UnreadTable({ descriptionMax }: { descriptionMax: number }) {
  const client = useClient((state) => state.client);
  const unread = useNotifications((state) => state.unread);
  const unreadUpdateById = useNotifications((state) => state.updateById);

  if (unread === null) {
    return <span>Loading...</span>;
  } else if (unread.size === 0) {
    return <span>No notifications to read</span>;
  } else {
    return (
      <table className={styles.table}>
        <thead>
          <tr className={styles.row}>
            <th>Title</th>
            <th>Description</th>
            <th>Created Time (UTC)</th>
            <th>Read</th>
          </tr>
        </thead>
        <tbody>
          {Array.from(unread.values()).map((notification) => (
            <tr className={styles.row} key={notification.id}>
              <td>
                <Link to={`/notifications/${notification.id}`}>{notification.title}</Link>
              </td>
              <td>{sliceRemaining(notification.description, descriptionMax)}</td>
              <td>{new Date(notification.createdAt).toLocaleString()}</td>
              <td>
                <Checkbox
                  isChecked={!!notification.readAt}
                  onChange={(v) => {
                    if (v) {
                      client.notifications
                        .updateNotification(notification.id, {
                          readAt: new Date().toISOString(),
                        })
                        .then((r) => unreadUpdateById(r.data))
                        .catch((e) => console.error(e));
                    }
                  }}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }
}

function Search({ descriptionMax }: { descriptionMax: number }) {
  const client = useClient((state) => state.client);

  const [searchNotifications, searchNotificationsSet] = useState([] as NotificationDto[]);
  const [searchStatus, searchStatusSet] = useState(NotificationsControllerStatus.ALL);
  const [searchFrom, searchFromSet] = useState(new Date(0).toISOString());
  const [searchTo, searchToSet] = useState(new Date().toISOString());

  useEffect(() => {
    search();
  }, []);

  function search() {
    client.notifications
      .getNotifications({
        from: searchFrom,
        to: searchTo,
        status: searchStatus,
      })
      .then((r) => {
        searchNotificationsSet(r.data);
      })
      .catch((e) => console.error(e));
  }

  return (
    <>
      <div className={styles.panel}>
        <button onClick={search}>Search</button>
        <div>
          <label>From (UTC):</label>
          <input type="input" value={searchFrom} onChange={(e) => searchFromSet(e.target.value)} />
        </div>
        <div>
          <label>To (UTC):</label>
          <input type="input" value={searchTo} onChange={(e) => searchToSet(e.target.value)} />
        </div>
        <select
          value={searchStatus}
          onChange={(e) =>
            searchStatusSet(
              NotificationsControllerStatus[
                e.target.value as keyof typeof NotificationsControllerStatus
              ]
            )
          }
        >
          {Object.values(NotificationsControllerStatus).map((status) => (
            <option value={status} key={status}>
              {status}
            </option>
          ))}
        </select>
      </div>
      <table className={styles.table}>
        <thead>
          <tr className={styles.row}>
            <th>Title</th>
            <th>Description</th>
            <th>Created Time (UTC)</th>
            <th>Read Time (UTC)</th>
          </tr>
        </thead>
        <tbody>
          {searchNotifications.map((notification) => (
            <tr className={styles.row} key={notification.id}>
              <td>
                <Link to={`/notifications/${notification.id}`}>{notification.title}</Link>
              </td>
              <td>{sliceRemaining(notification.description, descriptionMax)}</td>
              <td>{new Date(notification.createdAt).toLocaleString()}</td>
              <td>{notification.readAt ? new Date(notification.readAt).toLocaleString() : "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}

export default function NotificationsPage(props: { descriptionMaxLen?: number }) {
  const descriptionMaxLen = props.descriptionMaxLen ?? 20;

  return (
    <div className={styles.root}>
      <div className={styles.unread}>
        <h2>Unread Notifications</h2>
        <UnreadTable descriptionMax={descriptionMaxLen} />
      </div>
      <div className={styles.search}>
        <h2>Search Notifications</h2>
        <Search descriptionMax={descriptionMaxLen} />
      </div>
    </div>
  );
}
