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

import { CellItemEntityDto } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class CellItems<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * No description
   *
   * @tags cell-item-controller
   * @name GetCellItemById
   * @request GET:/cellItems/{id}
   */
  getCellItemById = (id: number, params: RequestParams = {}) =>
    this.request<CellItemEntityDto, any>({
      path: `/cellItems/${id}`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags cell-item-controller
   * @name UpdateCellItem
   * @request PUT:/cellItems/{id}
   */
  updateCellItem = (
    id: number,
    data: CellItemEntityDto,
    params: RequestParams = {},
  ) =>
    this.request<CellItemEntityDto, any>({
      path: `/cellItems/${id}`,
      method: "PUT",
      body: data,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags cell-item-controller
   * @name DeleteCellItem
   * @request DELETE:/cellItems/{id}
   */
  deleteCellItem = (id: number, params: RequestParams = {}) =>
    this.request<void, any>({
      path: `/cellItems/${id}`,
      method: "DELETE",
      ...params,
    });
  /**
   * No description
   *
   * @tags cell-item-controller
   * @name GetAllCellItems
   * @request GET:/cellItems
   */
  getAllCellItems = (params: RequestParams = {}) =>
    this.request<CellItemEntityDto[], any>({
      path: `/cellItems`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags cell-item-controller
   * @name CreateCellItem
   * @request POST:/cellItems
   */
  createCellItem = (
    query: {
      /** @format int64 */
      storageCellId: number;
    },
    data: CellItemEntityDto,
    params: RequestParams = {},
  ) =>
    this.request<CellItemEntityDto, any>({
      path: `/cellItems`,
      method: "POST",
      query: query,
      body: data,
      type: ContentType.Json,
      ...params,
    });
}
