import { Route } from "~react-router/src/pages/case/+types";
import styles from "./index.module.scss";
import { useClient } from "@state/client";
import { useEffect, useRef, useState } from "react";
import {
  CaseDto,
  CaseSuspectEntityStatus,
  CaseEntityStatus,
  CaseSuspectDto,
  CaseSuspectMainInfoDto,
  ClueDto,
  ClueEntityStatus,
  ClueMainInfoDto,
  ClueMetadata,
  IncidentDto,
  TemplateDto,
  TemplateEntityStatus,
  TemplateMainInfoDto,
  TemplateMetadata,
  UserInfoResponse,
  UserDto,
} from "@api/gen/data-contracts";
import { Link, useNavigate } from "react-router";
import { exportToFile } from "@util";
import { hasPermission, usePermissions } from "@state/permissions";
import { useUser } from "@state/user";
import { useToken } from "@state/token";
import { useToken } from "@state/token";

type UserEntry = {
  id: number;
  position: string;
  username?: string;
  name?: string;
  surname?: string;
  patronymic?: string | null;
};

function userToMap(caze: Record<string, string>) {
  const map = new Map<number, UserEntry>();
  for (const key in caze) {
    const id = Number(key);
    map.set(id, { id: id, position: caze[key] });
  }
  return map;
}

function Users({
  caseUsers,
  setCaseUsers,
  canUpdateCase,
}: {
  caseUsers: Record<string, string>;
  setCaseUsers: (users: Record<string, string>) => void;
  canUpdateCase: boolean;
}) {
  const client = useClient((state) => state.client);
  const [users, usersSet] = useState(userToMap(caseUsers) as Map<number, UserEntry>);
  const [usersLoaded, usersLoadedSet] = useState(false);
  const [userOptions, userOptionsSet] = useState<UserInfoResponse[]>([]);
  const [newUserId, newUserIdSet] = useState("");
  const [newUserPosition, newUserPositionSet] = useState("");
  const [positions, positionsSet] = useState<string[]>([]);

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

  useEffect(() => {
    usersSet(userToMap(caseUsers));
    usersLoadedSet(false);
  }, [caseUsers]);

  useEffect(() => {
    client.users
      .getUsers()
      .then((r) => userOptionsSet(r.data))
      .catch(console.error);
    client.users
      .getPrimaryPositions()
      .then((r) => {
        const normalized = r.data.map((p: any) =>
          typeof p === "string" ? p : p?.value ?? p?.label ?? String(p)
        );
        positionsSet(normalized);
      })
      .catch(console.error);
  }, [client]);

  function addUser() {
    const idNum = Number(newUserId);
    if (!idNum || !newUserPosition.trim()) return;
    const next = new Map(users);
    next.set(idNum, {
      id: idNum,
      position: newUserPosition.trim(),
    });
    usersSet(next);
    const record: Record<string, string> = {};
    next.forEach((u) => {
      if (u.position.trim()) {
        record[String(u.id)] = u.position.trim();
      }
    });
    setCaseUsers(record);
    newUserIdSet("");
    newUserPositionSet("");
  }

  if (!usersLoaded) {
    return <span>Loading...</span>;
  } else {
    return (
      <div className={styles.card}>
        <h4>Participants</h4>
        <table className={styles.table}>
          <thead>
            <tr className={styles.row}>
              <th>Position</th>
              <th>User</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {Array.from(users ? users.values() : []).map((user) => (
              <tr className={styles.row} key={user.id}>
                <td>
                  <select
                    value={user.position}
                    disabled={!canUpdateCase}
                    onChange={(e) => {
                      user.position = e.target.value;
                      const next = new Map(users);
                      const record: Record<string, string> = {};
                      next.forEach((u) => {
                        if (u.position.trim()) {
                          record[String(u.id)] = u.position.trim();
                        }
                      });
                      setCaseUsers(record);
                      usersSet(new Map(users));
                    }}
                  >
                    <option value="">Select position</option>
                    {positions.map((p) => (
                      <option key={p} value={p}>
                        {p}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  {user.username ? (
                    <Link to={`/users/${user.id}`}>
                      {user.name} {user.surname} {user.patronymic}
                    </Link>
                  ) : (
                    user.id
                  )}
                </td>
                <td>
                  <button
                    disabled={!canUpdateCase}
                    onClick={() => {
                      const next = new Map(users);
                      next.delete(user.id);
                      const record: Record<string, string> = {};
                      next.forEach((u) => {
                        if (u.position.trim()) {
                          record[String(u.id)] = u.position.trim();
                        }
                      });
                      setCaseUsers(record);
                      usersSet(next);
                    }}
                  >
                    Remove
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {canUpdateCase ? (
          <div className={styles.addUser}>
            <select
              value={newUserId}
              onChange={(e) => newUserIdSet(e.target.value)}
            >
              <option value="">Select user</option>
              {userOptions.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.username} ({u.id})
                </option>
              ))}
            </select>
            <select
              value={newUserPosition}
              onChange={(e) => newUserPositionSet(e.target.value)}
            >
              <option value="">Select position</option>
              {positions.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
            <button type="button" onClick={addUser}>
              Add user
            </button>
          </div>
        ) : null}
      </div>
    );
  }
}

type CasePermissions = {
  clueAdd: boolean;
  clueUpdate: boolean;
  clueDelete: boolean;
  suspectAdd: boolean;
  suspectUpdate: boolean;
  suspectDelete: boolean;
  templateAdd: boolean;
  templateUpdate: boolean;
  templateDelete: boolean;
};

function Clues({ caze, perms, canViewDebug }: { caze: CaseDto; perms: CasePermissions; canViewDebug: boolean }) {
  const navigate = useNavigate();
  const client = useClient((state) => state.client);
  const permissionsState = usePermissions((state) => state.permissions);
  const user = useUser((state) => state.user);
  const token = useToken((state) => state.token);
  const [clues, cluesSet] = useState<
    (ClueMainInfoDto & { description?: string | null; metadata?: ClueMetadata | null })[] | null
  >(null);
  const [previewMap, previewMapSet] = useState<Record<string, string>>({});
  const authHeader =
    token.present && token.accessToken ? `${token.tokenType} ${token.accessToken}` : undefined;

  useEffect(() => {
    client.cases
      .getCaseClues(caze.id)
      .then((r) => cluesSet(r.data))
      .catch(console.error);
  }, [caze, client]);

  function resolveUrl(idOrPath: string, type: "inline" | "download") {
    if (!idOrPath) return "";
    if (idOrPath.startsWith("http://") || idOrPath.startsWith("https://")) {
      return idOrPath;
    }
    if (idOrPath.startsWith("/")) {
      return client.baseUrl ? `${client.baseUrl}${idOrPath}` : idOrPath;
    }
    const suffix = type === "inline" ? "/inline" : "/download";
    return client.baseUrl
      ? `${client.baseUrl}/api/files/${idOrPath}${suffix}`
      : `/api/files/${idOrPath}${suffix}`;
  }

  async function fetchPreview(url: string) {
    try {
      const res = await fetch(url, {
        headers: authHeader ? { Authorization: authHeader } : undefined,
      });
      if (!res.ok) throw new Error(`Preview failed ${res.status}`);
      const blob = await res.blob();
      const obj = URL.createObjectURL(blob);
      previewMapSet((prev) => ({ ...prev, [url]: obj }));
    } catch (e) {
      console.error("[case clues] preview load failed", url, e);
    }
  }

  useEffect(() => {
    if (!clues) return;
    const uris = new Set<string>();
    clues.forEach((clue) => {
      (clue.metadata?.imageUris ?? []).forEach((uri) => {
        uris.add(resolveUrl(uri, "inline"));
      });
    });
    uris.forEach((u) => {
      if (!previewMap[u]) {
        fetchPreview(u);
      }
    });
  }, [clues, authHeader]);

  async function openInline(uri: string) {
    const url = resolveUrl(uri, "inline");
    try {
      const res = await fetch(url, {
        headers: authHeader ? { Authorization: authHeader } : undefined,
      });
      if (!res.ok) throw new Error(`Open failed ${res.status}`);
      const blob = await res.blob();
      const obj = URL.createObjectURL(blob);
      window.open(obj, "_blank");
    } catch (e) {
      console.error(e);
      alert("Не удалось открыть файл");
    }
  }

  async function download(uri: string, filename?: string) {
    const url = resolveUrl(uri, "download");
    try {
      const res = await fetch(url, {
        headers: authHeader ? { Authorization: authHeader } : undefined,
      });
      if (!res.ok) throw new Error(`Download failed ${res.status}`);
      const blob = await res.blob();
      exportToFile(blob, blob.type || "application/octet-stream", filename || "file");
    } catch (e) {
      console.error(e);
      alert("Не удалось скачать файл");
    }
  }

  if (!clues) {
    return <span>Loading...</span>;
  } else {
    return (
      <>
        <div className={styles.funcs}>
          <button
            onClick={() => navigate(`/clues/create?case_id=${caze.id}`)}
            disabled={!perms.clueAdd}
          >
            Create
          </button>
          {!perms.clueAdd ? (
            <span className={styles.hint}>
              Нет прав{canViewDebug ? ` (CASES:${caze.id}:CLUES_ADD)` : ""}
            </span>
          ) : null}
          {!perms.clueAdd && canViewDebug ? (
            <div className={styles.debug}>
              caseId: {caze.id}
              {"\n"}ownerId: {caze.creatorId}
              {"\n"}currentUserId: {user.present ? user.id : "n/a"}
              {"\n"}permissions loaded: {String(!!permissionsState)}
              {"\n"}perm pattern: CASES:{caze.id}:CLUES_ADD
            </div>
          ) : null}
        </div>
        <table className={styles.table}>
          <thead>
            <tr className={styles.row}>
              <th>Title</th>
              <th>Status</th>
              <th>Description</th>
              <th>Attachments</th>
              <th>Options</th>
            </tr>
          </thead>
          <tbody>
            {Array.from(clues ?? [])
              .sort((a, b) => b.id - a.id)
              .map((clue) => (
                <tr className={styles.row} key={clue.id}>
                  <td>
                    <input
                      value={clue.title}
                      disabled={!perms.clueUpdate}
                      onChange={(e) => {
                        clue.title = e.target.value;
                        cluesSet([...clues]);
                      }}
                    />
                    <div className={styles.small}>
                      <Link to={`/clues/${clue.id}`}>open</Link>
                    </div>
                  </td>
                  <td>
                    <select
                      value={clue.status}
                      disabled={!perms.clueUpdate}
                      onChange={(e) => {
                        clue.status = e.target.value as ClueEntityStatus;
                        cluesSet([...clues]);
                      }}
                    >
                      {Object.values(ClueEntityStatus).map((s) => (
                        <option key={s} value={s}>
                          {s}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <textarea
                      value={clue.description ?? ""}
                      disabled={!perms.clueUpdate}
                      onChange={(e) => {
                        clue.description = e.target.value;
                        cluesSet([...clues]);
                      }}
                    />
                  </td>
                  <td>
                    <div className={styles.files}>
                      {(clue.metadata?.imageUris ?? []).map((uri: string) => (
                        <div key={uri} className={styles.file}>
                          {previewMap[resolveUrl(uri, "inline")] ? (
                            <img src={previewMap[resolveUrl(uri, "inline")]} className={styles.preview} />
                          ) : (
                            <div className={styles.small}>Loading preview...</div>
                          )}
                          <div className={styles.small}>
                            <button type="button" onClick={() => openInline(uri)}>
                              Open
                            </button>
                          </div>
                        </div>
                      ))}
                      {(clue.metadata?.reportUris ?? []).map((uri: string) => (
                        <div key={uri} className={styles.file}>
                          <button type="button" onClick={() => download(uri)}>
                            Download
                          </button>
                        </div>
                      ))}
                      {(clue.metadata?.videoUris ?? []).map((uri: string) => (
                        <div key={uri} className={styles.file}>
                          <button type="button" onClick={() => openInline(uri)}>
                            View video
                          </button>
                        </div>
                      ))}
                      {!clue.metadata?.imageUris?.length &&
                      !clue.metadata?.reportUris?.length &&
                      !clue.metadata?.videoUris?.length ? (
                        <span className={styles.small}>-</span>
                      ) : null}
                    </div>
                  </td>
                  <td className={styles.rowActions}>
                    {perms.clueUpdate ? (
                      <button
                        onClick={(_) => {
                          client.clues
                            .updateClue(clue.id, {
                              title: clue.title,
                              description: clue.description,
                              status: clue.status,
                              metadata: clue.metadata || {},
                            })
                            .then((r) => {
                              const next = clues.map((c) => (c.id === clue.id ? { ...c, ...r.data } : c));
                              cluesSet(next);
                            })
                            .catch(console.error);
                        }}
                      >
                        Save
                      </button>
                    ) : null}
                    {perms.clueDelete ? (
                      <button
                        onClick={(_) => {
                          if (!confirm("Delete clue?")) return;
                          client.clues
                            .deleteClue(clue.id)
                            .then(() => cluesSet(clues.filter((c) => c.id !== clue.id)))
                            .catch(console.error);
                        }}
                      >
                        Delete
                      </button>
                    ) : null}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </>
    );
  }
}

function Suspects({ caze, perms, canViewDebug }: { caze: CaseDto; perms: CasePermissions; canViewDebug: boolean }) {
  const navigate = useNavigate();
  const client = useClient((state) => state.client);
  const [suspects, suspectsSet] = useState<(CaseSuspectMainInfoDto & { editing?: boolean })[] | null>(
    null
  );

  useEffect(() => {
    client.cases
      .getAllCaseSuspect(caze.id)
      .then((r) => suspectsSet(r.data))
      .catch(console.error);
  }, [caze, client]);

  useEffect(() => {
    console.log(suspects);
  });

  if (!suspects) {
    return <span>Loading...</span>;
  } else {
    return (
      <>
        <div className={styles.funcs}>
          <button
            onClick={() => navigate(`/suspects/create?case_id=${caze.id}`)}
            disabled={!perms.suspectAdd}
          >
            Create
          </button>
          {!perms.suspectAdd ? (
            <span className={styles.hint}>
              Нет прав{canViewDebug ? ` (CASES:${caze.id}:SUSPECT_ADD)` : ""}
            </span>
          ) : null}
        </div>
        <table className={styles.table}>
          <thead>
            <tr className={styles.row}>
              <th>Name</th>
              <th>Identifier</th>
              <th>Verdict</th>
              <th>Accusation</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {Array.from(suspects ?? [])
              .sort((a, b) => b.id - a.id)
              .map((suspect) => (
                <tr className={styles.row} key={suspect.id}>
                  <td>
                    <input
                      value={suspect.name}
                      disabled={!perms.suspectUpdate}
                      onChange={(e) => {
                        suspect.name = e.target.value;
                        suspectsSet([...suspects]);
                      }}
                    />
                    <input
                      value={suspect.surname}
                      disabled={!perms.suspectUpdate}
                      onChange={(e) => {
                        suspect.surname = e.target.value;
                        suspectsSet([...suspects]);
                      }}
                    />
                    <input
                      value={suspect.patronymic ?? ""}
                      disabled={!perms.suspectUpdate}
                      onChange={(e) => {
                        suspect.patronymic = e.target.value;
                        suspectsSet([...suspects]);
                      }}
                    />
                  </td>
                  <td>
                    <input
                      value={suspect.identifier}
                      disabled={!perms.suspectUpdate}
                      onChange={(e) => {
                        suspect.identifier = e.target.value;
                        suspectsSet([...suspects]);
                      }}
                    />
                  </td>
                  <td>
                    <input
                      value={suspect.verdict}
                      disabled={!perms.suspectUpdate}
                      onChange={(e) => {
                        suspect.verdict = e.target.value;
                        suspectsSet([...suspects]);
                      }}
                    />
                  </td>
                  <td>
                    <input
                      value={suspect.accusation}
                      disabled={!perms.suspectUpdate}
                      onChange={(e) => {
                        suspect.accusation = e.target.value;
                        suspectsSet([...suspects]);
                      }}
                    />
                  </td>
                  <td>
                    <select
                      value={suspect.status}
                      disabled={!perms.suspectUpdate}
                      onChange={(e) => {
                        suspect.status = e.target.value as any;
                        suspectsSet([...suspects]);
                      }}
                    >
                      {Object.values(CaseSuspectEntityStatus).map((s) => (
                        <option key={s} value={s}>
                          {s}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    {perms.suspectUpdate ? (
                      <button
                        onClick={() => {
                          client.suspects
                            .updateCaseSuspect(suspect.id, {
                              name: suspect.name,
                              surname: suspect.surname,
                              patronymic: suspect.patronymic,
                              identifier: suspect.identifier,
                              verdict: suspect.verdict,
                              accusation: suspect.accusation,
                              status: suspect.status,
                            })
                            .then((r) => {
                              const next = suspects.map((s) =>
                                s.id === suspect.id ? { ...s, ...r.data } : s
                              );
                              suspectsSet(next);
                            })
                            .catch(console.error);
                        }}
                      >
                        Save
                      </button>
                    ) : null}
                    {perms.suspectDelete ? (
                      <button
                        onClick={() => {
                          if (!confirm("Delete suspect?")) return;
                          client.suspects
                            .deleteCaseSuspect(suspect.id)
                            .then(() => suspectsSet(suspects.filter((s) => s.id !== suspect.id)))
                            .catch(console.error);
                        }}
                      >
                        Delete
                      </button>
                    ) : null}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </>
    );
  }
}

function metadataToString(metadata: any): string {
  if (metadata === null || metadata === undefined) return "";
  if (typeof metadata === "string") return metadata;
  try {
    return JSON.stringify(metadata, null, 2);
  } catch {
    return String(metadata);
  }
}

function metadataPreview(metadata: any): string {
  const str = metadataToString(metadata);
  if (str.length > 120) {
    return str.slice(0, 120) + "…";
  }
  return str || "-";
}

function normalizeTemplateMetadata(metadata: any): TemplateMetadata | null {
  if (metadata === null || metadata === undefined || metadata === "") return null;
  if (typeof metadata === "string") {
    try {
      return JSON.parse(metadata) as TemplateMetadata;
    } catch {
      return null;
    }
  }
  return metadata as TemplateMetadata;
}

function Templates({ caze, perms, canViewDebug }: { caze: CaseDto; perms: CasePermissions; canViewDebug: boolean }) {
  const navigate = useNavigate();
  const client = useClient((state) => state.client);
  const token = useToken((state) => state.token);
  const [templates, templatesSet] = useState<(TemplateMainInfoDto & { description?: string })[] | null>(
    null
  );
  const [metadataModal, setMetadataModal] = useState<{ id: number; value: string } | null>(null);
  const authHeader =
    token.present && token.accessToken ? `${token.tokenType} ${token.accessToken}` : undefined;
  const syncedCdbMeta = useRef<Set<number>>(new Set());

  useEffect(() => {
    client.cases
      .getCaseTemplates(caze.id)
      .then((r) =>
        templatesSet(
          r.data.map((t) => ({
            ...t,
            metadata: metadataToString(t.metadata),
          }))
        )
      )
      .catch(console.error);
  }, [caze]);

  useEffect(() => {
    // автозаполнение меты по cdbId, если она пустая
    if (!templates || !authHeader) return;
    templates.forEach(async (t) => {
      if (!t.cdbId) return;
      const meta = normalizeTemplateMetadata(t.metadata);
      const alreadySynced = syncedCdbMeta.current.has(t.id);
      const hasFileInfo = !!meta?.templateFileId || !!meta?.templateFileUrl;
      if (alreadySynced || hasFileInfo) return;
      try {
        const url = client.baseUrl ? `${client.baseUrl}/api/files/${t.cdbId}/meta` : `/api/files/${t.cdbId}/meta`;
        const res = await fetch(url, {
          headers: { Authorization: authHeader },
        });
        if (!res.ok) return;
        const contentType = res.headers.get("content-type") || "";
        if (!contentType.includes("json")) {
          console.warn("Meta response is not json, skip", t.id, contentType);
          return;
        }
        let fileMeta: any;
        try {
          fileMeta = await res.json();
        } catch (err) {
          console.warn("Failed to parse meta json", t.id, err);
          return;
        }
        const nextMeta = {
          ...(meta || {}),
          templateFileId: t.cdbId,
          templateFileUrl: fileMeta.downloadUrl?.startsWith("http")
            ? fileMeta.downloadUrl
            : `${client.baseUrl ?? ""}/api${fileMeta.downloadUrl}`,
          templateFileName: fileMeta.filename,
          templateContentType: fileMeta.contentType,
        };
        const updated = await client.templates.updateTemplate(t.id, {
          title: t.title,
          status: t.status,
          metadata: nextMeta as any,
          cdbId: t.cdbId,
        });
        syncedCdbMeta.current.add(t.id);
        templatesSet((prev) =>
          prev
            ? prev.map((tpl) =>
                tpl.id === t.id
                  ? { ...tpl, ...updated.data, metadata: metadataToString(updated.data.metadata) }
                  : tpl
              )
            : prev
        );
      } catch (e) {
        console.error("Failed to sync cdb metadata for template", t.id, e);
      }
    });
  }, [templates, authHeader, client.baseUrl, client.templates]);

  // use server function for that
  function downloadConstructed(
    template: TemplateDto,
    subscriber: UserDto,
    users: (UserDto & UserEntry)[],
    suspects: CaseSuspectDto[],
    clues: ClueDto[]
  ) {
    let content = "";
    content += `# Отчет №${template.id} о деле №${caze.id} от ${new Date(template.updatedAt).toLocaleDateString()}\n\n`;

    content += "## Участники дела\n\n";

    content += "| ФИО | Должность |\n";
    content += "|-----|-----------|\n";
    for (const user of users) {
      content += `|${user.name} ${user.surname} ${user?.patronymic}|${user.position}|\n`;
    }
    content += "\n\n";

    content += "## Улики\n\n";

    content += "|Номер (№)|Название|Описание|\n";
    content += "|-----|--------|--------|\n";
    for (const clue of clues) {
      content += `|${clue.id}|${clue.title}|${clue.description ?? "Отсутствует"}|\n`;
    }
    content += "\n\n";

    content += "## Подозреваемые\n\n";

    content += "| ФИО | Идентифицирующие данные | Вердикт | Обвиняется в | Статус |\n";
    content += "|-----|-------------------------|---------|--------------|----------|\n";
    for (const suspect of suspects) {
      content += `|${suspect.name} ${suspect.surname} ${suspect.patronymic}|${suspect.identifier}|${suspect.verdict}|${suspect.accusation}|${suspect.status}|\n`;
    }
    content += "\n\n";

    content += `Подписано ${subscriber.name} ${subscriber.surname} ${subscriber.patronymic} ________________`;

    exportToFile(content, "text/markdown", `PAI_report_${template.id}.md`);
  }

  function download(template: TemplateMainInfoDto) {
    return async () => {
      const metadata = normalizeTemplateMetadata(template.metadata);
      if (!metadata && !template.cdbId) {
        alert("Invalid metadata");
        return;
      }
      // если есть файл в ЦБД — качаем его
      if (template.cdbId) {
        await downloadFile(`/api/files/${template.cdbId}/download`, `template_${template.id}.md`);
        return;
      }
      const caseUsers = await client.cases
        .getCase(caze.id)
        .then((r) => r.data.users)
        .then(userToMap)
        .catch((e) => {
          console.error(e);
          return new Map<number, UserEntry>();
        });

      const templateFull = await client.templates.getTemplate(template.id).then((r) => r.data);
      const subscriber = await client.users.getUser(templateFull.updatorId).then((r) => r.data);

      const users = [] as (UserDto & UserEntry)[];
      const suspects = [] as CaseSuspectDto[];
      const clues = [] as ClueDto[];

      for (const userId of metadata.employeeIds ?? []) {
        const user = await client.users
          .getUser(userId)
          .then((r) => r.data)
          .catch(console.error);
        if (user) {
          const user1 = {
            ...user,
            ...caseUsers.get(userId),
            position: caseUsers.get(userId)?.position ?? "Unspecified",
          };
          users.push(user1);
        }
      }
      for (const suspectId of metadata.suspectIds ?? []) {
        const suspect = await client.suspects
          .getCaseSuspect(suspectId)
          .then((r) => r.data)
          .catch(console.error);
        if (suspect) {
          suspects.push(suspect);
        }
      }
      for (const clueId of metadata.clueIds ?? []) {
        const clue = await client.clues
          .getClue(clueId)
          .then((r) => r.data)
          .catch(console.error);
        if (clue) {
          clues.push(clue);
        }
      }

      downloadConstructed(templateFull, subscriber, users, suspects, clues);
    };
  }

  function resolveAbsolute(url?: string | null) {
    if (!url) return "";
    if (url.startsWith("http://") || url.startsWith("https://")) return url;
    if (url.startsWith("/api") && client.baseUrl) return `${client.baseUrl}${url}`;
    if (url.startsWith("/")) return client.baseUrl ? `${client.baseUrl}${url}` : url;
    return client.baseUrl ? `${client.baseUrl}/api${url}` : url;
  }

  async function downloadFile(url?: string | null, filename?: string) {
    if (!url) return;
    const resolved = resolveAbsolute(url);
    try {
      const res = await fetch(resolved, {
        headers: authHeader ? { Authorization: authHeader } : undefined,
      });
      if (!res.ok) throw new Error(`Download failed ${res.status}`);
      const blob = await res.blob();
      exportToFile(blob, blob.type || "application/octet-stream", filename || "file");
    } catch (e) {
      console.error(e);
      alert("Не удалось скачать файл");
    }
  }

  async function uploadFileForTemplate(
    tmpl: TemplateMainInfoDto & { description?: string },
    kind: "templateFile" | "reportFile",
    file: File
  ) {
    if (!client.baseUrl || !authHeader) {
      alert("Не авторизован для загрузки файла");
      return;
    }
    try {
      const formData = new FormData();
      formData.append("file", file);
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
      const meta = (await metaRes.json()) as any;
      const downloadUrl = client.baseUrl ? `${client.baseUrl}/api${meta.downloadUrl}` : meta.downloadUrl;
      const nextMeta = normalizeTemplateMetadata(tmpl.metadata) || {};
      if (kind === "templateFile") {
        nextMeta.templateFileUrl = downloadUrl;
        nextMeta.templateFileId = meta.id;
        nextMeta.templateFileName = meta.filename;
        nextMeta.templateContentType = meta.contentType;
      } else {
        nextMeta.reportFileUrl = downloadUrl;
        nextMeta.reportFileId = meta.id;
        nextMeta.reportFileName = meta.filename;
        nextMeta.reportContentType = meta.contentType;
      }
      const updated = await client.templates.updateTemplate(tmpl.id, {
        title: tmpl.title,
        status: kind === "templateFile" ? TemplateEntityStatus.CDB_ADDED : tmpl.status,
        metadata: nextMeta,
      });
      const nextList = templates?.map((t) =>
        t.id === tmpl.id ? { ...t, ...updated.data, metadata: metadataToString(updated.data.metadata) } : t
      );
      templatesSet(nextList ?? null);
    } catch (e) {
      console.error(e);
      alert("Не удалось загрузить файл");
    }
  }

  if (!templates) {
    return <span>Loading...</span>;
  } else {
    return (
      <>
        <div className={styles.funcs}>
          <button
            onClick={() => navigate(`/templates/create?case_id=${caze.id}`)}
            disabled={!perms.templateAdd}
          >
            Create
          </button>
          {!perms.templateAdd ? (
            <span className={styles.hint}>
              Нет прав{canViewDebug ? ` (CASES:${caze.id}:TEMPLATES_ADD)` : ""}
            </span>
          ) : null}
        </div>
        <table className={styles.table}>
          <thead>
            <tr className={styles.row}>
              <th>Title</th>
              <th>Status</th>
              <th>Metadata</th>
              <th>Files</th>
              <th>Options</th>
            </tr>
          </thead>
          <tbody>
            {Array.from(templates ?? [])
              .sort((a, b) => b.id - a.id)
              .map((template) => (
                <tr className={styles.row} key={template.id}>
                  <td>
                    <input
                      value={template.title}
                      disabled={!perms.templateUpdate}
                      onChange={(e) => {
                        template.title = e.target.value;
                        templatesSet([...templates]);
                      }}
                    />
                    {/* open link removed per requirement */}
                  </td>
                  <td>
                    <select
                      value={template.status}
                      disabled={!perms.templateUpdate}
                      onChange={(e) => {
                        template.status = e.target.value as TemplateEntityStatus;
                        templatesSet([...templates]);
                      }}
                    >
                      {Object.values(TemplateEntityStatus).map((s) => (
                        <option key={s} value={s}>
                          {s}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <div className={styles.metaCell}>
                      <pre className={styles.metaPreview}>{metadataPreview(template.metadata)}</pre>
                      <button
                        type="button"
                        onClick={() =>
                          setMetadataModal({
                            id: template.id,
                            value: metadataToString(template.metadata),
                          })
                        }
                      >
                        View
                      </button>
                    </div>
                  </td>
                  <td>
                    {(() => {
                      const meta = normalizeTemplateMetadata(template.metadata);
                      const hasTemplateFile = !!meta?.templateFileUrl || !!meta?.templateFileId || !!template.cdbId;
                      const hasReportFile = !!meta?.reportFileUrl || !!meta?.reportFileId;
                      return (
                        <div className={styles.filesCol}>
                          <div className={styles.fileRow}>
                            <div className={styles.small}>Template</div>
                            {hasTemplateFile ? (
                              <button
                                type="button"
                                onClick={() => {
                                  if (meta?.templateFileUrl) {
                                    downloadFile(meta.templateFileUrl, meta.templateFileName);
                                  } else if (meta?.templateFileId) {
                                    downloadFile(`/api/files/${meta.templateFileId}/download`, meta.templateFileName);
                                  } else if (template.cdbId) {
                                    downloadFile(
                                      `/api/files/${template.cdbId}/download`,
                                      meta?.templateFileName || `template_${template.id}.md`
                                    );
                                  }
                                }}
                              >
                                Download
                              </button>
                            ) : (
                              <span className={styles.small}>—</span>
                            )}
                            {meta?.templateFileName ? (
                              <span className={styles.small}>{meta.templateFileName}</span>
                            ) : null}
                          </div>
                          <div className={styles.fileRow}>
                            <div className={styles.small}>Report</div>
                            {hasReportFile ? (
                              <button
                                type="button"
                                onClick={() => {
                                  if (meta?.reportFileUrl) {
                                    downloadFile(meta.reportFileUrl, meta.reportFileName);
                                  } else if (meta?.reportFileId) {
                                    downloadFile(`/api/files/${meta.reportFileId}/download`, meta.reportFileName);
                                  } else if (template.cdbId) {
                                    downloadFile(
                                      `/api/files/${template.cdbId}/download`,
                                      meta?.reportFileName || `report_${template.id}`
                                    );
                                  }
                                }}
                              >
                                Download
                              </button>
                            ) : (
                              <span className={styles.small}>—</span>
                            )}
                            {meta?.reportFileName ? (
                              <span className={styles.small}>{meta.reportFileName}</span>
                            ) : null}
                            {perms.templateUpdate ? (
                              <input
                                type="file"
                                onChange={(e) => {
                                  const f = e.target.files?.[0];
                                  if (f) uploadFileForTemplate(template, "reportFile", f);
                                }}
                              />
                            ) : null}
                          </div>
                        </div>
                      );
                    })()}
                  </td>
                  <td className={styles.rowActions}>
                    {template.status !== TemplateEntityStatus.PENDING ? (
                      <button onClick={download(template)}>Download</button>
                    ) : null}
                    {perms.templateUpdate ? (
                      <button
                        onClick={(_) => {
                          const parsed = normalizeTemplateMetadata(template.metadata);
                          client.templates
                            .updateTemplate(template.id, {
                              title: template.title,
                              status: template.status,
                              metadata: parsed,
                            })
                            .then((r) => {
                              const next = templates.map((t) =>
                                t.id === template.id
                                  ? { ...t, ...r.data, metadata: metadataToString(r.data.metadata) }
                                  : t
                              );
                              templatesSet(next);
                            })
                            .catch(console.error);
                        }}
                      >
                        Save
                      </button>
                    ) : null}
                    {perms.templateDelete ? (
                      <button
                        onClick={() => {
                          if (!confirm("Delete template?")) return;
                          client.templates
                            .deleteTemplate(template.id)
                            .then(() =>
                              templatesSet(templates.filter((t) => t.id !== template.id))
                            )
                            .catch(console.error);
                        }}
                      >
                        Delete
                      </button>
                    ) : null}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
        {metadataModal ? (
          <div className={styles.modalOverlay}>
            <div className={styles.modal}>
              <div className={styles.modalHeader}>
                <h4>Template metadata</h4>
                <button onClick={() => setMetadataModal(null)}>×</button>
              </div>
              <pre className={styles.modalPre}>{metadataModal.value}</pre>
              <div className={styles.modalActions}>
                <button onClick={() => setMetadataModal(null)}>Close</button>
              </div>
            </div>
          </div>
        ) : null}
      </>
    );
  }
}

// TODO: use search params instead
function Menu({
  caze,
  perms,
  canUpdateCase,
  caseUsers,
  setCaseUsers,
  canViewDebug,
}: {
  caze: CaseDto;
  perms: CasePermissions;
  canUpdateCase: boolean;
  caseUsers: Record<string, string>;
  setCaseUsers: (users: Record<string, string>) => void;
  canViewDebug: boolean;
}) {
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
            <Users
              caseUsers={caseUsers}
              setCaseUsers={setCaseUsers}
              canUpdateCase={canUpdateCase}
            />
          </div>
        ) : (
          <></>
        )}
        {menu == "clues" ? (
          <div>
            <h3>Clues</h3>
            <Clues caze={caze} perms={perms} canViewDebug={canViewDebug} />
          </div>
        ) : (
          <></>
        )}
        {menu == "suspects" ? (
          <div>
            <h3>Suspects</h3>
            <Suspects caze={caze} perms={perms} canViewDebug={canViewDebug} />
          </div>
        ) : (
          <></>
        )}
        {menu == "templates" ? (
          <div>
            <h3>Templates</h3>
            <Templates caze={caze} perms={perms} canViewDebug={canViewDebug} />
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
  const permissions = usePermissions((state) => state.permissions);
  const [title, titleSet] = useState("");
  const [description, descriptionSet] = useState("");
  const [status, statusSet] = useState<CaseEntityStatus>(CaseEntityStatus.OPENED);
  const [saving, savingSet] = useState(false);
  const [caseUsers, caseUsersSet] = useState<Record<string, string>>({});
  const [saveError, setSaveError] = useState<string | null>(null);
  const canViewDebug = !!permissions && hasPermission("LOGS:*:READ");
  const currentUser = useUser((state) => state.user);

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

  useEffect(() => {
    if (caze) {
      titleSet(caze.title);
      descriptionSet(caze.description ?? "");
      statusSet(caze.status);
      caseUsersSet(caze.users);
    }
  }, [caze]);

  if (!caze) {
    return <span>Loading...</span>;
  } else {
    const ownerId = caze.creatorId;
    const perms: CasePermissions = {
      clueAdd: !!permissions && hasPermission(`CASES:${caze.id}:CLUES_ADD`, ownerId),
      clueUpdate: !!permissions && hasPermission(`CASES:${caze.id}:CLUES_UPDATE`, ownerId),
      clueDelete: !!permissions && hasPermission(`CASES:${caze.id}:CLUES_DELETE`, ownerId),
      suspectAdd: !!permissions && hasPermission(`CASES:${caze.id}:SUSPECT_ADD`, ownerId),
      suspectUpdate: !!permissions && hasPermission(`CASES:${caze.id}:SUSPECT_UPDATE`, ownerId),
      suspectDelete: !!permissions && hasPermission(`CASES:${caze.id}:SUSPECT_DELETE`, ownerId),
      templateAdd: !!permissions && hasPermission(`CASES:${caze.id}:TEMPLATES_ADD`, ownerId),
      templateUpdate: !!permissions && hasPermission(`CASES:${caze.id}:TEMPLATES_UPDATE`, ownerId),
      templateDelete: !!permissions && hasPermission(`CASES:${caze.id}:TEMPLATES_DELETE`, ownerId),
    };
    const canUpdateCase =
      !!permissions && incident
        ? hasPermission(`INCIDENTS:${incident.id}:UPDATE`, incident.creatorId)
        : false;
    function saveCase() {
      if (!canUpdateCase) return;
      savingSet(true);
      setSaveError(null);
      const usersRecord: Record<string, string> = {};
      Object.entries(caseUsers).forEach(([id, pos]) => {
        if (pos) {
          usersRecord[id] = pos;
        }
      });
      client.incidents
        .updateCaseForIncident(caze.incidentId, {
          title,
          description,
          status,
          users: usersRecord,
        })
        .then((r) => {
          titleSet(r.data.title);
          descriptionSet(r.data.description ?? "");
          statusSet(r.data.status);
          caseUsersSet(r.data.users);
          cazeSet(r.data);
        })
        .catch((e: any) => {
          console.error("Save case error:", e);

          const status = e?.status ?? e?.response?.status;
          let msg = "";

          // 1. Сразу пробуем взять сырую строку из наших полей
          if (typeof e === "string") {
            msg = e;
          } else if (typeof e?.error === "string") {
            // http-client положил текст ошибки сюда
            msg = e.error;
          } else if (typeof e?.data === "string") {
            // вдруг сервер прислал текст, а не ошибку
            msg = e.data;
          } else if (typeof e?.rawBody === "string") {
            // наше дополнительное поле с raw body
            msg = e.rawBody;

          // 2. Классический JSON с _embedded.errors от Micronaut
          } else if (e?.error?._embedded?.errors) {
            msg = e.error._embedded.errors
              .map((ev: any) => ev.message)
              .join("\n");

          // 3. Если бэк вернул обычный JSON с message
          } else if (e?.error?.message) {
            msg = e.error.message;

          // 4. Fallback на message исключения
          } else if (e?.message) {
            msg = e.message;

          } else {
            msg = "Unknown error";
          }

          setSaveError("Failed to save case: " + msg);
        })
        .finally(() => savingSet(false));

    }

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
          <h3>Updated At (UTC)</h3>
          <span>{new Date(caze.updatedAt).toLocaleString()}</span>
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
        <div className={styles.card}>
          <h3>Case details</h3>
          <div className={styles.form}>
            <label>
              Title
              <input
                value={title}
                onChange={(e) => titleSet(e.target.value)}
                disabled={!canUpdateCase}
              />
            </label>
            <label>
              Description
              <textarea
                value={description}
                onChange={(e) => descriptionSet(e.target.value)}
                disabled={!canUpdateCase}
              />
            </label>
            <label>
              Status
              <select
                value={status}
                onChange={(e) => statusSet(e.target.value as CaseEntityStatus)}
                disabled={!canUpdateCase}
              >
                {Object.values(CaseEntityStatus).map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </label>
            <div className={styles.actions}>
              <button onClick={saveCase} disabled={!canUpdateCase || saving}>
                Save case
              </button>
              {!canUpdateCase ? (
                <span className={styles.hint}>
                  Нет прав{canViewDebug ? ` (INCIDENTS:${caze.incidentId}:UPDATE)` : ""}
                </span>
              ) : null}
              {saveError ? <span className={styles.error}>{saveError}</span> : null}
              {canViewDebug && incident ? (
                <div className={styles.debug}>
                  caseId: {caze.id}
                  {"\n"}incidentId: {incident.id}
                  {"\n"}caseOwnerId: {caze.creatorId}
                  {"\n"}incidentOwnerId: {incident.creatorId}
                  {"\n"}currentUserId: {currentUser.present ? currentUser.id : "n/a"}
                  {"\n"}permissions loaded: {String(!!permissions)}
                  {"\n"}perm pattern: INCIDENTS:{incident.id}:UPDATE
                  {"\n"}canUpdateCase: {String(canUpdateCase)}
                </div>
              ) : null}
            </div>
          </div>
        </div>
        <Menu
          caze={caze}
          perms={perms}
          canUpdateCase={canUpdateCase}
          caseUsers={caseUsers}
          setCaseUsers={caseUsersSet}
          canViewDebug={canViewDebug}
        />
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
