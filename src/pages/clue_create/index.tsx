import { useClient } from "@state/client";
import styles from "./index.module.scss";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { catchResponse, exportToFile } from "@util";
import { ClueEntityStatus, ClueMetadata, FileMetaResponse } from "@api/gen/data-contracts";
import { useToken } from "@state/token";
import { hasPermission, usePermissions } from "@state/permissions";

export default function ClueCreatePage() {
  const navigate = useNavigate();
  const [search, _searchSet] = useSearchParams();
  const client = useClient((state) => state.client);
  const token = useToken((state) => state.token);
  const permissions = usePermissions((state) => state.permissions);
  const canViewDebug = permissions ? hasPermission("LOGS:*:READ") : false;

  const [caseId, _caseIdSet] = useState(search.get("case_id"));
  const [caseOwnerId, caseOwnerIdSet] = useState<number | null>(null);
  const [ownerLoading, ownerLoadingSet] = useState<boolean>(true);

  const [title, titleSet] = useState("");
  const [description, descriptionSet] = useState("");
  const [files, filesSet] = useState<FileMetaResponse[]>([]);
  const [uploading, uploadingSet] = useState(false);
  const [uploadError, uploadErrorSet] = useState<string | null>(null);
  const canUpdateCase =
    permissions && caseId && !ownerLoading
      ? hasPermission(`CASES:${caseId}:CLUES_UPDATE`, caseOwnerId ?? undefined)
      : false;
  const canUpload =
    permissions && caseId && !ownerLoading ? hasPermission("FILES:*:UPLOAD") && canUpdateCase : false;
  const canAddClue =
    permissions && caseId && !ownerLoading
      ? hasPermission(`CASES:${caseId}:CLUES_ADD`, caseOwnerId ?? undefined)
      : false;

  const metadata: ClueMetadata = useMemo(() => {
    const imageUris: string[] = [];
    const videoUris: string[] = [];
    const reportUris: string[] = [];
    files.forEach((f) => {
      if (f.contentType.startsWith("image/")) {
        imageUris.push(f.inlineUrl);
      } else if (f.contentType.startsWith("video/")) {
        videoUris.push(f.inlineUrl);
      } else {
        reportUris.push(f.downloadUrl);
      }
    });
    return {
      imageUris: imageUris.length ? imageUris : undefined,
      videoUris: videoUris.length ? videoUris : undefined,
      reportUris: reportUris.length ? reportUris : undefined,
    };
  }, [files]);

  const authHeader =
    token.present && token.accessToken ? `${token.tokenType} ${token.accessToken}` : undefined;

  useEffect(() => {
    if (!caseId) return;
    console.log("[clue-create] fetching case for ownerId...");
    client.cases
      .getCase(Number(caseId))
      .then((r) => {
        caseOwnerIdSet(r.data.creatorId);
        ownerLoadingSet(false);
        console.log("[clue-create] case loaded", {
          caseId,
          creatorId: r.data.creatorId,
        });
      })
      .catch((e) => {
        console.error(e);
        caseOwnerIdSet(null);
        ownerLoadingSet(false);
        console.log("[clue-create] failed to load case", { caseId, error: e });
      });
  }, [caseId, client]);

  useEffect(() => {
    console.log("[clue-create] perms check", {
      caseId,
      ownerLoading,
      caseOwnerId,
      hasPermissionsLoaded: !!permissions,
      canAddClue,
      canUpdateCase,
      canUpload,
    });
  }, [caseId, ownerLoading, caseOwnerId, permissions, canAddClue, canUpdateCase, canUpload]);

  async function fetchBlobUrl(url: string) {
    const res = await fetch(url, {
      headers: authHeader ? { Authorization: authHeader } : undefined,
    });
    if (!res.ok) throw new Error(`Failed to fetch blob: ${res.status}`);
    const blob = await res.blob();
    return URL.createObjectURL(blob);
  }

  async function uploadFile(file: File) {
    if (!canUpload) {
      uploadErrorSet("No permission to upload files (FILES:*:UPLOAD)");
      return;
    }
    if (!client.baseUrl) {
      uploadErrorSet("No baseUrl for upload");
      return;
    }
    if (!authHeader) {
      uploadErrorSet("Not authenticated");
      return;
    }
    uploadingSet(true);
    uploadErrorSet(null);
    try {
      const formData = new FormData();
      formData.append("file", file);
      const res = await fetch(`${client.baseUrl}/api/files`, {
        method: "POST",
        headers: {
          Authorization: authHeader,
        },
        body: formData,
      });
      if (!res.ok) {
        throw new Error(`Upload failed: ${res.status}`);
      }
      const idResp = (await res.json()) as { id: string };
      const metaRes = await fetch(`${client.baseUrl}/api/files/${idResp.id}/meta`, {
        headers: {
          Authorization: authHeader,
        },
      });
      if (!metaRes.ok) {
        throw new Error(`Meta failed: ${metaRes.status}`);
      }
      const meta = (await metaRes.json()) as FileMetaResponse;
      const withBase: FileMetaResponse & { previewUrl?: string } = {
        ...meta,
        inlineUrl: client.baseUrl ? `${client.baseUrl}/api${meta.inlineUrl}` : meta.inlineUrl,
        downloadUrl: client.baseUrl ? `${client.baseUrl}/api${meta.downloadUrl}` : meta.downloadUrl,
      };
      if (meta.contentType.startsWith("image/")) {
        withBase.previewUrl = await fetchBlobUrl(withBase.inlineUrl);
      }
      filesSet((prev) => [...prev, withBase]);
    } catch (err: any) {
      console.error(err);
      uploadErrorSet(err?.message || "Upload error");
    } finally {
      uploadingSet(false);
    }
  }

  async function viewFile(file: FileMetaResponse & { previewUrl?: string }) {
    const url = file.previewUrl || (await fetchBlobUrl(file.inlineUrl));
    window.open(url, "_blank");
  }

  async function downloadFile(file: FileMetaResponse) {
    const res = await fetch(file.downloadUrl, {
      headers: authHeader ? { Authorization: authHeader } : undefined,
    });
    if (!res.ok) return;
    const blob = await res.blob();
    exportToFile(blob, blob.type || "application/octet-stream", file.filename);
  }

  function createClue() {
    if (ownerLoading) {
      alert("Checking permissions...");
      return;
    }
    if (!canAddClue) {
      alert(`No permission (CASES:${caseId}:CLUES_ADD)`);
      return;
    }
    try {
      client.cases
        .addCaseClue(Number(caseId), {
          title: title,
          description: description,
          status: ClueEntityStatus.PENDING,
        })
        .then((r) => {
          const id = (r.data as any)?.id;
          if (id) {
            client.clues
              .updateClue(id, {
                title: title || undefined,
                metadata: metadata || {},
              })
              .catch(catchResponse);
          }
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
      {!canAddClue && !ownerLoading ? (
        <span className={styles.error}>
          No permission{canViewDebug ? ` (CASES:${caseId}:CLUES_ADD)` : ""}
        </span>
      ) : null}
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
          <h3>Attachments</h3>
          <input
            type="file"
            accept="*/*"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) uploadFile(file);
            }}
            disabled={uploading || !canUpload}
          />
          {!canUpload ? (
            <span className={styles.error}>No permission (FILES:*:UPLOAD)</span>
          ) : null}
          {uploadError ? <span className={styles.error}>{uploadError}</span> : null}
          <div className={styles.files}>
            {files.map((f) => (
              <div key={f.id} className={styles.file}>
                <div>
                  <strong>{f.filename}</strong> ({f.contentType}, {(f.sizeBytes / 1024).toFixed(1)}{" "}
                  KB)
                </div>
                {f.contentType.startsWith("image/") ? (
                  <img src={f.previewUrl || f.inlineUrl} alt={f.filename} className={styles.preview} />
                ) : null}
                <div className={styles.actions}>
                  <button type="button" onClick={() => viewFile(f)}>
                    View
                  </button>
                  <button type="button" onClick={() => downloadFile(f)}>
                    Download
                  </button>
                  <button
                    type="button"
                    onClick={() => filesSet((prev) => prev.filter((x) => x.id !== f.id))}
                  >
                    Remove
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
        <button
          onClick={(e) => {
            e.preventDefault();
            createClue();
          }}
          disabled={!canAddClue || ownerLoading}
        >
          Submit
        </button>
        {canViewDebug ? (
          <div className={styles.debug}>
            ownerLoading: {String(ownerLoading)}
            {"\n"}caseId: {caseId}
            {"\n"}caseOwnerId: {caseOwnerId ?? "null"}
            {"\n"}permissions loaded: {String(!!permissions)}
            {"\n"}canAddClue: {String(canAddClue)}
            {"\n"}canUpdateCase: {String(canUpdateCase)}
            {"\n"}canUpload: {String(canUpload)}
          </div>
        ) : null}
      </form>
    </div>
  );
}
