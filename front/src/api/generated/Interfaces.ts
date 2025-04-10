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

import { MeInterfaceDto } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Interfaces<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * No description
   *
   * @tags me-interface-controller
   * @name GetInterfaceById
   * @request GET:/interfaces/{id}
   */
  getInterfaceById = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/interfaces/${id}`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags me-interface-controller
   * @name UpdateInterface
   * @request PUT:/interfaces/{id}
   */
  updateInterface = (
    id: number,
    data: MeInterfaceDto,
    params: RequestParams = {},
  ) =>
    this.request<object, any>({
      path: `/interfaces/${id}`,
      method: "PUT",
      body: data,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags me-interface-controller
   * @name DeleteInterface
   * @request DELETE:/interfaces/{id}
   */
  deleteInterface = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/interfaces/${id}`,
      method: "DELETE",
      ...params,
    });
  /**
   * No description
   *
   * @tags me-interface-controller
   * @name GetAllInterfaces
   * @request GET:/interfaces
   */
  getAllInterfaces = (params: RequestParams = {}) =>
    this.request<MeInterfaceDto[], any>({
      path: `/interfaces`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags me-interface-controller
   * @name CreateInterface
   * @request POST:/interfaces
   */
  createInterface = (
    query: {
      /** @format int64 */
      networkId: number;
    },
    data: MeInterfaceDto,
    params: RequestParams = {},
  ) =>
    this.request<object, any>({
      path: `/interfaces`,
      method: "POST",
      query: query,
      body: data,
      type: ContentType.Json,
      ...params,
    });
}
