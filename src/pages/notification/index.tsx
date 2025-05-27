import { NotificationDto, UserDto } from "@api/gen/data-contracts";
import { useClient } from "@state/client";
import { useEffect, useState } from "react";
import { Route } from "~react-router/src/pages/notification/+types";
import styles from "./index.module.scss";
import { Link } from "react-router";

export default function NotificationPage({ params }: Route.ComponentProps) {
  const client = useClient((state) => state.client);
  const [notification, notificationSet] = useState(null as NotificationDto | null);
  const [user, userSet] = useState(null as UserDto | null);

  useEffect(() => {
    client.notifications
      .getNotification(Number(params.id))
      .then((r) => {
        notificationSet(r.data);
      })
      .catch((e) => alert(e));
  }, []);

  useEffect(() => {
    if (notification) {
      client.users.getUser(notification.userId).then((r) => userSet(r.data));
    }
  }, [notification]);

  useEffect(() => {
    console.log(user);
  }, [user]);

  function getUserString(user: UserDto) {
    let str = "";
    str = " (" + user.name + " " + user.surname;
    if (user.patronymic) {
      str += " " + user.patronymic;
    }
    str += ")";
    return str;
  }

  function Info() {
    if (notification === null) {
      return <span>Loading...</span>;
    } else {
      return (
        <>
          <div>
            <h3>ID</h3>
            <span>{notification.id}</span>
          </div>
          <div>
            <h3>User</h3>
            {user ? (
              <Link to={`/users/${user.id}`}>
                {notification.userId} {getUserString(user)}
              </Link>
            ) : (
              <></>
            )}
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

  return <div className={styles.root}>{Info()}</div>;
}
