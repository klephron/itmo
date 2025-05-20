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

import { StorageCellDto } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class StorageCells<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * No description
   *
   * @tags storage-cell-controller
   * @name GetStorageCellById
   * @request GET:/storageCells/{id}
   */
  getStorageCellById = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/storageCells/${id}`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags storage-cell-controller
   * @name UpdateStorageCell
   * @request PUT:/storageCells/{id}
   */
  updateStorageCell = (
    id: number,
    data: StorageCellDto,
    params: RequestParams = {},
  ) =>
    this.request<object, any>({
      path: `/storageCells/${id}`,
      method: "PUT",
      body: data,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags storage-cell-controller
   * @name DeleteStorageCell
   * @request DELETE:/storageCells/{id}
   */
  deleteStorageCell = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/storageCells/${id}`,
      method: "DELETE",
      ...params,
    });
  /**
   * No description
   *
   * @tags storage-cell-controller
   * @name GetAllStorageCells
   * @request GET:/storageCells
   */
  getAllStorageCells = (params: RequestParams = {}) =>
    this.request<StorageCellDto[], any>({
      path: `/storageCells`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags storage-cell-controller
   * @name CreateStorageCell
   * @request POST:/storageCells
   */
  createStorageCell = (
    query: {
      /** @format int64 */
      driveId: number;
    },
    data: StorageCellDto,
    params: RequestParams = {},
  ) =>
    this.request<object, any>({
      path: `/storageCells`,
      method: "POST",
      query: query,
      body: data,
      type: ContentType.Json,
      ...params,
    });
}
