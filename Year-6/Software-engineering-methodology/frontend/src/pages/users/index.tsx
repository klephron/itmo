import { UserInfoResponse } from "@api/gen/data-contracts";
import { useClient } from "@state/client";
import { Link } from "react-router";
import { useEffect, useState } from "react";
import styles from "./index.module.scss";
import { hasPermission, usePermissions } from "@state/permissions";
import { PermissionRequestHint } from "@components/permission-request";

type UserEntry = {
  id: number;
  username: string;
  name?: string;
  surname?: string;
  patronymic?: string;
  primaryPosition?: string;
  locationLatitude?: number;
  locationLongitude?: number;
  locationAltitude?: number;
};

function infoToMap(infos: UserInfoResponse[]) {
  return new Map(infos.map((info) => [info.id, { id: info.id, username: info.username }]));
}

function Table({
  canRead,
  canViewDebug,
  reloadKey,
}: {
  canRead: boolean;
  canViewDebug: boolean;
  reloadKey: number;
}) {
  const client = useClient((state) => state.client);
  const [users, usersSet] = useState(null as Map<number, UserEntry> | null);
  const [usersLoaded, usersLoadedSet] = useState(false);

  useEffect(() => {
    if (!canRead) {
      usersSet(null);
      return;
    }
    client.users
      .getUsers()
      .then((r) => {
        usersSet(infoToMap(r.data));
        usersLoadedSet(false);
      })
      .catch(console.error);
  }, [client, canRead, reloadKey]);

  useEffect(() => {
    if (!users || usersLoaded) {
      return;
    }
    (async () => {
      const users1 = new Map(users);
      for (const user of users1.values()) {
        await client.users
          .getUser(user.id)
          .then((r) => {
            users1.set(user.id, {
              id: user.id,
              username: user.username,
              name: r.data.name,
              surname: r.data.surname,
              patronymic: r.data.patronymic ?? undefined,
              primaryPosition: r.data.primaryPosition ?? undefined,
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

  if (!canRead) {
    return (
      <PermissionRequestHint
        permission="USERS:*:READ"
        message="No permission to view users"
        showDebug={canViewDebug}
        reason="users list"
      />
    );
  }

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
        {Array.from(users ? users.values() : []).map((user) => (
          <tr className={styles.row} key={user.id}>
            <td>
              <Link to={`/users/${user.id}`}>{user.username}</Link>
            </td>
            <td>
              {user.name} {user.surname}
              {user.patronymic ? " " + user.patronymic : ""}
            </td>
            <td>{user.primaryPosition ?? "Not Specified"}</td>
            <td>
              {user.locationLatitude}, {user.locationLongitude}, {user.locationAltitude}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default function UsersPage() {
  const client = useClient((state) => state.client);
  const permissions = usePermissions((state) => state.permissions);
  const canViewDebug = !!permissions && hasPermission("LOGS:*:READ");
  const canRead = !!permissions && hasPermission("USERS:*:READ");
  const canAdd = !!permissions && hasPermission("USERS:*:ADD");
  const canListPositions = !!permissions && hasPermission("USERS:*:LIST_SYSTEM_PERMISSIONS");

  const [reloadKey, reloadKeySet] = useState(0);
  const [showCreate, showCreateSet] = useState(false);
  const [creating, creatingSet] = useState(false);
  const [createError, createErrorSet] = useState<string | null>(null);
  const [createOk, createOkSet] = useState<string | null>(null);

  const [positions, positionsSet] = useState<string[]>([]);
  const [positionsLoaded, positionsLoadedSet] = useState(false);

  const [username, usernameSet] = useState("");
  const [password, passwordSet] = useState("");
  const [name, nameSet] = useState("");
  const [surname, surnameSet] = useState("");
  const [patronymic, patronymicSet] = useState("");
  const [primaryPosition, primaryPositionSet] = useState("");

  useEffect(() => {
    if (!showCreate || !canAdd || !canListPositions || positionsLoaded) return;
    client.users
      .getPrimaryPositions()
      .then((r) => {
        const normalized = r.data.map((p: any) =>
          typeof p === "string" ? p : p.value ?? p.label ?? String(p)
        );
        positionsSet(normalized);
        positionsLoadedSet(true);
        if (!primaryPosition && normalized.length) {
          primaryPositionSet(normalized[0]);
        }
      })
      .catch((e) => {
        console.error(e);
        positionsLoadedSet(true);
      });
  }, [showCreate, canAdd, canListPositions, client, positionsLoaded, primaryPosition]);

  async function submitCreate() {
    createOkSet(null);
    createErrorSet(null);
    if (!canAdd) {
      createErrorSet("No permission to create user");
      return;
    }
    if (!username.trim() || !password.trim() || !name.trim() || !surname.trim() || !primaryPosition.trim()) {
      createErrorSet("username, password, name, surname, primaryPosition are required");
      return;
    }
    creatingSet(true);
    try {
      await client.users.addUser({
        username: username.trim(),
        password: password,
        name: name.trim(),
        surname: surname.trim(),
        patronymic: patronymic.trim() ? patronymic.trim() : null,
        primaryPosition: primaryPosition.trim(),
      });
      createOkSet("Ok");
      usernameSet("");
      passwordSet("");
      nameSet("");
      surnameSet("");
      patronymicSet("");
      primaryPositionSet(positions[0] ?? "");
      reloadKeySet((v) => v + 1);
    } catch (e: any) {
      const msg =
        e?.rawBody ||
        e?.error?._embedded?.errors?.map((er: any) => er.message).join("\n") ||
        (typeof e?.error === "string" ? e.error : null) ||
        e?.error?.message ||
        e?.message ||
        "Failed to create user";
      createErrorSet(msg);
    } finally {
      creatingSet(false);
    }
  }

  return (
    <div className={styles.root}>
      <h2>Users</h2>
      <div className={styles.menu}>
        <button type="button" onClick={() => showCreateSet((v) => !v)} disabled={!canAdd}>
          {showCreate ? "Close" : "Create"}
        </button>
        {!canAdd ? (
          <PermissionRequestHint
            permission="USERS:*:ADD"
            message="No permission to create users"
            showDebug={canViewDebug}
            reason="users list create"
          />
        ) : null}
      </div>

      {showCreate ? (
        <div className={styles.card}>
          <h3>Create user</h3>
          {canAdd && !canListPositions ? (
            <div className={styles.hint}>
              No permission for positions list
              {canViewDebug ? " (USERS:*:LIST_SYSTEM_PERMISSIONS)" : ""}. Enter enum manually.
            </div>
          ) : null}
          <div className={styles.form}>
            <label>
              Username
              <input value={username} onChange={(e) => usernameSet(e.target.value)} />
            </label>
            <label>
              Password
              <input value={password} type="password" onChange={(e) => passwordSet(e.target.value)} />
            </label>
            <label>
              Name
              <input value={name} onChange={(e) => nameSet(e.target.value)} />
            </label>
            <label>
              Surname
              <input value={surname} onChange={(e) => surnameSet(e.target.value)} />
            </label>
            <label>
              Patronymic
              <input value={patronymic} onChange={(e) => patronymicSet(e.target.value)} />
            </label>
            <label>
              Primary position
              {canListPositions && positions.length ? (
                <select value={primaryPosition} onChange={(e) => primaryPositionSet(e.target.value)}>
                  {positions.map((p) => (
                    <option key={p} value={p}>
                      {p}
                    </option>
                  ))}
                </select>
              ) : (
                <input value={primaryPosition} onChange={(e) => primaryPositionSet(e.target.value)} />
              )}
            </label>
            <div className={styles.actions}>
              <button type="button" onClick={submitCreate} disabled={!canAdd || creating}>
                Submit
              </button>
              {createOk ? <span>{createOk}</span> : null}
              {createError ? <span className={styles.error}>{createError}</span> : null}
            </div>
          </div>
        </div>
      ) : null}

      <Table canRead={canRead} canViewDebug={canViewDebug} reloadKey={reloadKey} />
    </div>
  );
}
