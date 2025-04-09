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

import { PatternEntityDto } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Patterns<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * No description
   *
   * @tags pattern-controller
   * @name GetPatternById
   * @request GET:/patterns/{id}
   */
  getPatternById = (id: number, params: RequestParams = {}) =>
    this.request<PatternEntityDto, any>({
      path: `/patterns/${id}`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags pattern-controller
   * @name UpdatePattern
   * @request PUT:/patterns/{id}
   */
  updatePattern = (
    id: number,
    data: PatternEntityDto,
    params: RequestParams = {},
  ) =>
    this.request<PatternEntityDto, any>({
      path: `/patterns/${id}`,
      method: "PUT",
      body: data,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags pattern-controller
   * @name DeletePattern
   * @request DELETE:/patterns/{id}
   */
  deletePattern = (id: number, params: RequestParams = {}) =>
    this.request<void, any>({
      path: `/patterns/${id}`,
      method: "DELETE",
      ...params,
    });
  /**
   * No description
   *
   * @tags pattern-controller
   * @name GetAllPatterns
   * @request GET:/patterns
   */
  getAllPatterns = (params: RequestParams = {}) =>
    this.request<PatternEntityDto[], any>({
      path: `/patterns`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags pattern-controller
   * @name CreatePattern
   * @request POST:/patterns
   */
  createPattern = (data: PatternEntityDto, params: RequestParams = {}) =>
    this.request<PatternEntityDto, any>({
      path: `/patterns`,
      method: "POST",
      body: data,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags pattern-controller
   * @name CraftPattern
   * @request POST:/patterns/{id}/craft
   */
  craftPattern = (
    id: number,
    query: {
      /** @format int32 */
      times: number;
    },
    params: RequestParams = {},
  ) =>
    this.request<void, any>({
      path: `/patterns/${id}/craft`,
      method: "POST",
      query: query,
      ...params,
    });
}
