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

import { LogEntryDto, PageLogEntryDto } from "./data-contracts";
import { HttpClient, RequestParams } from "./http-client";

export class Logs<SecurityDataType = unknown> extends HttpClient<SecurityDataType> {
  /**
   * @description Получить страницу логов с фильтрами
   *
   * @tags logs
   * @name GetLogs
   * @summary Получить логи
   * @request GET:/api/logs
   * @secure
   */
  getLogs = (
    query?: {
      /** @default 0 */
      page?: number;
      /** @default 50 */
      size?: number;
      level?: string | null;
      logger?: string | null;
      message?: string | null;
      /** @format date-time */
      from?: string | null;
      /** @format date-time */
      to?: string | null;
    },
    params: RequestParams = {},
  ) =>
    this.request<PageLogEntryDto, any>({
      path: `/api/logs`,
      method: "GET",
      query: query,
      secure: true,
      format: "json",
      ...params,
    });
}
