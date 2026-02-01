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

import { CaseSuspectDto, UpdateCaseSuspectRequest } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Suspects<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * No description
   *
   * @name GetCaseSuspect
   * @request GET:/api/suspects/{id}
   * @secure
   */
  getCaseSuspect = (id: number, params: RequestParams = {}) =>
    this.request<CaseSuspectDto, string>({
      path: `/api/suspects/${id}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * No description
   *
   * @name UpdateCaseSuspect
   * @request POST:/api/suspects/{id}
   * @secure
   */
  updateCaseSuspect = (
    id: number,
    data: UpdateCaseSuspectRequest,
    params: RequestParams = {},
  ) =>
    this.request<object, any>({
      path: `/api/suspects/${id}`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * No description
   *
   * @name DeleteCaseSuspect
   * @request DELETE:/api/suspects/{id}
   * @secure
   */
  deleteCaseSuspect = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/api/suspects/${id}`,
      method: "DELETE",
      secure: true,
      format: "json",
      ...params,
    });
}
