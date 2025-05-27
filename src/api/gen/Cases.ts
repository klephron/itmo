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

import {
  AddTemplateRequest,
  AddUpdateClueRequest,
  CaseDto,
  ClueMainInfoDto,
  TemplateMainInfoDto,
} from "./data-contracts";
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
   * @request GET:/api/cases/{id}
   * @secure
   */
  getCase = (id: number, params: RequestParams = {}) =>
    this.request<CaseDto, any>({
      path: `/api/cases/${id}`,
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
   * @request GET:/api/cases/{id}/clues
   * @secure
   */
  getCaseClues = (id: number, params: RequestParams = {}) =>
    this.request<ClueMainInfoDto[], any>({
      path: `/api/cases/${id}/clues`,
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
   * @request POST:/api/cases/{id}/clues
   * @secure
   */
  addCaseClue = (
    id: number,
    data: AddUpdateClueRequest,
    params: RequestParams = {},
  ) =>
    this.request<object, any>({
      path: `/api/cases/${id}/clues`,
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
   * @request GET:/api/cases/{id}/templates
   * @secure
   */
  getCaseTemplates = (id: number, params: RequestParams = {}) =>
    this.request<TemplateMainInfoDto[], any>({
      path: `/api/cases/${id}/templates`,
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
   * @request POST:/api/cases/{id}/templates
   * @secure
   */
  addCaseTemplate = (
    id: number,
    data: AddTemplateRequest,
    params: RequestParams = {},
  ) =>
    this.request<object, any>({
      path: `/api/cases/${id}/templates`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
}
