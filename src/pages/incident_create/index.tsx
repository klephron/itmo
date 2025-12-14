import { useClient } from "@state/client";
import styles from "./index.module.scss";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { catchResponse } from "@util";
import { hasPermission, usePermissions } from "@state/permissions";
import {
  RankedUserInfoResponse,
  UserInfoResponse,
} from "@api/gen/data-contracts";

export default function IncidentsCreatePage() {
  const navigate = useNavigate();
  const client = useClient((state) => state.client);
  const permissionsState = usePermissions((state) => state.permissions);
  const canCreate = permissionsState ? hasPermission("INCIDENTS:*:ADD") : false;
  const canViewDebug = permissionsState ? hasPermission("LOGS:*:READ") : false;
  const [type, typeSet] = useState("");
  const [reason, reasonSet] = useState("");
  const [locationLatitude, locationLatitudeSet] = useState("");
  const [locationLongitude, locationLongitudeSet] = useState("");
  const [locationAltitude, locationAltitudeSet] = useState("");
  const [applicant, applicantSet] = useState("");
  const [waitForAssignment, waitForAssignmentSet] = useState(true);
  const [positions, positionsSet] = useState<string[]>([]);
  const [requiredPosition, requiredPositionSet] = useState("");
  const [ranked, rankedSet] = useState<RankedUserInfoResponse[] | null>(null);
  const [rankedPositions, rankedPositionsSet] = useState<Record<string, string>>({});
  const [caseUsers, caseUsersSet] = useState<Record<string, string>>({});
  const [userOptions, userOptionsSet] = useState<UserInfoResponse[]>([]);
  const [loadError, loadErrorSet] = useState<string | null>(null);
  const [creating, creatingSet] = useState(false);
  const [manualUserId, manualUserIdSet] = useState("");
  const [manualPosition, manualPositionSet] = useState("");
  const [caseTitle, caseTitleSet] = useState("");
  const [caseDescription, caseDescriptionSet] = useState("");
  const [createError, createErrorSet] = useState<string | null>(null);

  useEffect(() => {
    client.users
      .getPrimaryPositions()
      .then((r) => {
        const normalized = r.data.map((p: any) => (typeof p === "string" ? p : p.value ?? p.label ?? String(p)));
        positionsSet(normalized);
      })
      .catch(console.error);

    client.users
      .getUsers()
      .then((r) => userOptionsSet(r.data))
      .catch(console.error);
  }, [client]);

  const canRank = useMemo(() => {
    return locationLatitude.trim() !== "" && locationLongitude.trim() !== "" && locationAltitude.trim() !== "";
  }, [locationLatitude, locationLongitude, locationAltitude]);

  function loadRanked() {
    if (!canRank) {
      loadErrorSet("Specify coordinates to rank users");
      return;
    }
    const lat = Number(locationLatitude);
    const lon = Number(locationLongitude);
    const alt = Number(locationAltitude);
    if (Number.isNaN(lat) || Number.isNaN(lon) || Number.isNaN(alt)) {
      loadErrorSet("Coordinates must be numbers");
      return;
    }
    loadErrorSet(null);
    client.users
      .getRankedUsers({
        latitude: lat,
        longitude: lon,
        altitude: alt,
        primaryPosition: requiredPosition || null,
      })
      .then((r) => {
        rankedSet(r.data);
        // подгружаем должности для отображения
        const idsToLoad = r.data.map((u) => u.user.id);
        Promise.all(
          idsToLoad.map((id) =>
            client.users
              .getUser(id)
              .then((res) => ({ id, position: res.data.primaryPosition ?? "" }))
              .catch(() => ({ id, position: "" }))
          )
        ).then((list) => {
          const map: Record<string, string> = {};
          list.forEach((it) => {
            map[String(it.id)] = it.position;
          });
          rankedPositionsSet(map);
          caseUsersSet((prev) => {
            const next = { ...prev } as Record<string, string>;
            r.data.forEach((u) => {
              const key = String(u.user.id);
              if (!next[key]) {
                next[key] = map[key] || requiredPosition || positions[0] || "ASSIGNEE";
              } else if (!next[key] && map[key]) {
                next[key] = map[key];
              }
            });
            return next;
          });
        });
        // Предзаполним участников (не перетираем ручные добавления)
      })
      .catch((e) => {
        console.error(e);
        loadErrorSet("Failed to load ranking");
      });
  }

  function toggleUser(userId: number, include: boolean) {
    const next = { ...caseUsers } as Record<string, string>;
    if (include) {
      const key = String(userId);
      next[key] = next[key] || rankedPositions[key] || requiredPosition || positions[0] || "ASSIGNEE";
    } else {
      delete next[String(userId)];
    }
    caseUsersSet(next);
  }

  function updateUserPosition(userId: number, position: string) {
    const next = { ...caseUsers } as Record<string, string>;
    next[String(userId)] = position;
    caseUsersSet(next);
  }

  function addManualUser(userId: string, position: string) {
    if (!userId || !position) return;
    const next = { ...caseUsers } as Record<string, string>;
    next[userId] = position;
    caseUsersSet(next);
    manualUserIdSet("");
    manualPositionSet("");
  }

  async function createIncident() {
    if (!canCreate) {
      createErrorSet(`No permission to create incident${canViewDebug ? " (INCIDENTS:*:ADD)" : ""}`);
      return;
    }
    if (!type.trim() || !reason.trim()) {
      createErrorSet("Type and Reason are required");
      return;
    }
    const lat = Number(locationLatitude);
    const lon = Number(locationLongitude);
    const alt = Number(locationAltitude);
    if (Number.isNaN(lat) || Number.isNaN(lon) || Number.isNaN(alt)) {
      createErrorSet("Coordinates must be numbers");
      return;
    }
    creatingSet(true);
    createErrorSet(null);
    try {
      const incidentResp = await client.incidents.addIncident({
        type: type,
        locationLatitude: lat,
        locationLongitude: lon,
        locationAltitude: alt,
        reason: reason,
        estimatedTime: new Date().toISOString(),
        applicant: applicant ?? undefined,
        waitForAssignment: waitForAssignment,
      });

      if (waitForAssignment) {
        navigate(`/incidents/${incidentResp.data.id}`);
        return;
      }

      const users: Record<string, string> = {};
      Object.entries(caseUsers).forEach(([id, pos]) => {
        if (pos) users[id] = pos;
      });

      try {
        const res = await client.incidents.addCaseForIncident(incidentResp.data.id, {
          title: caseTitle || reason || type || `Case for incident ${incidentResp.data.id}`,
          description: caseDescription || reason,
          users,
        });
        navigate(`/cases/${res.data.id}`);
      } catch (e: any) {
        const extractCaseError = (err: any): string => {
          let msg: string | null = null;
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

          if (!msg && err?.error?._embedded?.errors) {
            msg = err.error._embedded.errors.map((ev: any) => ev.message).join("\n");
          }

          if (!msg && err?.error?.message) {
            msg = err.error.message;
          } else if (!msg && err?.message) {
            msg = err.message;
          }

          return msg || "Failed to create case";
        };

        // rollback incident so что не осталось “пустого” инцидента без кейса
        try {
          await client.incidents.deleteIncident(incidentResp.data.id);
        } catch (cleanupErr) {
          console.error("Failed to rollback incident after case error", cleanupErr);
        }

        const msg = extractCaseError(e);
        console.error("[incident-create] failed to create case for incident", {
          incidentId: incidentResp.data.id,
          error: e,
          message: msg,
        });
        createErrorSet(msg);
      }
    } catch (e) {
      const msg =
        (e as any)?.error?._embedded?.errors?.map((er: any) => er.message).join("\n") ||
        (e as any)?.error?.message ||
        (e as any)?.message ||
        "Failed to create incident";
      createErrorSet(msg);
    }
    creatingSet(false);
  }

  return (
    <div className={styles.root}>
      <h2>Create Incident</h2>
      {!canCreate ? (
        <span>Insufficient permissions{canViewDebug ? " (INCIDENTS:*:ADD)" : ""}</span>
      ) : null}
      <form>
        <div>
          <h3>Reason</h3>
          <textarea value={reason} onChange={(e) => reasonSet(e.target.value)} />
        </div>
        <div>
          <h3>Type</h3>
          <input type="input" value={type} onChange={(e) => typeSet(e.target.value)} />
        </div>
        <div>
          <h3>Location Latitude</h3>
          <input
            type="input"
            value={locationLatitude}
            onChange={(e) => locationLatitudeSet(e.target.value)}
          />
        </div>
        <div>
          <h3>Location Longitude</h3>
          <input
            type="input"
            value={locationLongitude}
            onChange={(e) => locationLongitudeSet(e.target.value)}
          />
        </div>
        <div>
          <h3>Location Altitude</h3>
          <input
            type="input"
            value={locationAltitude}
            onChange={(e) => locationAltitudeSet(e.target.value)}
          />
        </div>
        <div>
          <h3>Applicant</h3>
          <input type="input" value={applicant} onChange={(e) => applicantSet(e.target.value)} />
        </div>
        <div>
          <h3>Required position (for ranking)</h3>
          <select value={requiredPosition} onChange={(e) => requiredPositionSet(e.target.value)}>
            <option value="">Any</option>
            {positions.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
        </div>
        <div className={styles.row}>
          <input
            id="waitForAssignment"
            type="checkbox"
            checked={waitForAssignment}
            onChange={(e) => waitForAssignmentSet(e.target.checked)}
          />
          <label htmlFor="waitForAssignment">Wait For Assignment</label>
          {waitForAssignment ? (
            <span className={styles.hint}>If enabled, the case will not be created automatically</span>
          ) : (
            <span className={styles.small}>Case will be created with selected participants</span>
          )}
        </div>
        {!waitForAssignment ? (
          <div className={styles.caseBlock}>
            <h3>Case details</h3>
            <label>
              Title
              <input
                type="text"
                value={caseTitle}
                placeholder={reason || type || "Case title"}
                onChange={(e) => caseTitleSet(e.target.value)}
              />
            </label>
            <label>
              Description
              <textarea
                value={caseDescription}
                placeholder={reason || "Case description"}
                onChange={(e) => caseDescriptionSet(e.target.value)}
              />
            </label>
          </div>
        ) : null}
        {!waitForAssignment ? (
          <div className={styles.rankBlock}>
            <div className={styles.rankActions}>
              <button type="button" onClick={loadRanked} disabled={!canRank}>
                Load ranked
              </button>
              {loadError ? <span className={styles.hint}>{loadError}</span> : null}
            </div>
            {ranked ? (
              <table className={styles.table}>
                <thead>
                  <tr className={styles.row}>
                    <th>Select</th>
                    <th>User</th>
                    <th>Position</th>
                    <th>Score</th>
                    <th>Distance (m)</th>
                    <th>Active cases</th>
                    <th>Matches</th>
                  </tr>
                </thead>
                <tbody>
                  {ranked.map((u) => {
                  const key = String(u.user.id);
                  const included = caseUsers[key] !== undefined;
                  return (
                    <tr className={styles.row} key={u.user.id}>
                      <td>
                          <input
                            type="checkbox"
                            checked={included}
                            onChange={(e) => toggleUser(u.user.id, e.target.checked)}
                          />
                        </td>
                        <td>
                        {u.user.username} ({u.user.id})
                      </td>
                      <td>
                        {rankedPositions[key] || "—"}
                      </td>
                        <td>{u.score.toFixed(2)}</td>
                        <td>{u.distanceMeters.toFixed(1)}</td>
                        <td>{u.activeCases}</td>
                        <td>{u.positionMatches ? "✓" : ""}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            ) : null}

            <div className={styles.manualAdd}>
              <h4>Add manually</h4>
              <div className={styles.addRow}>
                <select
                  value={manualUserId}
                  onChange={(e) => {
                    manualUserIdSet(e.target.value);
                  }}
                >
                  <option value="">Select user</option>
                  {userOptions.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.username} ({u.id})
                    </option>
                  ))}
                </select>
                <select
                  value={manualPosition}
                  onChange={(e) => manualPositionSet(e.target.value)}
                >
                  <option value="">Select position</option>
                  {positions.map((p) => (
                    <option key={p} value={p}>
                      {p}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  onClick={() => {
                    addManualUser(manualUserId, manualPosition || requiredPosition || positions[0] || "");
                  }}
                >
                  Add
                </button>
              </div>
              {Object.keys(caseUsers).length ? (
                <div className={styles.small}>Selected participants: {Object.keys(caseUsers).length}</div>
              ) : null}
              {Object.keys(caseUsers).length ? (
                <table className={styles.table}>
                  <thead>
                    <tr className={styles.row}>
                      <th>User</th>
                      <th>Position</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {Object.entries(caseUsers).map(([id, pos]) => {
                      const user = userOptions.find((u) => String(u.id) === String(id));
                      return (
                        <tr className={styles.row} key={id}>
                          <td>{user ? `${user.username} (${user.id})` : id}</td>
                          <td>
                            <select
                              value={pos}
                              onChange={(e) => updateUserPosition(Number(id), e.target.value)}
                            >
                              <option value="">Select position</option>
                              {positions.map((p) => (
                                <option key={p} value={p}>
                                  {p}
                                </option>
                              ))}
                            </select>
                          </td>
                          <td>
                            <button type="button" onClick={() => toggleUser(Number(id), false)}>
                              Remove
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              ) : null}
            </div>
          </div>
        ) : null}
        <button
          onClick={(e) => {
            e.preventDefault();
            createIncident();
          }}
          disabled={!canCreate || creating}
        >
          Submit
        </button>
        {createError ? <div className={styles.hint}>{createError}</div> : null}
      </form>
    </div>
  );
}
