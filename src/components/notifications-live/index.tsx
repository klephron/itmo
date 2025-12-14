import { useEffect, useMemo } from "react";
import { Link } from "react-router";
import { NotificationDto, NotificationsControllerStatus } from "@api/gen/data-contracts";
import { useClient } from "@state/client";
import { useNotifications } from "@state/notifications";
import { useToken } from "@state/token";
import { useUser } from "@state/user";
import styles from "./index.module.scss";
import { formatUtc } from "@util";

const wsOverride = import.meta.env.VITE_WS_URL?.trim();

function buildWsUrl(baseUrl?: string, token?: string) {
  if (!token) {
    return null;
  }

  try {
    // If VITE_WS_URL is set (can be ws/wss/http/https, full path or base), prefer it.
    if (wsOverride) {
      // If override already includes a protocol, use as-is; otherwise treat as base.
      const hasProtocol = /^[a-zA-Z]+:\/\//.test(wsOverride);
      const url = hasProtocol ? new URL(wsOverride) : new URL(wsOverride, baseUrl);
      if (url.protocol === "http:") {
        url.protocol = "ws:";
      } else if (url.protocol === "https:") {
        url.protocol = "wss:";
      }
      if (!url.searchParams.has("token")) {
        url.searchParams.set("token", token);
      }
      return url.toString();
    }

    if (!baseUrl) {
      return null;
    }

    const url = new URL("/ws/notifications", baseUrl);
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
    url.searchParams.set("token", token);
    return url.toString();
  } catch (e) {
    console.error("Failed to build WS url", e);
    return null;
  }
}

function pickId(message: string) {
  try {
    const parsed = JSON.parse(message);
    if (parsed && typeof parsed.id !== "undefined") {
      return Number(parsed.id);
    }
  } catch {
    // not JSON
  }

  const match = message.match(/id\s*=\s*([0-9]+)/i);
  if (match) {
    return Number(match[1]);
  }
  return null;
}

function selectNewest(notifications: NotificationDto[]) {
  return notifications.reduce<NotificationDto | null>((latest, current) => {
    if (!latest) {
      return current;
    }
    return new Date(current.createdAt).getTime() > new Date(latest.createdAt).getTime()
      ? current
      : latest;
  }, null);
}

export default function NotificationsLive() {
  const client = useClient((state) => state.client);
  const token = useToken((state) => state.token);
  const user = useUser((state) => state.user);

  const unreadUpdate = useNotifications((state) => state.update);
  const unreadUpdateById = useNotifications((state) => state.updateById);
  const incomingQueue = useNotifications((state) => state.incomingQueue);
  const incomingPush = useNotifications((state) => state.pushIncoming);
  const incomingClear = useNotifications((state) => state.clearIncoming);

  const tokenPresent = token.present;
  const accessToken = token.accessToken;
  const userPresent = user.present;

  const wsUrl = useMemo(
    () => buildWsUrl(client.baseUrl, accessToken),
    [client.baseUrl, accessToken]
  );

  useEffect(() => {
    if (!tokenPresent || !userPresent) {
      incomingClear();
      return;
    }
    if (!wsUrl) {
      return;
    }

    let closed = false;
    let ws: WebSocket | null = null;
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

    const handlePayload = async (raw: MessageEvent["data"]) => {
      const payload = typeof raw === "string" ? raw : String(raw);
      const id = pickId(payload);

      if (id) {
        try {
          const r = await client.notifications.getNotification(id);
          unreadUpdateById(r.data);
          incomingPush(r.data);
          return;
        } catch (e) {
          console.error("Failed to fetch notification by id from WS payload", e);
        }
      }

      try {
        const r = await client.notifications.getNotifications({
          status: NotificationsControllerStatus.UNREAD,
        });
        unreadUpdate(r.data);
        r.data.forEach((n) => incomingPush(n));
      } catch (e) {
        console.error("Failed to refresh unread notifications after WS message", e);
      }
    };

    const connect = () => {
      if (import.meta.env.DEV) {
        console.log("[notifications-live] connecting", wsUrl);
      }
      ws = new WebSocket(wsUrl);

      ws.onmessage = (event) => {
        handlePayload(event.data);
      };
      ws.onopen = () => {
        if (import.meta.env.DEV) {
          console.log("[notifications-live] ws open");
        }
      };
      ws.onclose = () => {
        if (!closed) {
          if (import.meta.env.DEV) {
            console.log("[notifications-live] ws closed, reconnect in 5s");
          }
          reconnectTimer = setTimeout(connect, 5000);
        }
      };
      ws.onerror = (event) => {
        if (import.meta.env.DEV) {
          console.log("[notifications-live] ws error", event);
        }
        ws?.close();
      };
    };

    connect();

    return () => {
      closed = true;
      if (reconnectTimer) {
        clearTimeout(reconnectTimer);
      }
      ws?.close();
    };
  }, [
    client,
    incomingClear,
    incomingPush,
    tokenPresent,
    unreadUpdate,
    unreadUpdateById,
    userPresent,
    wsUrl,
  ]);

  const incoming = incomingQueue.length ? incomingQueue[0] : null;

  useEffect(() => {
    if (!incoming) {
      return;
    }

    const timer = setTimeout(() => incomingClear(incoming.id), 6000);
    return () => clearTimeout(timer);
  }, [incoming, incomingClear]);

  if (!incoming) {
    return null;
  }

  return (
    <div className={styles.toast}>
      <div className={styles.header}>
        <div className={styles.pill}>Новое уведомление</div>
        <span className={styles.time}>{formatUtc(incoming.createdAt)}</span>
      </div>
      <div className={styles.title}>{incoming.title}</div>
      {incoming.description ? (
        <div className={styles.description}>{incoming.description}</div>
      ) : (
        <div className={styles.descriptionMuted}>Без описания</div>
      )}
      <div className={styles.actions}>
        <Link to={`/notifications/${incoming.id}`} className={styles.link}>
          Открыть
        </Link>
        <button className={styles.dismiss} onClick={() => incomingClear(incoming.id)}>
          Закрыть
        </button>
      </div>
    </div>
  );
}
