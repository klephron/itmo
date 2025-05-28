import { Route } from "~react-router/src/pages/case/+types";
import styles from "./index.module.scss";
import { useClient } from "@state/client";
import { useEffect, useState } from "react";
import {
  CaseDto,
  ClueEntityStatus,
  ClueMainInfoDto,
  IncidentDto,
  TemplateEntityStatus,
  TemplateMainInfoDto,
  UserDto,
} from "@api/gen/data-contracts";
import { Link, useNavigate } from "react-router";

type UserEntry = {
  id: number;
  position: string;
  username?: string;
  name?: string;
  surname?: string;
  patronymic?: string;
};

function userToMap(caze: Record<string, string>) {
  const map = new Map<number, UserEntry>();
  for (const key in caze) {
    const id = Number(key);
    map.set(id, { id: id, position: caze[key] });
  }
  return map;
}

function Users({ caze }: { caze: CaseDto }) {
  const client = useClient((state) => state.client);
  const [users, usersSet] = useState(userToMap(caze.users) as Map<number, UserEntry>);
  const [usersLoaded, usersLoadedSet] = useState(false);

  useEffect(() => {
    if (!users || usersLoaded) {
      return;
    }
    (async () => {
      const users1 = new Map(users);
      for (const user of users1.values()) {
        await client.users
          .getUser(user.id)
          .then((r) => {
            users1.set(user.id, {
              id: user.id,
              position: user.position,
              username: r.data.username,
              name: r.data.name,
              surname: r.data.surname,
              patronymic: r.data.patronymic ?? undefined,
            });
          })
          .catch(console.error);
      }
      usersLoadedSet(true);
      usersSet(users1);
    })();
  }, [users]);

  if (!usersLoaded) {
    return <span>Loading...</span>;
  } else {
    return (
      <table className={styles.table}>
        <thead>
          <tr className={styles.row}>
            <th>Name</th>
            <th>Position</th>
          </tr>
        </thead>
        <tbody>
          {Array.from(users ? users.values() : []).map((user) => (
            <tr className={styles.row} key={user.id}>
              <td>
                <Link
                  to={`/users/${user.id}`}
                >{`${user.name} ${user.surname} ${user.patronymic}`}</Link>
              </td>
              <td>{user.position}</td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }
}

function Clues({ caze }: { caze: CaseDto }) {
  const navigate = useNavigate();
  const client = useClient((state) => state.client);
  const [clues, cluesSet] = useState(null as ClueMainInfoDto[] | null);

  useEffect(() => {
    client.cases
      .getCaseClues(caze.id)
      .then((r) => cluesSet(r.data))
      .catch(console.error);
  }, [caze, client]);

  if (!clues) {
    return <span>Loading...</span>;
  } else {
    return (
      <>
        <div className={styles.funcs}>
          <button onClick={() => navigate(`/clues/create?case_id=${caze.id}`)}>Create</button>
        </div>
        <table className={styles.table}>
          <thead>
            <tr className={styles.row}>
              <th>Title</th>
              <th>Status</th>
              <th>Options</th>
            </tr>
          </thead>
          <tbody>
            {Array.from(clues ?? [])
              .sort((a, b) => b.id - a.id)
              .map((clue) => (
                <tr className={styles.row} key={clue.id}>
                  <td>
                    <Link to={`/clues/${clue.id}`}>{clue.title}</Link>
                  </td>
                  <td>{clue.status}</td>
                  <td>
                    {clue.status == ClueEntityStatus.PENDING ? (
                      <>
                        <button
                          onClick={(_) => {
                            client.clues
                              .updateClue(clue.id, { status: ClueEntityStatus.ACCEPTED })
                              .then((r) => {
                                clue.status = r.data.status;
                                cluesSet([...clues]);
                              })
                              .catch(console.error);
                          }}
                        >
                          Accept
                        </button>
                        <button
                          onClick={(_) => {
                            client.clues
                              .updateClue(clue.id, { status: ClueEntityStatus.REJECTED })
                              .then((r) => {
                                clue.status = r.data.status;
                                cluesSet([...clues]);
                              })
                              .catch(console.error);
                          }}
                        >
                          Reject
                        </button>
                      </>
                    ) : (
                      <></>
                    )}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </>
    );
  }
}

function Suspects({ caze }: { caze: CaseDto }) {
  const [suspects, _suspectsSet] = useState(null as null);
  if (!suspects) {
    return <span>Loading...</span>;
  } else {
    return <table className={styles.table}></table>;
  }
}

function Templates({ caze }: { caze: CaseDto }) {
  const navigate = useNavigate();
  const client = useClient((state) => state.client);
  const [templates, templatesSet] = useState(null as TemplateMainInfoDto[] | null);

  useEffect(() => {
    client.cases
      .getCaseTemplates(caze.id)
      .then((r) => templatesSet(r.data))
      .catch(console.error);
  }, [caze]);

  function download(template: TemplateMainInfoDto) {
    return () => {
      console.log("downloading...", template);
    };
  }

  if (!templates) {
    return <span>Loading...</span>;
  } else {
    return (
      <>
        <div className={styles.funcs}>
          <button onClick={() => navigate(`/templates/create?case_id=${caze.id}`)}>Create</button>
        </div>
        <table className={styles.table}>
          <thead>
            <tr className={styles.row}>
              <th>Title</th>
              <th>Status</th>
              <th>Options</th>
            </tr>
          </thead>
          <tbody>
            {Array.from(templates ?? [])
              .sort((a, b) => b.id - a.id)
              .map((template) => (
                <tr className={styles.row} key={template.id}>
                  <td>
                    <Link to={`/templates/${template.id}`}>{template.title}</Link>
                  </td>
                  <td>{template.status}</td>
                  <td>
                    {template.status == TemplateEntityStatus.PENDING ? (
                      <button
                        onClick={(_) => {
                          client.templates
                            .updateTemplate(template.id, {
                              status: TemplateEntityStatus.CREATED,
                              metadata: template.metadata,
                            })
                            .then((r) => {
                              template.status = r.data.status;
                              templatesSet([...templates]);
                            })
                            .catch(console.error);
                        }}
                      >
                        Accept
                      </button>
                    ) : (
                      <></>
                    )}
                    {template.status == TemplateEntityStatus.CREATED ? (
                      <button onClick={download(template)}>Download</button>
                    ) : (
                      <></>
                    )}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </>
    );
  }
}

// TODO: use search params instead
function Menu({ caze }: { caze: CaseDto }) {
  const [menu, menuSet] = useState("users");

  return (
    <div className={styles.menu}>
      <div className={styles.selector}>
        <button onClick={() => menuSet("users")}>Users</button>
        <button onClick={() => menuSet("clues")}>Clues</button>
        <button onClick={() => menuSet("suspects")}>Suspects</button>
        <button onClick={() => menuSet("templates")}>Templates</button>
      </div>
      <div className={styles.menus}>
        {menu == "users" ? (
          <div>
            <h3>Users</h3>
            <Users caze={caze} />
          </div>
        ) : (
          <></>
        )}
        {menu == "clues" ? (
          <div>
            <h3>Clues</h3>
            <Clues caze={caze} />
          </div>
        ) : (
          <></>
        )}
        {menu == "suspects" ? (
          <div>
            <h3>Suspects</h3>
            <Suspects caze={caze} />
          </div>
        ) : (
          <></>
        )}
        {menu == "templates" ? (
          <div>
            <h3>Templates</h3>
            <Templates caze={caze} />
          </div>
        ) : (
          <></>
        )}
      </div>
    </div>
  );
}

function Case({ id }: { id: number }) {
  const client = useClient((state) => state.client);
  const [caze, cazeSet] = useState(null as CaseDto | null);
  const [incident, incidentSet] = useState(null as IncidentDto | null);
  const [creator, creatorSet] = useState(null as UserDto | null);
  const [updator, updatorSet] = useState(null as UserDto | null);

  useEffect(() => {
    client.cases
      .getCase(id)
      .then((r) => cazeSet(r.data))
      .catch((e) => console.error(e));
  }, [client]);

  useEffect(() => {
    if (!caze) {
      creatorSet(null);
      return;
    }
    client.users
      .getUser(caze.creatorId)
      .then((r) => creatorSet(r.data))
      .catch(console.error);
  }, [caze]);

  useEffect(() => {
    if (!caze) {
      updatorSet(null);
      return;
    }
    client.users
      .getUser(caze.updatorId)
      .then((r) => updatorSet(r.data))
      .catch(console.error);
  }, [caze]);

  useEffect(() => {
    if (!caze) {
      incidentSet(null);
      return;
    }
    client.incidents
      .getIncident(caze.incidentId)
      .then((r) => incidentSet(r.data))
      .catch((e) => console.error(e));
  }, [caze]);

  if (!caze) {
    return <span>Loading...</span>;
  } else {
    return (
      <>
        <h2>Case ({caze.title})</h2>
        <div>
          <h3>Related Incident</h3>
          {incident ? (
            <Link to={`/incidents/${incident.id}`}>
              {incident.reason} ({incident.type})
            </Link>
          ) : (
            <span>Loading...</span>
          )}
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
          <span>{new Date(caze.createdAt).toLocaleString()}</span>
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
          <span>{new Date(caze.createdAt).toLocaleString()}</span>
        </div>
        <div>
          <h3>Status</h3>
          <span>{caze.status}</span>
        </div>
        <div>
          <h3>Description</h3>
          <span>{caze.description ?? "-"}</span>
        </div>
        <Menu caze={caze} />
      </>
    );
  }
}

export default function CasePage({ params }: Route.ComponentProps) {
  return (
    <div className={styles.root}>
      <Case id={Number(params.id)} />
    </div>
  );
}
