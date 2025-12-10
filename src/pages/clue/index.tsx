import { useEffect, useState } from "react";
import { Link } from "react-router";
import styles from "./index.module.scss";
import { useClient } from "@state/client";
import { useToken } from "@state/token";
import { ClueDto, ClueMetadata, FileMetaResponse } from "@api/gen/data-contracts";
import { Route } from "~react-router/src/pages/clue/+types";
import { exportToFile } from "@util";

type Attachment = {
  uri: string;
  contentType?: string;
  filename?: string;
  previewUrl?: string;
};

function buildAbsolute(baseUrl: string | undefined, uri: string) {
  if (uri.startsWith("http://") || uri.startsWith("https://")) return uri;
  if (!baseUrl) return uri;
  if (uri.startsWith("/api")) return `${baseUrl}${uri}`;
  return `${baseUrl}/api${uri}`;
}

async function fetchBlobUrl(url: string, authHeader?: string) {
  const res = await fetch(url, {
    headers: authHeader ? { Authorization: authHeader } : undefined,
  });
  if (!res.ok) throw new Error(`Failed to fetch blob ${res.status}`);
  const blob = await res.blob();
  const objUrl = URL.createObjectURL(blob);
  return { url: objUrl, type: blob.type };
}

export default function CluePage({ params }: Route.ComponentProps) {
  const client = useClient((state) => state.client);
  const token = useToken((state) => state.token);
  const authHeader =
    token.present && token.accessToken ? `${token.tokenType} ${token.accessToken}` : undefined;

  const [clue, clueSet] = useState<ClueDto | null>(null);
  const [attachments, attachmentsSet] = useState<Attachment[]>([]);
  const [error, errorSet] = useState<string | null>(null);

  useEffect(() => {
    client.clues
      .getClue(Number(params.id))
      .then((r) => clueSet(r.data))
      .catch((e) => {
        const status = (e?.response && e.response.status) || e?.status;
        if (status === 401 || status === 403) {
          errorSet("Нет прав на просмотр");
        } else {
          console.error(e);
          errorSet("Failed to load clue");
        }
      });
  }, [client, params.id]);

  useEffect(() => {
    async function loadAttachments(metadata?: ClueMetadata | null) {
      if (!metadata) return;
      const result: Attachment[] = [];
      (metadata.imageUris ?? []).forEach((u) =>
        result.push({ uri: buildAbsolute(client.baseUrl, u), contentType: "image/*" })
      );
      (metadata.videoUris ?? []).forEach((u) =>
        result.push({ uri: buildAbsolute(client.baseUrl, u), contentType: "video/*" })
      );
      (metadata.reportUris ?? []).forEach((u) =>
        result.push({ uri: buildAbsolute(client.baseUrl, u) })
      );

      const enhanced: Attachment[] = [];
      for (const att of result) {
        if (att.contentType?.startsWith("image")) {
          try {
            const blob = await fetchBlobUrl(att.uri, authHeader);
            enhanced.push({ ...att, previewUrl: blob.url, contentType: blob.type || att.contentType });
            continue;
          } catch (e) {
            console.error(e);
          }
        }
        enhanced.push(att);
      }
      attachmentsSet(enhanced);
    }
    loadAttachments(clue?.metadata as ClueMetadata | undefined);
  }, [clue, client.baseUrl, authHeader]);

  async function viewAttachment(att: Attachment) {
    try {
      const blob = await fetchBlobUrl(att.uri, authHeader);
      const url = blob.url;
      window.open(url, "_blank");
    } catch (e) {
      console.error(e);
      alert("Нет прав на просмотр файла или файл недоступен");
    }
  }

  async function downloadAttachment(att: Attachment) {
    try {
      const res = await fetch(att.uri, {
        headers: authHeader ? { Authorization: authHeader } : undefined,
      });
      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          throw new Error("Нет прав на скачивание файла");
        }
        throw new Error(`Download failed ${res.status}`);
      }
      const blob = await res.blob();
      exportToFile(blob, blob.type || "application/octet-stream", att.filename || "file");
    } catch (e) {
      console.error(e);
      alert(e instanceof Error ? e.message : "Download failed");
    }
  }

  if (error) {
    return (
      <div className={styles.root}>
        <h2>Clue</h2>
        <span className={styles.hint}>{error}</span>
      </div>
    );
  }

  if (!clue) return <span>Loading...</span>;

  return (
    <div className={styles.root}>
      <h2>Clue</h2>
      <div>
        <h3>Title</h3>
        <span>{clue.title}</span>
      </div>
      <div>
        <h3>Status</h3>
        <span>{clue.status}</span>
      </div>
      <div>
        <h3>Description</h3>
        <span>{clue.description ?? "-"}</span>
      </div>
      <div>
        <h3>Related case</h3>
        <Link to={`/cases/${clue.relatedCaseId}`}>Case #{clue.relatedCaseId}</Link>
      </div>
      <div>
        <h3>Created / Updated</h3>
        <span>
          {new Date(clue.createdAt).toLocaleString()} / {new Date(clue.updatedAt).toLocaleString()}
        </span>
      </div>

      <div>
        <h3>Attachments</h3>
        <div className={styles.files}>
          {attachments.length === 0 ? <span className={styles.hint}>No attachments</span> : null}
          {attachments.map((att) => (
            <div className={styles.fileCard} key={att.id ?? att.inlineUrl}>
              <div>
                <strong>{att.filename ?? att.id ?? "file"}</strong>{" "}
                {att.contentType ? `(${att.contentType})` : ""}
              </div>
              {att.previewUrl ? <img src={att.previewUrl} className={styles.preview} /> : null}
              <div className={styles.actions}>
                <button type="button" onClick={() => viewAttachment(att)}>
                  View
                </button>
                <button type="button" onClick={() => downloadAttachment(att)}>
                  Download
                </button>
                <a href={att.inlineUrl} target="_blank" rel="noreferrer">
                  Raw
                </a>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
