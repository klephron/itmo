import { UserInfoResponse } from "@api/gen/data-contracts";
import { useClient } from "@state/client";
import { Link } from "react-router";
import { useEffect, useState } from "react";
import styles from "./index.module.scss";

type UserEntry = {
  id: number;
  username: string;
  name?: string;
  surname?: string;
  patronymic?: string;
  locationLatitude?: number;
  locationLongitude?: number;
  locationAltitude?: number;
};

function infoToMap(infos: UserInfoResponse[]) {
  return new Map(infos.map((info) => [info.id, { id: info.id, username: info.username }]));
}

export default function UsersPage() {
  const client = useClient((state) => state.client);

  const [users, usersSet] = useState(null as Map<number, UserEntry> | null);
  const [usersLoaded, usersLoadedSet] = useState(false);

  useEffect(() => {
    client.users
      .getUsers()
      .then((r) => usersSet(infoToMap(r.data)))
      .catch(alert);
  }, [client]);

  useEffect(() => {
    if (!users || usersLoaded) {
      return;
    }
    (async () => {
      const users1 = new Map(users);
      for (let user of users1.values()) {
        await client.users
          .getUser(user.id)
          .then((r) => {
            users1.set(user.id, {
              id: user.id,
              username: user.username,
              name: r.data.name,
              surname: r.data.surname,
              patronymic: r.data.patronymic ?? undefined,
              locationLatitude: r.data.locationLatitude,
              locationLongitude: r.data.locationLongitude,
              locationAltitude: r.data.locationAltitude,
            });
          })
          .catch(console.error);
      }
      usersLoadedSet(true);
      usersSet(users1);
    })();
  }, [users]);

  function Search() {
    return <></>;
  }

  function Table() {
    if (!users) {
      return <span>Loading...</span>;
    } else {
      return (
        <table className={styles.table}>
          <thead>
            <tr className={styles.row}>
              <th>Username</th>
              <th>Name</th>
              <th>Primary Position</th>
              <th>Location</th>
            </tr>
          </thead>
          <tbody>
            {Array.from(users.values()).map((user) => (
              <tr className={styles.row} key={user.id}>
                <td>
                  <Link to={`/users/${user.id}`}>{user.username}</Link>
                </td>
                <td>
                  {user.name} {user.surname}
                  {user.patronymic ? " " + user.patronymic : ""}
                </td>
                <td>TODO: -</td>
                <td>
                  {user.locationLatitude}, {user.locationLongitude}, {user.locationAltitude}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      );
    }
  }
  return (
    <div className={styles.root}>
      {Search()}
      <h2>Users</h2>
      {Table()}
    </div>
  );
}
