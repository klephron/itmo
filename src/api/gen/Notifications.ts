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

import { NotificationsControllerStatus } from "./data-contracts";
import { HttpClient, RequestParams } from "./http-client";

export class Notifications<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * @description Get notifications
   *
   * @tags notifications
   * @name GetNotifications
   * @summary Get notifications
   * @request GET:/notifications
   * @secure
   */
  getNotifications = (
    query?: {
      /** @format date-time */
      after?: string | null;
      status?: NotificationsControllerStatus | null;
      /** @format date-time */
      before?: string | null;
    },
    params: RequestParams = {},
  ) =>
    this.request<object, any>({
      path: `/notifications`,
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
   * @request GET:/notifications/{id}
   * @secure
   */
  getNotification = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/notifications/${id}`,
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
   * @request PUT:/notifications/{id}
   * @secure
   */
  updateNotification = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/notifications/${id}`,
      method: "PUT",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Delete notification
   *
   * @tags notifications
   * @name DeleteNotification
   * @summary Delete notification
   * @request DELETE:/notifications/{id}
   * @secure
   */
  deleteNotification = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/notifications/${id}`,
      method: "DELETE",
      secure: true,
      format: "json",
      ...params,
    });
}
