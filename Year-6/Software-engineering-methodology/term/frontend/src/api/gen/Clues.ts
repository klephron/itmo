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

import { ClueDto, UpdateClueRequest } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Clues<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * @description Возвращает информацию об улике по её идентификатору
   *
   * @tags clues
   * @name GetClue
   * @summary Получить улику
   * @request GET:/api/clues/{id}
   * @secure
   */
  getClue = (id: number, params: RequestParams = {}) =>
    this.request<ClueDto, void>({
      path: `/api/clues/${id}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Обновляет параметры улики по идентификатору
   *
   * @tags clues
   * @name UpdateClue
   * @summary Обновить улику
   * @request PUT:/api/clues/{id}
   * @secure
   */
  updateClue = (
    id: number,
    data: UpdateClueRequest,
    params: RequestParams = {},
  ) =>
    this.request<ClueDto, void>({
      path: `/api/clues/${id}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Удаляет улику по идентификатору
   *
   * @tags clues
   * @name DeleteClue
   * @summary Удалить улику
   * @request DELETE:/api/clues/{id}
   * @secure
   */
  deleteClue = (id: number, params: RequestParams = {}) =>
    this.request<void, void>({
      path: `/api/clues/${id}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
}
