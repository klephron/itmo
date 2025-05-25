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

export class Templates<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * @description Get template
   *
   * @tags templates
   * @name GetTemplate
   * @summary Get template
   * @request GET:/templates/{id}
   * @secure
   */
  getTemplate = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/templates/${id}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Update template (generic)
   *
   * @tags templates
   * @name UpdateTemplate
   * @summary Update template (generic)
   * @request PUT:/templates/{id}
   * @secure
   */
  updateTemplate = (id: number, data: object, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/templates/${id}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Delete template
   *
   * @tags templates
   * @name DeleteTemplate
   * @summary Delete template
   * @request DELETE:/templates/{id}
   * @secure
   */
  deleteTemplate = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/templates/${id}`,
      method: "DELETE",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Publish template in Central Database
   *
   * @tags templates
   * @name AddTemplateToCdb
   * @summary Publish template in Central Database
   * @request POST:/templates/{id}/cdb
   * @secure
   */
  addTemplateToCdb = (id: number, params: RequestParams = {}) =>
    this.request<object, any>({
      path: `/templates/${id}/cdb`,
      method: "POST",
      secure: true,
      format: "json",
      ...params,
    });
}
