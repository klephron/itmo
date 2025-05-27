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
  AddCaseRequest,
  AddUpdateIncidentRequest,
  CaseDto,
  IncidentMainInfoDto,
  UpdateCaseRequest,
} from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Incidents<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * @description Get incidents
   *
   * @tags incidents
   * @name GetIncidents
   * @summary Get incidents
   * @request GET:/api/incidents
   * @secure
   */
  getIncidents = (params: RequestParams = {}) =>
    this.request<IncidentMainInfoDto[], any>({
      path: `/api/incidents`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Create incident
   *
   * @tags incidents
   * @name AddIncident
   * @summary Create incident
   * @request POST:/api/incidents
   * @secure
   */
  addIncident = (data: AddUpdateIncidentRequest, params: RequestParams = {}) =>
    this.request<IncidentMainInfoDto, string>({
      path: `/api/incidents`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Get incident
   *
   * @tags incidents
   * @name GetIncident
   * @summary Get incident
   * @request GET:/api/incidents/{id}
   * @secure
   */
  getIncident = (id: number, params: RequestParams = {}) =>
    this.request<IncidentMainInfoDto, string>({
      path: `/api/incidents/${id}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Update incident
   *
   * @tags incidents
   * @name UpdateIncident
   * @summary Update incident
   * @request PUT:/api/incidents/{id}
   * @secure
   */
  updateIncident = (
    id: number,
    data: AddUpdateIncidentRequest,
    params: RequestParams = {},
  ) =>
    this.request<IncidentMainInfoDto, string>({
      path: `/api/incidents/${id}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Delete incident
   *
   * @tags incidents
   * @name DeleteIncident
   * @summary Delete incident
   * @request DELETE:/api/incidents/{id}
   * @secure
   */
  deleteIncident = (id: number, params: RequestParams = {}) =>
    this.request<object, string>({
      path: `/api/incidents/${id}`,
      method: "DELETE",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Update case for incident
   *
   * @tags incidents
   * @name UpdateCaseForIncident
   * @summary Update case for incident
   * @request PUT:/api/incidents/{id}/case
   * @secure
   */
  updateCaseForIncident = (
    id: number,
    data: UpdateCaseRequest,
    params: RequestParams = {},
  ) =>
    this.request<CaseDto, string>({
      path: `/api/incidents/${id}/case`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Create case for incident
   *
   * @tags incidents
   * @name AddCaseForIncident
   * @summary Create case for incident
   * @request POST:/api/incidents/{id}/case
   * @secure
   */
  addCaseForIncident = (
    id: number,
    data: AddCaseRequest,
    params: RequestParams = {},
  ) =>
    this.request<CaseDto, string>({
      path: `/api/incidents/${id}/case`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
}
