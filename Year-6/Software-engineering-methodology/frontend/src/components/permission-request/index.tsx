import { useState } from "react";
import { useClient } from "@state/client";
import styles from "./index.module.scss";

type Props = {
  permission: string;
  reason?: string;
  message: string;
  showDebug?: boolean;
  className?: string;
  buttonLabel?: string;
};

export function PermissionRequestHint({
  permission,
  reason,
  message,
  showDebug,
  className,
  buttonLabel = "Request",
}: Props) {
  const client = useClient((state) => state.client);
  const [status, statusSet] = useState<"idle" | "sending" | "ok" | "error">("idle");
  const [info, infoSet] = useState<string | null>(null);

  const send = () => {
    statusSet("sending");
    infoSet(null);
    client.users
      .requestPermission({ permission, reason: reason ?? undefined })
      .then(() => {
        statusSet("ok");
        infoSet("Request sent");
      })
      .catch((e: any) => {
        statusSet("error");
        const msg =
          e?.rawBody ||
          e?.error?._embedded?.errors?.map((er: any) => er.message).join("\n") ||
          (typeof e?.error === "string" ? e.error : null) ||
          e?.error?.message ||
          e?.message ||
          "Failed to send request";
        infoSet(msg);
      });
  };

  return (
    <div className={`${styles.root} ${className ?? ""}`}>
      <span className={styles.message}>
        {message}
        {showDebug ? ` (${permission})` : ""}
      </span>
      <button type="button" onClick={send} disabled={status === "sending"}>
        {status === "sending" ? "Sending..." : buttonLabel}
      </button>
      <span className={styles.status}>
        {status === "ok" ? <span className={styles.small}>{info}</span> : null}
        {status === "error" ? <span className={styles.error}>{info}</span> : null}
      </span>
    </div>
  );
}
