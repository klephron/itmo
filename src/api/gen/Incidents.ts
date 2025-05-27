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
  AddIncidentRequest,
  CaseDto,
  IncidentMainInfoDto,
  UpdateCaseRequest,
  UpdateIncidentRequest,
} from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Incidents<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * @description Возвращает все инциденты
   *
   * @tags incidents
   * @name GetIncidents
   * @summary Получить список инцидентов
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
   * @description Создаёт новый инцидент с заданными параметрами
   *
   * @tags incidents
   * @name AddIncident
   * @summary Создать инцидент
   * @request POST:/api/incidents
   * @secure
   */
  addIncident = (data: AddIncidentRequest, params: RequestParams = {}) =>
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
   * @description Возвращает подробную информацию об инциденте
   *
   * @tags incidents
   * @name GetIncident
   * @summary Получить инцидент по ID
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
   * @description Обновляет данные существующего инцидента по идентификатору
   *
   * @tags incidents
   * @name UpdateIncident
   * @summary Обновить инцидент
   * @request PUT:/api/incidents/{id}
   * @secure
   */
  updateIncident = (
    id: number,
    data: UpdateIncidentRequest,
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
   * @description Удаляет инцидент по идентификатору
   *
   * @tags incidents
   * @name DeleteIncident
   * @summary Удалить инцидент
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
   * @description Обновляет существующее дело, связанное с инцидентом
   *
   * @tags incidents
   * @name UpdateCaseForIncident
   * @summary Обновить дело по инциденту
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
   * @description Добавляет новое дело к указанному инциденту
   *
   * @tags incidents
   * @name AddCaseForIncident
   * @summary Создать дело по инциденту
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
