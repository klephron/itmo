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
  AddClueRequest,
  AddTemplateRequest,
  CaseDto,
  ClueMainInfoDto,
  TemplateDto,
  TemplateMainInfoDto,
} from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Cases<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * @description Возвращает дело по его идентификатору
   *
   * @tags cases
   * @name GetCase
   * @summary Получить дело
   * @request GET:/api/cases/{id}
   * @secure
   */
  getCase = (id: number, params: RequestParams = {}) =>
    this.request<CaseDto, void>({
      path: `/api/cases/${id}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Возвращает список улик, связанных с делом
   *
   * @tags cases
   * @name GetCaseClues
   * @summary Получить улики дела
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
   * @description Создаёт новую улику, связанную с делом
   *
   * @tags cases
   * @name AddCaseClue
   * @summary Добавить улику к делу
   * @request POST:/api/cases/{id}/clues
   * @secure
   */
  addCaseClue = (
    id: number,
    data: AddClueRequest,
    params: RequestParams = {},
  ) =>
    this.request<ClueMainInfoDto, string>({
      path: `/api/cases/${id}/clues`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Возвращает список шаблонов, связанных с делом
   *
   * @tags cases
   * @name GetCaseTemplates
   * @summary Получить шаблоны дела
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
   * @description Создаёт новый шаблон, связанный с делом
   *
   * @tags cases
   * @name AddCaseTemplate
   * @summary Добавить шаблон к делу
   * @request POST:/api/cases/{id}/templates
   * @secure
   */
  addCaseTemplate = (
    id: number,
    data: AddTemplateRequest,
    params: RequestParams = {},
  ) =>
    this.request<TemplateDto, string>({
      path: `/api/cases/${id}/templates`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
}
