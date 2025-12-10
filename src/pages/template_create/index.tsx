import { useClient } from "@state/client";
import styles from "./index.module.scss";
import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { catchResponse } from "@util";
import { useToken } from "@state/token";
import Checkbox from "@components/checkbox";
import { CaseSuspectDto, CaseSuspectMainInfoDto, TemplateEntityStatus, TemplateMetadata } from "@api/gen/data-contracts";

type UserEntry = {
  id: number;
  name?: string;
  surname?: string;
  patronymic?: string | null;
  position?: string;
  selected: boolean;
};

type SuspectEntry = CaseSuspectMainInfoDto & {
  selected: boolean;
};

type ClueEntry = {
  id: number;
  title: string;
  description?: string | null;
  // metadata...
  selected: boolean;
};

export default function TemplatesCreatePage() {
  const navigate = useNavigate();
  const [search, _searchSet] = useSearchParams();
  const client = useClient((state) => state.client);
  const token = useToken((state) => state.token);
  const authHeader =
    token.present && token.accessToken ? `${token.tokenType} ${token.accessToken}` : undefined;

  const [caseId, _caseIdSet] = useState(search.get("case_id"));

  const [title, titleSet] = useState("");
  const [description, descriptionSet] = useState("");
  const [users, usersSet] = useState(null as Map<number, UserEntry> | null);
  const [clues, cluesSet] = useState(null as Map<number, ClueEntry> | null);
  const [suspects, suspectsSet] = useState(null as Map<number, SuspectEntry> | null);
  const [creating, creatingSet] = useState(false);
  const [error, errorSet] = useState<string | null>(null);

  useEffect(() => {
    if (users) {
      return;
    }
    client.cases
      .getCase(Number(caseId))
      .then((r) => r.data.users)
      .then(async (r) => {
        const map = new Map<number, UserEntry>();
        for (const key in r) {
          const id = Number(key);
          map.set(id, { id: id, position: r[key], selected: true });
        }
        for (const user of map.values()) {
          await client.users
            .getUser(user.id)
            .then((r) => {
              map.set(user.id, {
                id: user.id,
                name: r.data.name,
                surname: r.data.surname,
                patronymic: r.data.patronymic,
                position: user.position,
                selected: user.selected,
              });
            })
            .catch(console.error);
        }
        usersSet(map);
      });
  }, [caseId, client]);

  useEffect(() => {
    if (clues) {
      return;
    }
    client.cases
      .getCaseClues(Number(caseId))
      .then((r) => r.data)
      .then((r) => {
        const map = new Map<number, ClueEntry>();
        for (const clue of r) {
          map.set(clue.id, {
            id: clue.id,
            title: clue.title,
            description: clue.description,
            selected: true,
          });
        }
        cluesSet(map);
      })
      .catch(console.error);
  }, [caseId, client]);

  useEffect(() => {
    if (suspects) {
      return;
    }
    client.cases
      .getAllCaseSuspect(Number(caseId))
      .then((r) => r.data)
      .then((r) => {
        const map = new Map<number, SuspectEntry>();
        console.log(r);
        for (const suspect of r) {
          map.set(suspect.id, { ...suspect, selected: true });
        }
        suspectsSet(map);
      })
      .catch(console.error);
  }, [caseId, client]);

function buildMetadata() {
  const data: TemplateMetadata = {
    employeeIds: users
      ? users
          .values()
            .filter((u) => u.selected)
            .map((u) => u.id)
            .toArray()
        : [],
      suspectIds: suspects
        ? suspects
            .values()
            .filter((u) => u.selected)
            .map((u) => u.id)
            .toArray()
        : [],
      clueIds: clues
        ? clues
            .values()
            .filter((u) => u.selected)
      .map((u) => u.id)
        .toArray()
    : [],
  };
  return data;
}

  function buildTemplateContent() {
    const lines: string[] = [];
    lines.push(`# Template: ${title || "(untitled)"}`);
    lines.push("");
    if (description) {
      lines.push(description);
      lines.push("");
    }
    lines.push("## Users");
    lines.push("");
    lines.push("| ID | Name | Position |");
    lines.push("|----|------|----------|");
    users?.forEach((u) => {
      if (u.selected) {
        lines.push(`|${u.id}|${(u.name ?? "") + " " + (u.surname ?? "")}|${u.position ?? ""}|`);
      }
    });
    lines.push("");
    lines.push("## Clues");
    lines.push("");
    lines.push("| ID | Title | Description |");
    lines.push("|----|-------|-------------|");
    clues?.forEach((c) => {
      if (c.selected) {
        lines.push(`|${c.id}|${c.title}|${c.description ?? ""}|`);
      }
    });
    lines.push("");
    lines.push("## Suspects");
    lines.push("");
    lines.push("| ID | Name | Verdict | Accusation | Status |");
    lines.push("|----|------|---------|------------|--------|");
    suspects?.forEach((s) => {
      if (s.selected) {
        lines.push(`|${s.id}|${s.name} ${s.surname}|${s.verdict ?? ""}|${s.accusation ?? ""}|${s.status}|`);
      }
    });
    lines.push("");
    return lines.join("\n");
  }

  async function createTemplate() {
    creatingSet(true);
    errorSet(null);
    client.cases
      .addCaseTemplate(Number(caseId), {
        title: title,
        description: description,
        metadata: buildMetadata(),
      })
      .then(async (r) => {
        const templateId = r.data.id;
        // Автозагрузка шаблона в ЦБД
        if (client.baseUrl && authHeader) {
          try {
            const content = buildTemplateContent();
            const blob = new Blob([content], { type: "text/markdown" });
            const formData = new FormData();
            formData.append("file", blob, `template_${templateId}.md`);
            const res = await fetch(`${client.baseUrl}/api/files`, {
              method: "POST",
              headers: { Authorization: authHeader },
              body: formData,
            });
            if (!res.ok) throw new Error(`Upload failed: ${res.status}`);
            const idResp = (await res.json()) as { id: string };
            const metaRes = await fetch(`${client.baseUrl}/api/files/${idResp.id}/meta`, {
              headers: { Authorization: authHeader },
            });
            if (!metaRes.ok) throw new Error(`Meta failed: ${metaRes.status}`);
            const meta = await metaRes.json();

            const nextMeta: TemplateMetadata = {
              ...buildMetadata(),
              templateFileId: idResp.id,
              templateFileUrl: client.baseUrl ? `${client.baseUrl}/api${meta.downloadUrl}` : meta.downloadUrl,
              templateFileName: meta.filename,
              templateContentType: meta.contentType,
            };
            await client.templates.updateTemplate(templateId, {
              title: r.data.title,
              status: TemplateEntityStatus.CDB_ADDED,
              cdbId: idResp.id,
              metadata: nextMeta,
            });
          } catch (e: any) {
            console.error("Auto-upload template file failed", e);
            errorSet(e?.message || "Не удалось загрузить шаблон в ЦБД");
          }
        }
        navigate(`/cases/${r.data.relatedCaseId}`);
      })
      .catch(catchResponse)
      .finally(() => creatingSet(false));
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
          <Users users={users} usersSet={usersSet} />
        </div>
        <div>
          <h3>Clues</h3>
          <Clues clues={clues} cluesSet={cluesSet} />
        </div>
        <div>
          <h3>Suspects</h3>
          <Suspects suspects={suspects} suspectsSet={suspectsSet} />
        </div>
        <button
          onClick={(e) => {
            e.preventDefault();
            createTemplate();
          }}
          disabled={creating}
        >
          Submit
        </button>
        {error ? <div className={styles.error}>{error}</div> : null}
      </form>
    </div>
  );
}

function Users({
  users,
  usersSet,
}: {
  users: Map<number, UserEntry> | null;
  usersSet: React.Dispatch<React.SetStateAction<Map<number, UserEntry> | null>>;
}) {
  if (!users) {
    return <span>Loading...</span>;
  } else {
    return (
      <table className={styles.table}>
        <thead>
          <tr className={styles.row}>
            <th>Select</th>
            <th>Name</th>
            <th>Position</th>
          </tr>
        </thead>
        <tbody>
          {Array.from(users ? users.values() : []).map((user) => (
            <tr className={styles.row} key={user.id}>
              <td>
                <Checkbox
                  isChecked={user.selected}
                  onChange={(_) => {
                    user.selected = !user.selected;
                    usersSet(new Map(users));
                  }}
                />
              </td>
              <td>{`${user.name} ${user.surname} ${user.patronymic}`}</td>
              <td>{user.position}</td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }
}

function Clues({
  clues,
  cluesSet,
}: {
  clues: Map<number, ClueEntry> | null;
  cluesSet: React.Dispatch<React.SetStateAction<Map<number, ClueEntry> | null>>;
}) {
  if (!clues) {
    return <span>Loading...</span>;
  } else {
    return (
      <table className={styles.table}>
        <thead>
          <tr className={styles.row}>
            <th>Select</th>
            <th>Title</th>
            <th>Descroption</th>
          </tr>
        </thead>
        <tbody>
          {Array.from(clues ? clues.values() : []).map((clue) => (
            <tr className={styles.row} key={clue.id}>
              <td>
                <Checkbox
                  isChecked={clue.selected}
                  onChange={(_) => {
                    clue.selected = !clue.selected;
                    cluesSet(new Map(clues));
                  }}
                />
              </td>
              <td>{clue.title}</td>
              <td>{clue.description}</td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }
}

function Suspects({
  suspects,
  suspectsSet,
}: {
  suspects: Map<number, SuspectEntry> | null;
  suspectsSet: React.Dispatch<React.SetStateAction<Map<number, SuspectEntry> | null>>;
}) {
  if (!suspects) {
    return <span>Loading...</span>;
  } else {
    return (
      <table className={styles.table}>
        <thead>
          <tr className={styles.row}>
            <th>Name</th>
            <th>Identifier</th>
            <th>Verdict</th>
            <th>Accusation</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {Array.from(suspects ? suspects.values() : []).map((suspect) => (
            <tr className={styles.row} key={suspect.id}>
              <td>
                <Checkbox
                  isChecked={suspect.selected}
                  onChange={(_) => {
                    suspect.selected = !suspect.selected;
                    suspectsSet(new Map(suspects));
                  }}
                />
              </td>
              <td>
                {suspect.name} {suspect.surname} {suspect.patronymic}
              </td>
              <td>{suspect.verdict}</td>
              <td>{suspect.accusation}</td>
              <td>{suspect.status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }
}
