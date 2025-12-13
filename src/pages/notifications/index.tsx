import { Link } from "react-router";
import { useClient } from "@state/client";
import { useNotifications } from "@state/notifications";
import styles from "./index.module.scss";
import Checkbox from "@components/checkbox";
import { useEffect, useState } from "react";
import { NotificationDto, NotificationsControllerStatus } from "@api/gen/data-contracts";
import { datetimeLocalToUtcIso, formatUtc } from "@util";

function sliceRemaining(text: string | null | undefined) {
  if (!text) return "";
  return text;
}

function toInputValue(value?: string) {
  if (!value) return "";
  try {
    return new Date(value).toISOString().slice(0, 16);
  } catch {
    return "";
  }
}

function toIso(value?: string) {
  return datetimeLocalToUtcIso(value);
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
              <td>{sliceRemaining(notification.description)}</td>
              <td>{formatUtc(notification.createdAt)}</td>
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
  const [searchFrom, searchFromSet] = useState(toInputValue(new Date(0).toISOString()));
  const [searchTo, searchToSet] = useState(toInputValue(new Date().toISOString()));

  useEffect(() => {
    search();
  }, []);

  function search() {
    client.notifications
      .getNotifications({
        from: toIso(searchFrom),
        to: toIso(searchTo),
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
          <input
            type="datetime-local"
            value={searchFrom}
            onChange={(e) => searchFromSet(e.target.value)}
          />
        </div>
        <div>
          <label>To (UTC):</label>
          <input
            type="datetime-local"
            value={searchTo}
            onChange={(e) => searchToSet(e.target.value)}
          />
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
              <td>{sliceRemaining(notification.description)}</td>
              <td>{formatUtc(notification.createdAt)}</td>
              <td>{notification.readAt ? formatUtc(notification.readAt) : "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}

export default function NotificationsPage(props: { descriptionMaxLen?: number }) {
  const descriptionMaxLen = props.descriptionMaxLen ?? 20;
  const unread = useNotifications((state) => state.unread);
  const unreadCount = unread?.size ?? 0;

  return (
    <div className={styles.root}>
      <div className={styles.unread}>
        <div className={styles.headerRow}>
          <h2>Unread Notifications</h2>
          <span className={styles.badge}>{unreadCount}</span>
        </div>
        <UnreadTable descriptionMax={descriptionMaxLen} />
      </div>
      <div className={styles.search}>
        <h2>Search Notifications</h2>
        <Search descriptionMax={descriptionMaxLen} />
      </div>
    </div>
  );
}
