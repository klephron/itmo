import styles from "./index.module.scss";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { useClient } from "@state/client";
import { catchResponse } from "@util";
import { IncidentDto, UserInfoResponse } from "@api/gen/data-contracts";
import Checkbox from "@components/checkbox";

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

  useEffect(() => {
    client.incidents
      .getIncident(Number(incidentId))
      .then((r) => incidentSet(r.data))
      .catch(console.error);
  }, [incidentId]);

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

  function createCase() {
    const usersRecord = {} as Record<string, string>;
    if (users) {
      for (const user of users.values()) {
        if (user.selected) {
          usersRecord[String(user.id)] = user.position;
        }
      }
    }

    try {
      client.incidents
        .addCaseForIncident(Number(incidentId), {
          title: title,
          description: description,
          users: usersRecord,
        })
        .then((r) => {
          navigate(`/cases/${r.data.id}`);
        })
        .catch(catchResponse);
    } catch (e) {
      alert(e);
    }
  }

  return (
    <div className={styles.root}>
      <h2>Create Case</h2>
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
                <th>Position</th>
                <th>Username</th>
                <th>Name</th>
                <th>Primary Position</th>
                <th>Location</th>
                <th>Distance (meters)</th>
              </tr>
            </thead>
            <tbody>
              {Array.from(users && incident ? users.values() : [])
                .sort(distanceSort(incident))
                .map((user) => (
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
                    <td>
                      <input
                        onChange={(e) => {
                          user.position = e.target.value;
                          usersSet(new Map(users));
                        }}
                      />
                    </td>
                    <td>{user.username}</td>
                    <td>{`${user.name} ${user.surname} ${user.patronymic}`}</td>
                    <td>{user.primaryPosition ?? "Not Specified"}</td>
                    <td>{`${user.locationLatitude}, ${user.locationLongitude}, ${user.locationAltitude}`}</td>
                    <td>
                      {distance(incident, user).toLocaleString(undefined, {
                        maximumFractionDigits: 0,
                      })}
                    </td>
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
        >
          Submit
        </button>
      </form>
    </div>
  );
}
