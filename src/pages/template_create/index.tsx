import { useClient } from "@state/client";
import styles from "./index.module.scss";
import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { catchResponse } from "@util";

export default function TemplatesCreatePage() {
  const navigate = useNavigate();
  const [search, _searchSet] = useSearchParams();
  const client = useClient((state) => state.client);

  const [caseId, _caseIdSet] = useState(search.get("case_id"));

  const [title, titleSet] = useState("");
  const [description, descriptionSet] = useState("");

  function buildMetadata() {
    const data = {
      clueIds: [],
      suspectIds: [],
      employeeIds: [],
    };
    return JSON.stringify(data);
  }

  function createTemplate() {
    try {
      client.cases
        .addCaseTemplate(Number(caseId), {
          title: title,
          description: description,
          metadata: buildMetadata(),
        })
        .then((r) => {
          navigate(`/cases/${r.data.relatedCaseId}`);
        })
        .catch(catchResponse);
    } catch (e) {
      alert(e);
    }
  }

  return (
    <div className={styles.root}>
      <h2>Create Template</h2>
      <form>
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
          <div></div>
        </div>
        <div>
          <h3>Clues</h3>
          <div></div>
        </div>
        <div>
          <h3>Suspects</h3>
          <div></div>
        </div>
        <button
          onClick={(e) => {
            e.preventDefault();
            createTemplate();
          }}
        >
          Submit
        </button>
      </form>
    </div>
  );
}
