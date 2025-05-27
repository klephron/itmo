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

import { AddUpdateClueRequest, ClueDto } from "./data-contracts";
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
   * @request GET:/api/clues/{id}
   * @secure
   */
  getClue = (id: number, params: RequestParams = {}) =>
    this.request<ClueDto, any>({
      path: `/api/clues/${id}`,
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
   * @request PUT:/api/clues/{id}
   * @secure
   */
  updateClue = (
    id: number,
    data: AddUpdateClueRequest,
    params: RequestParams = {},
  ) =>
    this.request<ClueDto, any>({
      path: `/api/clues/${id}`,
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
   * @request DELETE:/api/clues/{id}
   * @secure
   */
  deleteClue = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/api/clues/${id}`,
      method: "DELETE",
      secure: true,
      format: "json",
      ...params,
    });
}
