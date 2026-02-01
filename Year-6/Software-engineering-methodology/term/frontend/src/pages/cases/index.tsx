import { useEffect, useState } from "react";
import styles from "./index.module.scss";
import { useClient } from "@state/client";
import { UserCaseDto, UserCaseEntityStatus } from "@api/gen/data-contracts";
import { Link } from "react-router";
import { useUser } from "@state/user";

type CaseEntry = {
  id: number;
  title: string;
  position: string;
  status: UserCaseEntityStatus;
};

function infoToMap(infos: UserCaseDto[]): Map<number, CaseEntry> {
  return new Map(
    infos.map((info) => [
      info.idRelatedCaseId,
      {
        id: info.idRelatedCaseId,
        title: info.idRelatedCaseTitle,
        position: info.position,
        status: info.status,
      } satisfies CaseEntry,
    ])
  );
}

function Table() {
  const client = useClient((state) => state.client);
  const user = useUser((state) => state.user);
  const [cases, casesSet] = useState(null as Map<number, CaseEntry> | null);

  useEffect(() => {
    client.users
      .getUserCases(user.id)
      .then((r) => casesSet(infoToMap(r.data)))
      .catch(console.error);
  }, [client]);

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
        {Array.from(cases ? cases.values() : [])
          .sort((a, b) => b.id - a.id)
          .map((caze) => (
            <tr className={styles.row} key={caze.id}>
              <td>
                <Link to={`/cases/${caze.id}`}>{caze.title}</Link>
              </td>
              <td>{caze.position}</td>
              <td>{caze.status}</td>
            </tr>
          ))}
      </tbody>
    </table>
  );
}

export default function CasesPage() {
  return (
    <div className={styles.root}>
      <h2>Assigned Cases</h2>
      <Table />
    </div>
  );
}
