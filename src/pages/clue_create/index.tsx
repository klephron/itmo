import { useClient } from "@state/client";
import styles from "./index.module.scss";
import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { catchResponse } from "@util";
import { ClueEntityStatus } from "@api/gen/data-contracts";

export default function ClueCreatePage() {
  const navigate = useNavigate();
  const [search, _searchSet] = useSearchParams();
  const client = useClient((state) => state.client);

  const [caseId, _caseIdSet] = useState(search.get("case_id"));

  const [title, titleSet] = useState("");
  const [description, descriptionSet] = useState("");

  function createClue() {
    try {
      client.cases
        .addCaseClue(Number(caseId), {
          title: title,
          description: description,
          status: ClueEntityStatus.PENDING,
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
      <h2>Create Clue</h2>
      <form>
        <div>
          <h3>Title</h3>
          <input type="input" value={title} onChange={(e) => titleSet(e.target.value)} />
        </div>
        <div>
          <h3>Description</h3>
          <textarea value={description} onChange={(e) => descriptionSet(e.target.value)} />
        </div>
        <button
          onClick={(e) => {
            e.preventDefault();
            createClue();
          }}
        >
          Submit
        </button>
      </form>
    </div>
  );
}
