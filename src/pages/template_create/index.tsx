import { useClient } from "@state/client";
import styles from "./index.module.scss";
import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { catchResponse } from "@util";
import Checkbox from "@components/checkbox";

type UserEntry = {
  id: number;
  name?: string;
  surname?: string;
  patronymic?: string | null;
  position?: string;
  selected: boolean;
};

// TODO: sync when backend is ready
type SuspectEntry = {
  id: number;
  name?: string;
  surname?: string;
  patronymic?: string;
  identifier?: string;
  status?: string;
  // anything else...
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

  const [caseId, _caseIdSet] = useState(search.get("case_id"));

  const [title, titleSet] = useState("");
  const [description, descriptionSet] = useState("");
  const [users, usersSet] = useState(null as Map<number, UserEntry> | null);
  const [clues, cluesSet] = useState(null as Map<number, ClueEntry> | null);
  const [suspects, suspectsSet] = useState(null as Map<number, SuspectEntry> | null);

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

  // TODO: when suspects are ready
  useEffect(() => {
    if (suspects) {
      return;
    }
  }, [caseId, client]);

  function buildMetadata() {
    const data = {
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
        >
          Submit
        </button>
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
    return <table className={styles.table}></table>;
  }
}
