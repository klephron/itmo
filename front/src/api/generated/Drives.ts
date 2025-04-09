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

import { DriveEntityDto } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Drives<SecurityDataType = unknown> extends HttpClient<SecurityDataType> {
  /**
   * No description
   *
   * @tags drive-controller
   * @name GetDriveById
   * @request GET:/drives/{id}
   */
  getDriveById = (id: number, params: RequestParams = {}) =>
    this.request<DriveEntityDto, any>({
      path: `/drives/${id}`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags drive-controller
   * @name UpdateDrive
   * @request PUT:/drives/{id}
   */
  updateDrive = (id: number, data: DriveEntityDto, params: RequestParams = {}) =>
    this.request<DriveEntityDto, any>({
      path: `/drives/${id}`,
      method: "PUT",
      body: data,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags drive-controller
   * @name DeleteDrive
   * @request DELETE:/drives/{id}
   */
  deleteDrive = (id: number, params: RequestParams = {}) =>
    this.request<void, any>({
      path: `/drives/${id}`,
      method: "DELETE",
      ...params,
    });
  /**
   * No description
   *
   * @tags drive-controller
   * @name GetAllDrives
   * @request GET:/drives
   */
  getAllDrives = (params: RequestParams = {}) =>
    this.request<DriveEntityDto[], any>({
      path: `/drives`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags drive-controller
   * @name CreateDrive
   * @request POST:/drives
   */
  createDrive = (
    query: {
      /** @format int64 */
      networkId: number;
    },
    data: DriveEntityDto,
    params: RequestParams = {},
  ) =>
    this.request<DriveEntityDto, any>({
      path: `/drives`,
      method: "POST",
      query: query,
      body: data,
      type: ContentType.Json,
      ...params,
    });
}
