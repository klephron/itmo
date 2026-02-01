import { useEffect, useState } from "react";
import styles from "./index.module.scss";
import { useClient } from "@state/client";
import { IncidentMainInfoDto } from "@api/gen/data-contracts";
import { Link, useNavigate } from "react-router";
import { hasPermission, usePermissions } from "@state/permissions";
import { formatUtc } from "@util";
import { PermissionRequestHint } from "@components/permission-request";

type IncidentEntry = {
  id: number;
  type: string;
  reason: string;
  locationLatitude: number;
  locationLongitude: number;
  locationAltitude: number;
  estimatedTime: string;
  createdAt?: string;
  updatedAt?: string;
  caseId?: number | null;
  waitForAssignment: boolean;
};

function infoToMap(infos: IncidentMainInfoDto[]): Map<number, IncidentEntry> {
  return new Map(
    infos.map((info) => [
      info.id,
      {
        id: info.id,
        type: info.type,
        reason: info.reason,
        locationLatitude: info.locationLatitude,
        locationLongitude: info.locationLongitude,
        locationAltitude: info.locationAltitude,
        estimatedTime: info.estimatedTime,
        waitForAssignment: !!info.waitForAssignment,
        caseId: null,
      } satisfies IncidentEntry,
    ])
  );
}

function Table() {
  const client = useClient((state) => state.client);
  const [incidents, incidentsSet] = useState(null as Map<number, IncidentEntry> | null);
  const [incidentsLoaded, incidentsLoadedSet] = useState(false);

  useEffect(() => {
    client.incidents
      .getIncidents()
      .then((r) => incidentsSet(infoToMap(r.data)))
      .catch(console.error);
  }, [client]);

  useEffect(() => {
    if (!incidents || incidentsLoaded) {
      return;
    }
    (async () => {
      const incidents1 = new Map(incidents);
      for (const incident of incidents1.values()) {
        await client.incidents
          .getIncident(incident.id)
          .then((r) => {
            incidents1.set(incident.id, {
              id: incident.id,
              type: incident.type,
              reason: incident.reason,
              locationLatitude: incident.locationLatitude,
              locationLongitude: incident.locationLongitude,
              locationAltitude: incident.locationAltitude,
              estimatedTime: incident.estimatedTime,
              waitForAssignment: incident.waitForAssignment,
              createdAt: r.data.createdAt,
              updatedAt: r.data.updatedAt,
              caseId: r.data.relatedCaseId ?? undefined,
            } satisfies IncidentEntry);
          })
          .catch(console.error);
      }
      incidentsLoadedSet(true);
      incidentsSet(incidents1);
    })();
  }, [incidents]);

  return (
    <table className={styles.table}>
      <thead>
        <tr className={styles.row}>
          <th>Reason</th>
          <th>Type</th>
          <th>Location</th>
          <th>Time</th>
          <th>Wait For Assignment</th>
          <th>Created At (UTC)</th>
          <th>Updated At (UTC)</th>
          <th>Case</th>
        </tr>
      </thead>
      <tbody>
        {Array.from(incidents ? incidents.values() : [])
          .sort((a, b) => b.id - a.id)
          .map((incident) => (
            <tr className={styles.row} key={incident.id}>
              <td>
                <Link to={`/incidents/${incident.id}`}>{incident.reason}</Link>
              </td>
              <td>{incident.type}</td>
              <td>
                {incident.locationLatitude}, {incident.locationLongitude},{" "}
                {incident.locationAltitude}
              </td>
              <td>{formatUtc(incident.estimatedTime)}</td>
              <td>{incident.waitForAssignment ? "Yes" : "No"}</td>
              <td>{incident.createdAt ? formatUtc(incident.createdAt) : ""}</td>
              <td>{incident.updatedAt ? formatUtc(incident.updatedAt) : ""}</td>
              <td>
                {incident.caseId === null ? (
                  ""
                ) : incident.caseId ? (
                  <Link to={`/cases/${incident.caseId}`}>Follow</Link>
                ) : (
                  <Link to={`/cases/create?incident_id=${incident.id}`}>Create</Link>
                )}
              </td>
            </tr>
          ))}
      </tbody>
    </table>
  );
}

function Menu() {
  const navigate = useNavigate();
  const permissionState = usePermissions((state) => state.permissions);
  const canCreate = permissionState ? hasPermission("INCIDENTS:*:ADD") : false;
  const canViewDebug = permissionState ? hasPermission("LOGS:*:READ") : false;
  return (
    <div className={styles.menu}>
      <button
        onClick={() => {
          navigate("/incidents/create");
        }}
        disabled={!canCreate}
      >
        Create
      </button>
      {!canCreate ? (
        <PermissionRequestHint
          permission="INCIDENTS:*:ADD"
          message="No permission to create incident"
          showDebug={canViewDebug}
          reason="incidents list"
        />
      ) : null}
    </div>
  );
}

export default function IncidentsPage() {
  return (
    <div className={styles.root}>
      <h2>Incidents</h2>
      <Menu />
      <Table />
    </div>
  );
}
