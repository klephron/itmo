import { useClient } from "@state/client";
import styles from "./index.module.scss";
import { Link } from "react-router";
import { useEffect, useMemo, useState } from "react";
import { DefaultPermissions, UserCaseDto, UserDto } from "@api/gen/data-contracts";
import { Route } from "~react-router/src/pages/user/+types";
import { hasPermission, usePermissions } from "@state/permissions";

function UserPermissions({ id }: { id: number }) {
  const client = useClient((state) => state.client);
  const permissionsState = usePermissions((state) => state.permissions);
  const [permissions, permissionsSet] = useState<Set<string> | null>(null);
  const [scopes, scopesSet] = useState<string[]>([]);
  const [scope, scopeSet] = useState("");
  const [available, availableSet] = useState<string[]>([]);
  const [selectedToAdd, selectedToAddSet] = useState<Set<string>>(new Set());
  const [targetType, targetTypeSet] = useState<"all" | "own" | "custom">("all");
  const [customTarget, customTargetSet] = useState("");
  const [preset, presetSet] = useState<DefaultPermissions>(DefaultPermissions.OPERATOR);
  const [saveMessage, saveMessageSet] = useState<string | null>(null);
  const [targetStatus, targetStatusSet] = useState<
    { status: "idle" | "checking" | "ok" | "error"; message?: string }
  >({ status: "idle" });

  const canUpdate = permissionsState ? hasPermission("USERS:*:UPDATE") : false;
  const canBrowsePermissions = permissionsState ? hasPermission("USERS:*:LIST_SYSTEM_PERMISSIONS") : false;
  const canViewDebug = permissionsState ? hasPermission("LOGS:*:READ") : false;

  useEffect(() => {
    client.users
      .getUserPermissions(Number(id))
      .then((r) => permissionsSet(new Set(r.data.permissions)))
      .catch((e) => console.error(e));
  }, [client, id]);

  useEffect(() => {
    if (!canBrowsePermissions) {
      return;
    }
    client.users
      .listPermissionScopes()
      .then((r) => {
        scopesSet(r.data.scopes);
        if (r.data.scopes.length && !scope) {
          scopeSet(r.data.scopes[0]);
        }
      })
      .catch((e) => console.error(e));
  }, [canBrowsePermissions, client]);

  useEffect(() => {
    if (!canBrowsePermissions || !scope) {
      availableSet([]);
      return;
    }
    client.users
      .getPermissionsInScope(scope)
      .then((r) => availableSet(r.data.permissions))
      .catch((e) => console.error(e));
  }, [canBrowsePermissions, client, scope]);

  const targetValue = useMemo(() => {
    if (targetType === "own") {
      return "$";
    }
    if (targetType === "custom") {
      return customTarget.trim();
    }
    return "*";
  }, [customTarget, targetType]);

  useEffect(() => {
    // проверка существования ID для custom target (users/cases/incidents)
    const applicable = targetType === "custom" && targetValue && scope && ["USERS", "CASES", "INCIDENTS"].includes(scope);
    if (!applicable) {
      targetStatusSet({ status: "idle" });
      return;
    }
    const idNum = Number(targetValue);
    if (!Number.isFinite(idNum)) {
      targetStatusSet({ status: "error", message: "ID должен быть числом" });
      return;
    }
    targetStatusSet({ status: "checking" });
    const ctrl = new AbortController();
    const timer = setTimeout(() => {
      let req: Promise<any>;
      if (scope === "USERS") {
        req = client.users.getUser(idNum, { signal: ctrl.signal });
      } else if (scope === "CASES") {
        req = client.cases.getCase(idNum, { signal: ctrl.signal });
      } else {
        req = client.incidents.getIncident(idNum, { signal: ctrl.signal });
      }
      req
        .then(() => targetStatusSet({ status: "ok", message: "ID найден" }))
        .catch(() => targetStatusSet({ status: "error", message: "ID не найден" }));
    }, 400);
    return () => {
      clearTimeout(timer);
      ctrl.abort();
    };
  }, [client, scope, targetType, targetValue]);

  function toggleSelection(permission: string) {
    const next = new Set(selectedToAdd);
    if (next.has(permission)) {
      next.delete(permission);
    } else {
      next.add(permission);
    }
    selectedToAddSet(next);
  }

  function addSelected() {
    if (!permissions || !scope) return;
    const target = targetValue || "*";
    const next = new Set(permissions);
    selectedToAdd.forEach((p) => next.add(`${scope}:${target}:${p}`));
    permissionsSet(next);
    selectedToAddSet(new Set());
  }

  function removePermission(p: string) {
    if (!permissions || !canUpdate) return;
    const next = new Set(permissions);
    next.delete(p);
    permissionsSet(next);
  }

  function saveChanges() {
    if (!permissions || !canUpdate) return;
    saveMessageSet(null);
    client.users
      .updateUserPermissions(id, { permissions: Array.from(permissions) })
      .then((r) => {
        permissionsSet(new Set(r.data.permissions));
        saveMessageSet("Ok");
      })
      .catch((e) => {
        console.error(e);
        saveMessageSet(e?.message || "Ошибка");
      });
  }

  function applyPreset() {
    if (!canUpdate) return;
    saveMessageSet(null);
    client.users
      .updateUserPermissionsByPreset(id, { preset })
      .then((r) => {
        permissionsSet(new Set(r.data.permissions));
        saveMessageSet("Ok");
      })
      .catch((e) => {
        console.error(e);
        saveMessageSet(e?.message || "Ошибка");
      });
  }

  if (!permissions) {
    return <span>Loading...</span>;
  } else {
    return (
      <div className={styles.permissions}>
        <div>
          <h4>Current Permissions</h4>
          {permissions.size === 0 ? (
            <span>Empty</span>
          ) : (
            <ul className={styles.permissionsList}>
              {Array.from(permissions).sort().map((permission) => (
                <li key={permission} className={styles.permissionItem}>
                  <span>{permission}</span>
                  {canUpdate ? (
                    <button onClick={() => removePermission(permission)}>Remove</button>
                  ) : null}
                </li>
              ))}
            </ul>
          )}
        </div>

        {canUpdate && canBrowsePermissions ? (
          <>
            <div className={styles.permissionsEditor}>
              <h4>Add Permissions</h4>
            <div className={styles.presetRow}>
              <label htmlFor="preset">Default preset</label>
              <select
                id="preset"
                value={preset}
                onChange={(e) => presetSet(e.target.value as DefaultPermissions)}
              >
                {Object.values(DefaultPermissions).map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
              <button type="button" onClick={applyPreset}>
                Apply preset
              </button>
            </div>
            <div className={styles.scopeRow}>
              <label htmlFor="scope">Scope</label>
              <select
                id="scope"
                value={scope}
                onChange={(e) => {
                  scopeSet(e.target.value);
                  selectedToAddSet(new Set());
                }}
              >
                {scopes.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </div>
            <div className={styles.targetRow}>
              <label>Target</label>
              <div className={styles.targetOptions}>
                <label>
                  <input
                    type="radio"
                    name="target"
                    value="all"
                    checked={targetType === "all"}
                    onChange={() => targetTypeSet("all")}
                  />
                  All (*)
                </label>
                <label>
                  <input
                    type="radio"
                    name="target"
                    value="own"
                    checked={targetType === "own"}
                    onChange={() => targetTypeSet("own")}
                  />
                  Own ($)
                </label>
                <label>
                  <input
                    type="radio"
                    name="target"
                    value="custom"
                    checked={targetType === "custom"}
                    onChange={() => targetTypeSet("custom")}
                  />
                  ID:
                </label>
                <input
                  type="text"
                  value={customTarget}
                  onChange={(e) => customTargetSet(e.target.value)}
                  disabled={targetType !== "custom"}
                  className={styles.targetInput}
                />
                {targetStatus.status === "checking" ? (
                  <span className={styles.hint}>Проверяем ID...</span>
                ) : targetStatus.status === "error" ? (
                  <span className={styles.hint}>{targetStatus.message}</span>
                ) : targetStatus.status === "ok" ? (
                  <span className={styles.small}>{targetStatus.message}</span>
                ) : null}
              </div>
              <span className={styles.hint}>Права записываются как SCOPE:TARGET:ACTION</span>
            </div>
            <div className={styles.available}>
              {available.map((perm) => {
                const composed = scope ? `${scope}:${targetValue || "*"}:${perm}` : perm;
                const checked = selectedToAdd.has(perm) || permissions.has(composed);
                return (
                  <label key={perm} className={styles.availableRow}>
                    <input
                      type="checkbox"
                      checked={checked}
                      disabled={permissions.has(composed)}
                      onChange={() => toggleSelection(perm)}
                    />
                    <span>
                      {scope}:{targetValue || "*"}:{perm}
                    </span>
                  </label>
                );
              })}
            </div>
              <div className={styles.actions}>
                <button
                  type="button"
                  onClick={addSelected}
                  disabled={!selectedToAdd.size || !scope || (targetType === "custom" && !customTarget.trim())}
                >
                  Add Selected
                </button>
                <button type="button" onClick={saveChanges}>
                  Save Changes
                </button>
              </div>
            </div>
            {saveMessage ? <div className={styles.hint}>{saveMessage}</div> : null}
          </>
        ) : null}

        {!canUpdate ? (
          <span className={styles.hint}>
            Нет прав на изменение{canViewDebug ? " (USERS:*:UPDATE)" : ""}
          </span>
        ) : null}
        {canUpdate && !canBrowsePermissions ? (
          <span className={styles.hint}>
            Нет прав для просмотра системных прав
            {canViewDebug ? " (USERS:*:LIST_SYSTEM_PERMISSIONS)" : ""}
          </span>
        ) : null}
      </div>
    );
  }
}

function UserCases({ id }: { id: number }) {
  const client = useClient((state) => state.client);
  const [cases, casesSet] = useState(null as UserCaseDto[] | null);

  useEffect(() => {
    client.users
      .getUserCases(Number(id))
      .then((r) => casesSet(r.data))
      .catch((e) => console.error(e));
  }, []);

  if (!cases) {
    return <span>Loading...</span>;
  } else {
    return (
      <table className={styles.table}>
        <thead>
          <tr className={styles.row}>
            <th>Title</th>
            <th>Position</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {cases.map((caze) => (
            <tr className={styles.row} key={caze.idRelatedCaseId}>
              <td>
                <Link to={`/cases/${caze.idRelatedCaseId}`}>{caze.idRelatedCaseTitle}</Link>
              </td>
              <td>{caze.position}</td>
              <td>{caze.status.toString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }
}

function Info({ id }: { id: number }) {
  const client = useClient((state) => state.client);
  const [user, userSet] = useState(null as UserDto | null);
  const permissionsState = usePermissions((state) => state.permissions);
  const canViewDebug = permissionsState ? hasPermission("LOGS:*:READ") : false;
  const [latitude, latitudeSet] = useState<string>("");
  const [longitude, longitudeSet] = useState<string>("");
  const [altitude, altitudeSet] = useState<string>("");
  const [saving, savingSet] = useState(false);
  const [error, errorSet] = useState<string | null>(null);

  useEffect(() => {
    client.users
      .getUser(Number(id))
      .then((r) => {
        userSet(r.data);
        latitudeSet(r.data.locationLatitude?.toString() ?? "");
        longitudeSet(r.data.locationLongitude?.toString() ?? "");
        altitudeSet(r.data.locationAltitude?.toString() ?? "");
      })
      .catch((e) => console.error(e));
  }, [client]);

  const canUpdateLocation =
    !!permissionsState &&
    (hasPermission("USERS:*:UPDATE") || hasPermission(`USERS:${id}:UPDATE`));

  function saveLocation() {
    if (!user || !canUpdateLocation) return;
    savingSet(true);
    errorSet(null);
    const payload = {
      latitude: latitude.trim() === "" ? null : Number(latitude),
      longitude: longitude.trim() === "" ? null : Number(longitude),
      altitude: altitude.trim() === "" ? null : Number(altitude),
    };
    client.users
      .updateUserLocation(user.id, payload)
      .then((r) => {
        userSet(r.data);
        latitudeSet(r.data.locationLatitude?.toString() ?? "");
        longitudeSet(r.data.locationLongitude?.toString() ?? "");
        altitudeSet(r.data.locationAltitude?.toString() ?? "");
      })
      .catch((e) => {
        console.error(e);
        const msg =
          typeof e?.error === "string"
            ? e.error
            : e?.error?.message || e?.message || "Unknown error";
        errorSet(msg);
      })
      .finally(() => savingSet(false));
  }

  if (!user) {
    return <span>Loading...</span>;
  } else {
    return (
      <>
        <div>
          <h3>Username</h3>
          <span>{user.username}</span>
        </div>
        <div>
          <h3>Name</h3>
          <span>
            {user.name} {user.surname} {user.patronymic}
          </span>
        </div>
        <div>
          <h3>Primary Position</h3>
          <span>{user.primaryPosition ?? "Not Specified"}</span>
        </div>
        <div>
          <h3>Location</h3>
          <div className={styles.locationRow}>
            <input
              type="number"
              step="any"
              placeholder="Latitude"
              value={latitude}
              onChange={(e) => latitudeSet(e.target.value)}
              disabled={!canUpdateLocation}
            />
            <input
              type="number"
              step="any"
              placeholder="Longitude"
              value={longitude}
              onChange={(e) => longitudeSet(e.target.value)}
              disabled={!canUpdateLocation}
            />
            <input
              type="number"
              step="any"
              placeholder="Altitude"
              value={altitude}
              onChange={(e) => altitudeSet(e.target.value)}
              disabled={!canUpdateLocation}
            />
            <button onClick={saveLocation} disabled={!canUpdateLocation || saving}>
              Save
            </button>
          </div>
          {!canUpdateLocation ? (
            <span className={styles.hint}>
              Нет прав
              {canViewDebug ? ` (USERS:*:UPDATE или USERS:${id}:UPDATE)` : ""}
            </span>
          ) : null}
          {error ? <span className={styles.hint}>{error}</span> : null}
        </div>
        <div>
          <h3>Permissions</h3>
          <UserPermissions id={id} />
        </div>
        <div>
          <h3>Cases</h3>
          <UserCases id={id} />
        </div>
      </>
    );
  }
}

export default function UserPage({ params }: Route.ComponentProps) {
  return (
    <div className={styles.root}>
      <Info id={Number(params.id)} />
    </div>
  );
}
