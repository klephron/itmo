import { useClient } from "@state/client";
import styles from "./index.module.scss";
import { Link } from "react-router";
import { useEffect, useState } from "react";
import { UserCaseDto, UserDto } from "@api/gen/data-contracts";
import { Route } from "~react-router/src/pages/user/+types";

function UserPermissions({ id }: { id: number }) {
  const client = useClient((state) => state.client);
  const [permissions, permissionsSet] = useState(null as string[] | null);

  useEffect(() => {
    client.users
      .getUserPermissions(Number(id))
      .then((r) => permissionsSet(r.data.permissions))
      .catch((e) => console.error(e));
  }, []);

  if (!permissions) {
    return <span>Loading...</span>;
  } else if (!permissions.length) {
    return <span>Empty</span>;
  } else {
    return (
      <table className={styles.table}>
        <tbody>
          {permissions.map((permission) => (
            <tr className={styles.row} key={permission}>
              <td>{permission}</td>
            </tr>
          ))}
        </tbody>
      </table>
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

  useEffect(() => {
    client.users
      .getUser(Number(id))
      .then((r) => userSet(r.data))
      .catch((e) => console.error(e));
  }, [client]);

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
          <span>
            {user.locationLatitude}, {user.locationLongitude}, {user.locationAltitude}
          </span>
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
