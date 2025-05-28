import { Route } from "~react-router/src/pages/incident/+types";
import styles from "./index.module.scss";
import { useClient } from "@state/client";
import { useEffect, useState } from "react";
import { IncidentDto, UserDto } from "@api/gen/data-contracts";
import { Link } from "react-router";

function Info({ id }: { id: number }) {
  const client = useClient((state) => state.client);
  const [incident, incidentSet] = useState(null as IncidentDto | null);
  const [creator, creatorSet] = useState(null as UserDto | null);
  const [updator, updatorSet] = useState(null as UserDto | null);

  useEffect(() => {
    client.incidents
      .getIncident(id)
      .then((r) => incidentSet(r.data))
      .catch((e) => console.error(e));
  }, [client]);

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

  if (!incident) {
    return <span>Loading...</span>;
  } else {
    return (
      <>
        <div>
          <h2>Incident ({incident.type})</h2>
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
          <span>{new Date(incident.createdAt).toLocaleString()}</span>
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
          <h3>Created At (UTC)</h3>
          <span>{new Date(incident.createdAt).toLocaleString()}</span>
        </div>
        <div>
          <h3>Case</h3>
          {incident.relatedCaseId ? (
            <Link to={`/cases/${incident.relatedCaseId}`}>Follow</Link>
          ) : (
            <Link to={`/cases/create?incident_id=${incident.id}`}>Create</Link>
          )}
        </div>
        <div>
          <h3>Incident Time</h3>
          <span>{new Date(incident.estimatedTime).toLocaleString()}</span>
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
