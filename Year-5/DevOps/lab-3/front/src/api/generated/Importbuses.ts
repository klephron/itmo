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

import { ImportBusDto } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Importbuses<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * No description
   *
   * @tags import-bus-controller
   * @name GetImportBusById
   * @request GET:/importbuses/{id}
   */
  getImportBusById = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/importbuses/${id}`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags import-bus-controller
   * @name UpdateImportBus
   * @request PUT:/importbuses/{id}
   */
  updateImportBus = (
    id: number,
    data: ImportBusDto,
    params: RequestParams = {},
  ) =>
    this.request<object, any>({
      path: `/importbuses/${id}`,
      method: "PUT",
      body: data,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags import-bus-controller
   * @name DeleteImportBus
   * @request DELETE:/importbuses/{id}
   */
  deleteImportBus = (id: number, params: RequestParams = {}) =>
    this.request<void, any>({
      path: `/importbuses/${id}`,
      method: "DELETE",
      ...params,
    });
  /**
   * No description
   *
   * @tags import-bus-controller
   * @name GetAllImportBuses
   * @request GET:/importbuses
   */
  getAllImportBuses = (params: RequestParams = {}) =>
    this.request<ImportBusDto[], any>({
      path: `/importbuses`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags import-bus-controller
   * @name CreateImportBus
   * @request POST:/importbuses
   */
  createImportBus = (
    query: {
      /** @format int64 */
      networkId: number;
    },
    data: ImportBusDto,
    params: RequestParams = {},
  ) =>
    this.request<object, any>({
      path: `/importbuses`,
      method: "POST",
      query: query,
      body: data,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags import-bus-controller
   * @name ImportItems
   * @request POST:/importbuses/{id}/import
   */
  importItems = (
    id: number,
    query: {
      itemName: string;
      /** @format int32 */
      quantity: number;
    },
    params: RequestParams = {},
  ) =>
    this.request<object, any>({
      path: `/importbuses/${id}/import`,
      method: "POST",
      query: query,
      ...params,
    });
}
