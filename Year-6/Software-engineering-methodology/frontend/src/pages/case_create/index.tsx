import styles from "./index.module.scss";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { useClient } from "@state/client";
import { catchResponse } from "@util";
import { IncidentDto, RankedUserInfoResponse, UserInfoResponse } from "@api/gen/data-contracts";
import Checkbox from "@components/checkbox";
import { hasPermission, usePermissions } from "@state/permissions";

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

  position: string;
  selected: boolean;
};

function mainToMap(users: UserInfoResponse[]) {
  return new Map(
    users.map((main) => [
      main.id,
      { id: main.id, username: main.username, position: "", selected: false } satisfies UserEntry,
    ])
  );
}

/**
 * Calculate 3D distance between two geographic points
 * @param from - Source location with lat/lng/altitude(meters)
 * @param to - Destination location with lat/lng/altitude(meters)
 * @returns Distance in meters, or Number.MAX_VALUE if coordinates are missing
 */
function distance(from: IncidentDto | null, to: UserEntry): number {
  const R = 6371000; // Earth's radius in meters
  const toRad = (value: number) => (value * Math.PI) / 180;

  if (
    from === null ||
    from.locationLatitude === undefined ||
    from.locationLongitude === undefined ||
    to.locationLatitude === undefined ||
    to.locationLongitude === undefined
  ) {
    return Number.MAX_VALUE;
  }

  const lat1 = from.locationLatitude;
  const lon1 = from.locationLongitude;
  const lat2 = to.locationLatitude;
  const lon2 = to.locationLongitude;

  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  let dist = R * c;

  if (from.locationAltitude !== undefined && to.locationAltitude !== undefined) {
    const altDiff = Math.abs(from.locationAltitude - to.locationAltitude);
    dist = Math.sqrt(dist * dist + altDiff * altDiff);
  }

  return dist;
}

function distanceSort(incident: IncidentDto | null) {
  return (a: UserEntry, b: UserEntry) => {
    return distance(incident, a) - distance(incident, b);
  };
}

export default function CaseCreatePage() {
  const navigate = useNavigate();
  const [search, _searchSet] = useSearchParams();
  const client = useClient((state) => state.client);
  const [incidentId, _incidentIdSet] = useState(search.get("incident_id"));
  const [title, titleSet] = useState("");
  const [description, descriptionSet] = useState("");
  const [users, usersSet] = useState(null as Map<number, UserEntry> | null);
  const [usersLoaded, usersLoadedSet] = useState(false);
  const [incident, incidentSet] = useState(null as IncidentDto | null);
  const permissionsState = usePermissions((state) => state.permissions);
  const [canCreate, canCreateSet] = useState(false);
  const [positions, positionsSet] = useState<string[]>([]);
  const [createError, createErrorSet] = useState<string | null>(null);
  const [ranked, rankedSet] = useState<RankedUserInfoResponse[] | null>(null);
  const [rankedInfo, rankedInfoSet] = useState<
    Record<string, { score: number; distance: number; activeCases: number; positionMatches: boolean }>
  >({});
  const [rankedLoaded, rankedLoadedSet] = useState(false);

  useEffect(() => {
    client.incidents
      .getIncident(Number(incidentId))
      .then((r) => {
        incidentSet(r.data);
        const ownerId = r.data.creatorId;
        const allowed = permissionsState
          ? hasPermission(`INCIDENTS:${r.data.id}:UPDATE`, ownerId)
          : false;
        canCreateSet(allowed);
      })
      .catch(console.error);
  }, [incidentId, permissionsState]);

  useEffect(() => {
    client.users
      .getUsers()
      .then((r) => {
        usersSet(mainToMap(r.data));
        usersLoadedSet(false);
      })
      .catch(console.error);
  }, [client]);

  useEffect(() => {
    client.users
      .getPrimaryPositions()
      .then((r) => {
        const normalized = r.data.map((p: any) => (typeof p === "string" ? p : p.value ?? p.label ?? String(p)));
        positionsSet(normalized);
      })
      .catch(console.error);
  }, [client]);

  useEffect(() => {
    async function loadRanked() {
      if (!incident || !users || !usersLoaded || rankedLoaded) return;
      try {
        const res = await client.users.getRankedUsers({
          latitude: incident.locationLatitude,
          longitude: incident.locationLongitude,
          altitude: incident.locationAltitude,
          primaryPosition: null,
        });
        rankedSet(res.data);
        const info: Record<string, { score: number; distance: number; activeCases: number; positionMatches: boolean }> =
          {};
        res.data.forEach((r) => {
          info[String(r.user.id)] = {
            score: r.score,
            distance: r.distanceMeters,
            activeCases: r.activeCases,
            positionMatches: !!r.positionMatches,
          };
        });
        rankedInfoSet(info);
        const next = new Map(users);
        res.data.forEach((r) => {
          const existing = next.get(r.user.id);
          const defaultPos = existing?.position || r.user.primaryPosition || positions[0] || "";
          next.set(r.user.id, {
            id: r.user.id,
            username: existing?.username || r.user.username,
            name: existing?.name || r.user.name,
            surname: existing?.surname || r.user.surname,
            patronymic: existing?.patronymic ?? r.user.patronymic ?? undefined,
            primaryPosition: existing?.primaryPosition ?? r.user.primaryPosition ?? undefined,
            locationLatitude: existing?.locationLatitude ?? r.user.locationLatitude,
            locationLongitude: existing?.locationLongitude ?? r.user.locationLongitude,
            locationAltitude: existing?.locationAltitude ?? r.user.locationAltitude,
            position: defaultPos,
            selected: existing?.selected ?? false,
          });
        });
        usersSet(next);
        rankedLoadedSet(true);
      } catch (e) {
        console.error("load ranked users failed", e);
      }
    }
    loadRanked();
  }, [client, incident, users, usersLoaded, rankedLoaded, positions]);

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
              position: user.position,
              selected: user.selected,
            });
          })
          .catch(console.error);
      }
      usersLoadedSet(true);
      usersSet(users1);
    })();
  }, [users]);

  async function createCase() {
    createErrorSet(null);

    const extractError = (err: any): string => {
      let msg: string | null = null;
      // 1. Plain strings first
      if (typeof err === "string") {
        msg = err;
      } else if (typeof err?.error === "string") {
        msg = err.error;
      } else if (typeof err?.data === "string") {
        msg = err.data;
      } else if (typeof err?.error?.data === "string") {
        msg = err.error.data;
      } else if (typeof err?.rawBody === "string") {
        msg = err.rawBody;
      } else if (typeof err?.error?.rawBody === "string") {
        msg = err.error.rawBody;
      }

      // 2. Micronaut _embedded errors
      if (!msg && err?.error?._embedded?.errors) {
        msg = err.error._embedded.errors.map((ev: any) => ev.message).join("\n");
      }

      // 3. message fields
      if (!msg && err?.error?.message) {
        msg = err.error.message;
      } else if (!msg && err?.message) {
        msg = err.message;
      }

      return msg || "Failed to create case";
    };
    const usersRecord = {} as Record<string, string>;
    if (users) {
      for (const user of users.values()) {
        if (user.selected) {
          usersRecord[String(user.id)] = user.position;
        }
      }
    }

    if (!canCreate) {
      createErrorSet(`No permission to create a case (INCIDENTS:${incidentId}:UPDATE)`);
      return;
    }
    if (!title.trim() || !description.trim()) {
      createErrorSet("Title and Description are required");
      return;
    }

    try {
      const res = await client.incidents.addCaseForIncident(Number(incidentId), {
        title: title,
        description: description,
        users: usersRecord,
      });
      navigate(`/cases/${res.data.id}`);
    } catch (e: any) {
      const msg = extractError(e);
      console.error("[case-create] failed to create case", { error: e, message: msg });
      createErrorSet(msg);
    }
  }

  return (
    <div className={styles.root}>
      <h2>Create Case</h2>
      {!canCreate ? (
        <span>No permission (needs INCIDENTS:{incidentId}:UPDATE or own incident)</span>
      ) : null}
      <form>
        <div>
          <h3>Incident</h3>
          <input
            type="input"
            value={incident ? `${incident.reason} (${incident.type})` : "Loading..."}
            disabled
          />
        </div>
        <div>
          <h3>Title</h3>
          <input type="input" value={title} onChange={(e) => titleSet(e.target.value)} />
        </div>
        <div>
          <h3>Description</h3>
          <textarea value={description} onChange={(e) => descriptionSet(e.target.value)} />
        </div>
        <div>
          <h3>Users</h3>
          <table className={styles.table}>
            <thead>
              <tr className={styles.row}>
                <th>Select</th>
                <th>ID</th>
                <th>Position</th>
                <th>Username</th>
                <th>Name</th>
                <th>Primary Position</th>
                <th>Location</th>
                <th>Distance (meters)</th>
                <th>Active cases</th>
              </tr>
            </thead>
            <tbody>
              {(ranked && users
                ? ranked
                    .map((r) => users.get(r.user.id))
                    .filter((u): u is UserEntry => !!u)
                : Array.from(users && incident ? users.values() : []).sort(distanceSort(incident))
              ).map((user) => (
                <tr className={styles.row} key={user.id}>
                  <td>
                    <Checkbox
                      isChecked={user.selected}
                      onChange={(e) => {
                        user.selected = e;
                        usersSet(new Map(users));
                      }}
                    />
                  </td>
                  <td>{user.id}</td>
                  <td>
                    <select
                      value={user.position}
                      onChange={(e) => {
                        user.position = e.target.value;
                        usersSet(new Map(users));
                      }}
                    >
                      <option value="">Select position</option>
                      {positions.map((p) => (
                        <option key={p} value={p}>
                          {p}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>{user.username}</td>
                  <td>{`${user.name} ${user.surname} ${user.patronymic}`}</td>
                  <td>{user.primaryPosition ?? "Not Specified"}</td>
                  <td>{`${user.locationLatitude}, ${user.locationLongitude}, ${user.locationAltitude}`}</td>
                  <td>
                    {(rankedInfo[String(user.id)]?.distance ?? distance(incident, user)).toLocaleString(
                      undefined,
                      {
                        maximumFractionDigits: 0,
                      }
                    )}
                  </td>
                  <td>{rankedInfo[String(user.id)]?.activeCases ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <button
          onClick={(e) => {
            e.preventDefault();
            createCase();
          }}
          disabled={!canCreate}
        >
          Submit
        </button>
        {createError ? <div className={styles.hint}>{createError}</div> : null}
      </form>
    </div>
  );
}
