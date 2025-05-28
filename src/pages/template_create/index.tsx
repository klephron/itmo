import { useClient } from "@state/client";
import styles from "./index.module.scss";
import { useState } from "react";
import { useNavigate } from "react-router";
import { catchResponse } from "@util";

export default function TemplatesCreatePage() {
  const navigate = useNavigate();
  const client = useClient((state) => state.client);
  const [type, typeSet] = useState("");
  const [reason, reasonSet] = useState("");
  const [locationLatitude, locationLatitudeSet] = useState("");
  const [locationLongitude, locationLongitudeSet] = useState("");
  const [locationAltitude, locationAltitudeSet] = useState("");
  const [applicant, applicantSet] = useState("");
  const [estimatedTime, estimatedTimeSet] = useState(new Date().toISOString());

  function createIncident() {
    try {
      client.incidents
        .addIncident({
          type: type,
          locationLatitude: Number(locationLatitude),
          locationLongitude: Number(locationLongitude),
          locationAltitude: Number(locationAltitude),
          reason: reason,
          estimatedTime: estimatedTime,
          applicant: applicant ?? undefined,
        })
        .then((r) => {
          navigate(`/incidents/${r.data.id}`);
        })
        .catch(catchResponse);
    } catch (e) {
      alert(e);
    }
  }

  return (
    <div className={styles.root}>
      <h2>Create Incident</h2>
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
          <h3>Estimated Time (UTC)</h3>
          <input
            type="input"
            value={estimatedTime}
            onChange={(e) => estimatedTimeSet(e.target.value)}
          />
        </div>
        <button
          onClick={(e) => {
            e.preventDefault();
            createIncident();
          }}
        >
          Submit
        </button>
      </form>
    </div>
  );
}
