import { NotificationDto, NotificationsControllerStatus } from "@api/gen/data-contracts";
import { create } from "zustand";
import { combine, subscribeWithSelector } from "zustand/middleware";
import { usePermissions } from "./permissions";
import { useUser } from "./user";
import { useClient } from "./client";

export type Notifications = NotificationDto[] | null;

export const useNotifications = create(
  subscribeWithSelector(
    combine({ notifications: null as Notifications }, (set) => ({
      update: (notifications: NotificationDto[]) => set({ notifications: notifications }),
      reset: () => set({ notifications: null }),
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
  (state) => state.notifications,
  (notifications) => {
    if (import.meta.env.DEV) {
      console.log("[state.notifications]: ", notifications);
    }
  },
  { fireImmediately: true }
);
