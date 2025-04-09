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

import { ExportBusEntityDto } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Exportbuses<SecurityDataType = unknown> extends HttpClient<SecurityDataType> {
  /**
   * No description
   *
   * @tags export-bus-controller
   * @name GetExportBusById
   * @request GET:/exportbuses/{id}
   */
  getExportBusById = (id: number, params: RequestParams = {}) =>
    this.request<ExportBusEntityDto, any>({
      path: `/exportbuses/${id}`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags export-bus-controller
   * @name UpdateExportBus
   * @request PUT:/exportbuses/{id}
   */
  updateExportBus = (id: number, data: ExportBusEntityDto, params: RequestParams = {}) =>
    this.request<ExportBusEntityDto, any>({
      path: `/exportbuses/${id}`,
      method: "PUT",
      body: data,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags export-bus-controller
   * @name DeleteExportBus
   * @request DELETE:/exportbuses/{id}
   */
  deleteExportBus = (id: number, params: RequestParams = {}) =>
    this.request<void, any>({
      path: `/exportbuses/${id}`,
      method: "DELETE",
      ...params,
    });
  /**
   * No description
   *
   * @tags export-bus-controller
   * @name GetAllExportBuses
   * @request GET:/exportbuses
   */
  getAllExportBuses = (params: RequestParams = {}) =>
    this.request<ExportBusEntityDto[], any>({
      path: `/exportbuses`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags export-bus-controller
   * @name CreateExportBus
   * @request POST:/exportbuses
   */
  createExportBus = (
    query: {
      /** @format int64 */
      networkId: number;
    },
    data: ExportBusEntityDto,
    params: RequestParams = {},
  ) =>
    this.request<ExportBusEntityDto, any>({
      path: `/exportbuses`,
      method: "POST",
      query: query,
      body: data,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags export-bus-controller
   * @name ExportItems
   * @request POST:/exportbuses/{id}/export
   */
  exportItems = (
    id: number,
    query: {
      itemName: string;
      /** @format int32 */
      quantity: number;
    },
    params: RequestParams = {},
  ) =>
    this.request<number, any>({
      path: `/exportbuses/${id}/export`,
      method: "POST",
      query: query,
      ...params,
    });
}
