import { useEffect, useMemo } from "react";
import { Link } from "react-router";
import { NotificationDto, NotificationsControllerStatus } from "@api/gen/data-contracts";
import { useClient } from "@state/client";
import { useNotifications } from "@state/notifications";
import { useToken } from "@state/token";
import { useUser } from "@state/user";
import styles from "./index.module.scss";
import { formatUtc } from "@util";

function buildWsUrl(baseUrl?: string, token?: string) {
  if (!baseUrl || !token) {
    return null;
  }

  const url = new URL("/ws/notifications", baseUrl);
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  url.searchParams.set("token", token);
  return url.toString();
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
      ws = new WebSocket(wsUrl);

      ws.onmessage = (event) => {
        handlePayload(event.data);
      };
      ws.onclose = () => {
        if (!closed) {
          reconnectTimer = setTimeout(connect, 5000);
        }
      };
      ws.onerror = () => {
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
