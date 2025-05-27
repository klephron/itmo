/* eslint-disable */
/* tslint:disable */
// @ts-nocheck
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

import {
  NotificationDto,
  NotificationsControllerStatus,
  UpdateNotificationRequest,
} from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Notifications<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * @description Возвращает список всех уведомлений. Можно фильтровать по статусу (прочитано/непрочитано/все) и диапазону дат.
   *
   * @tags notifications
   * @name GetNotifications
   * @summary Получить список уведомлений
   * @request GET:/api/notifications
   * @secure
   */
  getNotifications = (
    query?: {
      /**
       * Дата и время начала интервала (например, 2025-05-26T17:28:43)
       * @format date-time
       * @example "2025-05-26T17:28:43.000Z"
       */
      from?: string | null;
      /**
       * Дата и время конца интервала (например, 2025-05-26T17:48:43)
       * @format date-time
       * @example "2025-05-26T17:48:43.000Z"
       */
      to?: string | null;
      /**
       * Статус уведомлений для фильтрации
       * @example "ALL"
       */
      status?: NotificationsControllerStatus | null;
    },
    params: RequestParams = {},
  ) =>
    this.request<NotificationDto[], any>({
      path: `/api/notifications`,
      method: "GET",
      query: query,
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Возвращает уведомление по его идентификатору
   *
   * @tags notifications
   * @name GetNotification
   * @summary Получить уведомление
   * @request GET:/api/notifications/{id}
   * @secure
   */
  getNotification = (id: number, params: RequestParams = {}) =>
    this.request<NotificationDto, void>({
      path: `/api/notifications/${id}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Обновляет уведомление, например, устанавливает дату прочтения
   *
   * @tags notifications
   * @name UpdateNotification
   * @summary Обновить уведомление
   * @request PUT:/api/notifications/{id}
   * @secure
   */
  updateNotification = (
    id: number,
    data: UpdateNotificationRequest,
    params: RequestParams = {},
  ) =>
    this.request<NotificationDto, void>({
      path: `/api/notifications/${id}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Удаляет уведомление по идентификатору
   *
   * @tags notifications
   * @name DeleteNotification
   * @summary Удалить уведомление
   * @request DELETE:/api/notifications/{id}
   * @secure
   */
  deleteNotification = (id: number, params: RequestParams = {}) =>
    this.request<void, void>({
      path: `/api/notifications/${id}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
}
