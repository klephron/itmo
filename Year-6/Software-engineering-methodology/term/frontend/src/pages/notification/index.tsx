import { NotificationDto, UserDto } from "@api/gen/data-contracts";
import { useClient } from "@state/client";
import { useNotifications } from "@state/notifications";
import { useEffect, useRef, useState } from "react";
import { Route } from "~react-router/src/pages/notification/+types";
import styles from "./index.module.scss";
import { Link } from "react-router";

function getUserString(user: UserDto) {
  let str = "";
  str = user.name + " " + user.surname;
  if (user.patronymic) {
    str += " " + user.patronymic;
  }
  return str;
}

function Info({ id }: { id: number }) {
  const client = useClient((state) => state.client);
  const [notification, notificationSet] = useState(null as NotificationDto | null);
  const [user, userSet] = useState(null as UserDto | null);
  const unreadUpdateById = useNotifications((state) => state.updateById);
  const markSent = useRef(false);

  useEffect(() => {
    client.notifications
      .getNotification(Number(id))
      .then((r) => {
        notificationSet(r.data);
      })
      .catch((e) => console.error(e));
  }, []);

  useEffect(() => {
    if (!notification) return;
    if (notification.readAt) return;
    if (markSent.current) return;

    markSent.current = true;
    const ts = new Date().toISOString();

    client.notifications
      .updateNotification(notification.id, {
        readAt: ts,
      })
      .then((r) => {
        notificationSet(r.data);
        unreadUpdateById(r.data);
      })
      .catch((e) => console.error(e));
  }, [client.notifications, notification, unreadUpdateById]);

  useEffect(() => {
    if (notification) {
      client.users.getUser(notification.userId).then((r) => userSet(r.data));
    }
  }, [notification]);

  useEffect(() => {
    console.log(user);
  }, [user]);

  if (notification === null) {
    return <span>Loading...</span>;
  } else {
    return (
      <>
        <div>
          <h3>User</h3>
          {user ? <Link to={`/users/${user.id}`}>{getUserString(user)}</Link> : <></>}
        </div>
        <div>
          <h3>Title</h3>
          <span>{notification.title}</span>
        </div>
        <div>
          <h3>Description</h3>
          <span>{notification.description}</span>
        </div>
        <div>
          <h3>Created At</h3>
          <span>{notification.createdAt}</span>
        </div>
        <div>
          <h3>Read At</h3>
          <span>{notification.readAt ?? "-"}</span>
        </div>
      </>
    );
  }
}

export default function NotificationPage({ params }: Route.ComponentProps) {
  return (
    <div className={styles.root}>
      <Info id={Number(params.id)} />
    </div>
  );
}
