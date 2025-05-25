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

import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Clues<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * @description Get clue
   *
   * @tags clues
   * @name GetClue
   * @summary Get clue
   * @request GET:/clues/{id}
   * @secure
   */
  getClue = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/clues/${id}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Update clue (for example approve)
   *
   * @tags clues
   * @name UpdateClue
   * @summary Update clue (for example approve)
   * @request PUT:/clues/{id}
   * @secure
   */
  updateClue = (id: number, data: object, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/clues/${id}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Delete clue
   *
   * @tags clues
   * @name DeleteClue
   * @summary Delete clue
   * @request DELETE:/clues/{id}
   * @secure
   */
  deleteClue = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/clues/${id}`,
      method: "DELETE",
      secure: true,
      format: "json",
      ...params,
    });
}
