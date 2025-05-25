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

export class Cases<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * @description Get case
   *
   * @tags cases
   * @name GetCase
   * @summary Get case
   * @request GET:/cases/{id}
   * @secure
   */
  getCase = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/cases/${id}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Get case clues
   *
   * @tags cases
   * @name GetCaseClues
   * @summary Get case clues
   * @request GET:/cases/{id}/clues
   * @secure
   */
  getCaseClues = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/cases/${id}/clues`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Add case clue
   *
   * @tags cases
   * @name AddCaseClue
   * @summary Add case clue
   * @request POST:/cases/{id}/clues
   * @secure
   */
  addCaseClue = (id: number, data: object, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/cases/${id}/clues`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Get case templates
   *
   * @tags cases
   * @name GetCaseTemplates
   * @summary Get case templates
   * @request GET:/cases/{id}/templates
   * @secure
   */
  getCaseTemplates = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/cases/${id}/templates`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Add case template
   *
   * @tags cases
   * @name AddCaseTemplate
   * @summary Add case template
   * @request POST:/cases/{id}/templates
   * @secure
   */
  addCaseTemplate = (id: number, data: object, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/cases/${id}/templates`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
}
