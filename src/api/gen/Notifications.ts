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
   * @description Get notifications
   *
   * @tags notifications
   * @name GetNotifications
   * @summary Get notifications
   * @request GET:/api/notifications
   * @secure
   */
  getNotifications = (
    query: {
      /** @format date-time */
      from: string;
      status: NotificationsControllerStatus;
      /** @format date-time */
      to: string;
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
   * @description Get notification
   *
   * @tags notifications
   * @name GetNotification
   * @summary Get notification
   * @request GET:/api/notifications/{id}
   * @secure
   */
  getNotification = (id: number, params: RequestParams = {}) =>
    this.request<NotificationDto, any>({
      path: `/api/notifications/${id}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Update notification (for example set date when read)
   *
   * @tags notifications
   * @name UpdateNotification
   * @summary Update notification (for example set date when read)
   * @request PUT:/api/notifications/{id}
   * @secure
   */
  updateNotification = (
    id: number,
    data: UpdateNotificationRequest,
    params: RequestParams = {},
  ) =>
    this.request<NotificationDto, any>({
      path: `/api/notifications/${id}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Delete notification
   *
   * @tags notifications
   * @name DeleteNotification
   * @summary Delete notification
   * @request DELETE:/api/notifications/{id}
   * @secure
   */
  deleteNotification = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/api/notifications/${id}`,
      method: "DELETE",
      secure: true,
      format: "json",
      ...params,
    });
}
