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

import { TemplateDto, UpdateTemplateRequest } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Templates<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * @description Возвращает шаблон по идентификатору
   *
   * @tags templates
   * @name GetTemplate
   * @summary Получить шаблон
   * @request GET:/api/templates/{id}
   * @secure
   */
  getTemplate = (id: number, params: RequestParams = {}) =>
    this.request<TemplateDto, string>({
      path: `/api/templates/${id}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Обновляет шаблон по идентификатору
   *
   * @tags templates
   * @name UpdateTemplate
   * @summary Обновить шаблон
   * @request PUT:/api/templates/{id}
   * @secure
   */
  updateTemplate = (
    id: number,
    data: UpdateTemplateRequest,
    params: RequestParams = {},
  ) =>
    this.request<TemplateDto, string>({
      path: `/api/templates/${id}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Удаляет шаблон по идентификатору
   *
   * @tags templates
   * @name DeleteTemplate
   * @summary Удалить шаблон
   * @request DELETE:/api/templates/{id}
   * @secure
   */
  deleteTemplate = (id: number, params: RequestParams = {}) =>
    this.request<object, string>({
      path: `/api/templates/${id}`,
      method: "DELETE",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Переводит шаблон в статус 'добавлен в CDB' (Центральную Базу Данных)
   *
   * @tags templates
   * @name AddTemplateToCdb
   * @summary Опубликовать шаблон в Центральной БД
   * @request POST:/api/templates/{id}/cdb
   * @secure
   */
  addTemplateToCdb = (id: number, params: RequestParams = {}) =>
    this.request<TemplateDto, string>({
      path: `/api/templates/${id}/cdb`,
      method: "POST",
      secure: true,
      format: "json",
      ...params,
    });
}
