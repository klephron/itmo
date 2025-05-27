import { NotificationDto, NotificationsControllerStatus } from "@api/gen/data-contracts";
import { create } from "zustand";
import { combine, subscribeWithSelector } from "zustand/middleware";
import { usePermissions } from "./permissions";
import { useUser } from "./user";
import { useClient } from "./client";

export type Notifications = Map<number, NotificationDto> | null;

function toMap(notifications: NotificationDto[]) {
  return new Map(
    notifications.map((notification) => [notification.id, notification as NotificationDto])
  );
}

function updateById(notification: NotificationDto) {
  const unread = useNotifications.getState().unread;
  if (!unread) {
    return {};
  }

  const unread1 = new Map(unread);

  if (notification.readAt) {
    unread1.delete(notification.id);
  } else {
    unread1.set(notification.id, notification);
  }
  return { unread: unread1 };
}

export const useNotifications = create(
  subscribeWithSelector(
    combine({ unread: null as Notifications }, (set) => ({
      update: (notifications: NotificationDto[]) => set({ unread: toMap(notifications) }),
      updateById: (notification: NotificationDto) => set(updateById(notification)),
      reset: () => set({ unread: null }),
    }))
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
