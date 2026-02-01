import { useClient } from "@state/client";
import styles from "./index.module.scss";
import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { catchResponse } from "@util";
import { CaseSuspectEntityStatus } from "@api/gen/data-contracts";

export default function SuspectCreatePage() {
  const navigate = useNavigate();
  const [search, _searchSet] = useSearchParams();
  const client = useClient((state) => state.client);

  const [caseId, _caseIdSet] = useState(search.get("case_id"));

  const [name, nameSet] = useState("");
  const [surname, surnameSet] = useState("");
  const [patronymic, patronymicSet] = useState("");
  const [identifier, identifierSet] = useState("");
  const [status, statusSet] = useState(CaseSuspectEntityStatus.ACCUSED);
  const [verdict, verdictSet] = useState("");
  const [accusation, accusationSet] = useState("");

  function createSuspect() {
    try {
      client.cases
        .addCaseSuspect(Number(caseId), {
          name: name,
          surname: surname,
          patronymic: patronymic,
          identifier: identifier,
          status: status,
          verdict: verdict,
          accusation: accusation,
        })
        .then((_) => {
          navigate(`/cases/${Number(caseId)}`);
        })
        .catch(catchResponse);
    } catch (e) {
      alert(e);
    }
  }

  return (
    <div className={styles.root}>
      <h2>Create Suspect</h2>
      <form>
        <div>
          <h3>Name</h3>
          <input type="input" value={name} onChange={(e) => nameSet(e.target.value)} />
        </div>
        <div>
          <h3>Surname</h3>
          <input type="input" value={surname} onChange={(e) => surnameSet(e.target.value)} />
        </div>
        <div>
          <h3>Patronymic</h3>
          <input type="input" value={patronymic} onChange={(e) => patronymicSet(e.target.value)} />
        </div>
        <div>
          <h3>Identifier</h3>
          <input type="input" value={identifier} onChange={(e) => identifierSet(e.target.value)} />
        </div>
        <div>
          <h3>Status</h3>
          <select
            value={status}
            onChange={(e) =>
              statusSet(
                CaseSuspectEntityStatus[e.target.value as keyof typeof CaseSuspectEntityStatus]
              )
            }
          >
            {Object.values(CaseSuspectEntityStatus).map((status) => (
              <option value={status} key={status}>
                {status}
              </option>
            ))}
          </select>
        </div>
        <div>
          <h3>Verdict</h3>
          <input value={verdict} onChange={(e) => verdictSet(e.target.value)} />
        </div>
        <div>
          <h3>Accusation</h3>
          <textarea value={accusation} onChange={(e) => accusationSet(e.target.value)} />
        </div>
        <button
          onClick={(e) => {
            e.preventDefault();
            createSuspect();
          }}
        >
          Submit
        </button>
      </form>
    </div>
  );
}
