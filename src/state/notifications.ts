import { NotificationDto, NotificationsControllerStatus } from "@api/gen/data-contracts";
import { create } from "zustand";
import { combine, subscribeWithSelector } from "zustand/middleware";
import { usePermissions } from "./permissions";
import { useUser } from "./user";
import { useClient } from "./client";

export type Notifications = Map<number, NotificationDto> | null;
export type NotificationQueue = NotificationDto[];

function toMap(notifications: NotificationDto[]) {
  return new Map(
    notifications.map((notification) => [notification.id, notification as NotificationDto])
  );
}

function updateById(notification: NotificationDto) {
  const unread = useNotifications.getState().unread;
  const unread1 = new Map(unread ?? []);

  if (notification.readAt) {
    unread1.delete(notification.id);
  } else {
    unread1.set(notification.id, notification);
  }
  return { unread: unread1 };
}

export const useNotifications = create(
  subscribeWithSelector(
    combine(
      { unread: null as Notifications, incomingQueue: [] as NotificationQueue },
      (set, get) => ({
        update: (notifications: NotificationDto[]) => set({ unread: toMap(notifications) }),
        updateById: (notification: NotificationDto) => set(updateById(notification)),
        pushIncoming: (notification: NotificationDto) =>
          set((state) => {
            const exists = state.incomingQueue.some((n) => n.id === notification.id);
            if (exists) return {};
            return { incomingQueue: [...state.incomingQueue, notification] };
          }),
        clearIncoming: (id?: number) =>
          set((state) => {
            if (!state.incomingQueue.length) return {};
            if (id) {
              const filtered = state.incomingQueue.filter((n) => n.id !== id);
              return { incomingQueue: filtered };
            }
            // pop first
            return { incomingQueue: state.incomingQueue.slice(1) };
          }),
        reset: () => set({ unread: null, incomingQueue: [] }),
      })
    )
  )
);

usePermissions.subscribe(
  (state) => state.permissions,
  (permissions) => {
    const user = useUser.getState().user;
    const client = useClient.getState().client;
    const notificationUpdate = useNotifications.getState().update;
    const notificationReset = useNotifications.getState().reset;

    if (permissions === null || !user.present || !client.authorization) {
      notificationReset();
      return;
    }

    client.notifications
      .getNotifications({
        status: NotificationsControllerStatus.UNREAD,
      })
      .then((r) => {
        notificationUpdate(r.data);
      })
      .catch((e) => {
        console.error(e);
      });
  },
  { fireImmediately: true }
);

useNotifications.subscribe(
  (state) => state.unread,
  (notifications) => {
    if (import.meta.env.DEV) {
      console.log("[state.notifications]: ", notifications);
    }
  },
  { fireImmediately: true }
);
