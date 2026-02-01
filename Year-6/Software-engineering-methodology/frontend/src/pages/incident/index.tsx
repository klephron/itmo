import { Route } from "~react-router/src/pages/incident/+types";
import styles from "./index.module.scss";
import { useClient } from "@state/client";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { IncidentDto, UserDto } from "@api/gen/data-contracts";
import { Link, useNavigate } from "react-router";
import { catchResponse } from "@util";
import { hasPermission, usePermissions } from "@state/permissions";
import { useUser } from "@state/user";
import { formatUtc } from "@util";
import { PermissionRequestHint } from "@components/permission-request";

function Info({ id }: { id: number }) {
  const client = useClient((state) => state.client);
  const navigate = useNavigate();
  const permissionSet = usePermissions((state) => state.permissions);
  const currentUser = useUser((state) => state.user);
  const [incident, incidentSet] = useState(null as IncidentDto | null);
  const [creator, creatorSet] = useState(null as UserDto | null);
  const [updator, updatorSet] = useState(null as UserDto | null);
  const [type, typeSet] = useState("");
  const [reason, reasonSet] = useState("");
  const [locationLatitude, locationLatitudeSet] = useState("");
  const [locationLongitude, locationLongitudeSet] = useState("");
  const [locationAltitude, locationAltitudeSet] = useState("");
  const [applicant, applicantSet] = useState("");
  const [estimatedTime, estimatedTimeSet] = useState("");
  const [waitForAssignment, waitForAssignmentSet] = useState(false);

  const updatePayload = useMemo(() => {
    return {
      applicant: applicant || null,
      type: type || null,
      reason: reason || null,
      locationLatitude: locationLatitude === "" ? null : Number(locationLatitude),
      locationLongitude: locationLongitude === "" ? null : Number(locationLongitude),
      locationAltitude: locationAltitude === "" ? null : Number(locationAltitude),
      estimatedTime: estimatedTime || null,
      waitForAssignment: waitForAssignment,
    };
  }, [
    applicant,
    estimatedTime,
    locationAltitude,
    locationLatitude,
    locationLongitude,
    reason,
    type,
    waitForAssignment,
  ]);

  const ownerId = incident?.creatorId;
  const canUpdate =
    !!permissionSet && incident ? hasPermission(`INCIDENTS:${incident.id}:UPDATE`, ownerId) : false;
  const canDelete =
    !!permissionSet && incident ? hasPermission(`INCIDENTS:${incident.id}:DELETE`, ownerId) : false;
  const canCreateCase =
    !!permissionSet && incident ? hasPermission(`INCIDENTS:${incident.id}:UPDATE`, ownerId) : false;
  const canViewDebug = !!permissionSet && hasPermission("LOGS:*:READ");

  useEffect(() => {
    client.incidents
      .getIncident(id)
      .then((r) => incidentSet(r.data))
      .catch((e) => console.error(e));
  }, [client, id]);

  useEffect(() => {
    if (!incident) {
      creatorSet(null);
      return;
    }
    client.users
      .getUser(incident.creatorId)
      .then((r) => creatorSet(r.data))
      .catch(console.error);
  }, [incident]);

  useEffect(() => {
    if (!incident) {
      updatorSet(null);
      return;
    }
    client.users
      .getUser(incident.updatorId)
      .then((r) => updatorSet(r.data))
      .catch(console.error);
  }, [incident]);

  useEffect(() => {
    if (!incident) {
      return;
    }
    typeSet(incident.type);
    reasonSet(incident.reason);
    locationLatitudeSet(String(incident.locationLatitude));
    locationLongitudeSet(String(incident.locationLongitude));
    locationAltitudeSet(String(incident.locationAltitude));
    applicantSet(incident.applicant ?? "");
    estimatedTimeSet(incident.estimatedTime);
    waitForAssignmentSet(incident.waitForAssignment);
  }, [incident]);

  function handleUpdate(e: FormEvent) {
    e.preventDefault();
    if (!incident || !canUpdate) {
      return;
    }

    client.incidents
      .updateIncident(incident.id, updatePayload)
      .then(() => client.incidents.getIncident(incident.id))
      .then((r) => incidentSet(r.data))
      .catch(catchResponse);
  }

  function handleDelete() {
    if (!incident || !canDelete) {
      return;
    }
    if (!confirm("Delete this incident?")) {
      return;
    }

    client.incidents
      .deleteIncident(incident.id)
      .then(() => navigate("/incidents"))
      .catch(catchResponse);
  }

  if (!incident) {
    return <span>Loading...</span>;
  } else {
    return (
      <>
        <h2>Incident ({incident.reason})</h2>
        <div>
          <h3>Type</h3>
          <span>{incident.type}</span>
        </div>
        <div>
          <h3>Creator</h3>
          <span>
            {creator ? (
              <Link to={`/users/${creator.id}`}>
                {creator.name + " " + creator.surname + " " + creator.patronymic}
              </Link>
            ) : (
              "Loading..."
            )}
          </span>
        </div>
        <div>
          <h3>Created At (UTC)</h3>
          <span>{formatUtc(incident.createdAt)}</span>
        </div>
        <div>
          <h3>Updator</h3>
          <span>
            {updator ? (
              <Link to={`/users/${updator.id}`}>
                {updator.name + " " + updator.surname + " " + updator.patronymic}
              </Link>
            ) : (
              "Loading..."
            )}
          </span>
        </div>
        <div>
          <h3>Updated At (UTC)</h3>
          <span>{formatUtc(incident.updatedAt)}</span>
        </div>
        <div>
          <h3>Case</h3>
          {incident.relatedCaseId ? (
            <Link to={`/cases/${incident.relatedCaseId}`}>Follow</Link>
          ) : canCreateCase ? (
            <Link to={`/cases/create?incident_id=${incident.id}`}>Create</Link>
          ) : (
            <PermissionRequestHint
              permission={`INCIDENTS:${incident.id}:UPDATE`}
              message="No permission"
              showDebug={canViewDebug}
              reason={`incident ${incident.id}`}
            />
          )}
        </div>
        <div>
          <h3>Incident Time</h3>
          <span>{formatUtc(incident.estimatedTime)}</span>
        </div>
        <div>
          <h3>Wait For Assignment</h3>
          <span>{incident.waitForAssignment ? "Yes" : "No"}</span>
        </div>
        <div>
          <h3>Location</h3>
          <span>
            {incident.locationLatitude}, {incident.locationLongitude}, {incident.locationAltitude}
          </span>
        </div>
        <div>
          <h3>Reason</h3>
          <span>{incident.reason}</span>
        </div>
        {canViewDebug ? (
          <div className={styles.debug}>
            incidentId: {incident.id}
            {"\n"}ownerId: {incident.creatorId}
            {"\n"}currentUserId: {currentUser.present ? currentUser.id : "n/a"}
            {"\n"}permissions loaded: {String(!!permissionSet)}
            {"\n"}perm pattern: INCIDENTS:{incident.id}:UPDATE
          </div>
        ) : null}
        {canUpdate || canDelete ? (
          <div className={styles.formCard}>
            <h3>Update Incident</h3>
            <form className={styles.form} onSubmit={handleUpdate}>
              <label>
                Reason
                <textarea value={reason} onChange={(e) => reasonSet(e.target.value)} />
              </label>
              <label>
                Type
                <input value={type} onChange={(e) => typeSet(e.target.value)} />
              </label>
              <label>
                Location Latitude
                <input
                  value={locationLatitude}
                  onChange={(e) => locationLatitudeSet(e.target.value)}
                />
              </label>
              <label>
                Location Longitude
                <input
                  value={locationLongitude}
                  onChange={(e) => locationLongitudeSet(e.target.value)}
                />
              </label>
              <label>
                Location Altitude
                <input
                  value={locationAltitude}
                  onChange={(e) => locationAltitudeSet(e.target.value)}
                />
              </label>
              <label>
                Applicant
                <input value={applicant} onChange={(e) => applicantSet(e.target.value)} />
              </label>
              <label>
                Estimated Time (UTC)
                <input value={estimatedTime} onChange={(e) => estimatedTimeSet(e.target.value)} />
              </label>
              <label className={styles.inline}>
                <input
                  type="checkbox"
                  checked={waitForAssignment}
                  onChange={(e) => waitForAssignmentSet(e.target.checked)}
                />
                Wait For Assignment
              </label>
              <div className={styles.actions}>
                <button type="submit" disabled={!canUpdate}>
                  Save
                </button>
                <button
                  className={styles.danger}
                  type="button"
                  onClick={handleDelete}
                  disabled={!canDelete}
                >
                  Delete
                </button>
              </div>
            </form>
            {!canUpdate ? (
              <PermissionRequestHint
                permission={`INCIDENTS:${incident.id}:UPDATE`}
                message="No permission to update"
                showDebug={canViewDebug}
                reason={`incident ${incident.id}`}
              />
            ) : null}
            {!canDelete ? (
              <PermissionRequestHint
                permission={`INCIDENTS:${incident.id}:DELETE`}
                message="No permission to delete"
                showDebug={canViewDebug}
                reason={`incident ${incident.id}`}
              />
            ) : null}
          </div>
        ) : null}
      </>
    );
  }
}

export default function IncidentPage({ params }: Route.ComponentProps) {
  return (
    <div className={styles.root}>
      <Info id={Number(params.id)} />
    </div>
  );
}
