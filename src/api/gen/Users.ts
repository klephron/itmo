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
  CaseMainInfoDto,
  LocationUpdateRequest,
  RegisterRequest,
  UserDto,
  UserInfoResponse,
  UserPermissionsRequest,
  UserPermissionsResponse,
} from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Users<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * @description Возвращает список всех пользователей
   *
   * @tags users
   * @name GetUsers
   * @summary Получить список пользователей
   * @request GET:/api/users
   * @secure
   */
  getUsers = (params: RequestParams = {}) =>
    this.request<UserInfoResponse[], any>({
      path: `/api/users`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Регистрирует нового пользователя
   *
   * @tags users
   * @name AddUser
   * @summary Добавить пользователя
   * @request POST:/api/users
   * @secure
   */
  addUser = (data: RegisterRequest, params: RequestParams = {}) =>
    this.request<UserDto, void>({
      path: `/api/users`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Возвращает информацию о пользователе по идентификатору
   *
   * @tags users
   * @name GetUser
   * @summary Получить пользователя по ID
   * @request GET:/api/users/{id}
   * @secure
   */
  getUser = (id: number, params: RequestParams = {}) =>
    this.request<UserDto, void>({
      path: `/api/users/${id}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Удаляет пользователя по идентификатору
   *
   * @tags users
   * @name DeleteUser
   * @summary Удалить пользователя
   * @request DELETE:/api/users/{id}
   * @secure
   */
  deleteUser = (id: number, params: RequestParams = {}) =>
    this.request<void, void>({
      path: `/api/users/${id}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * @description Возвращает список дел, связанных с пользователем
   *
   * @tags users
   * @name GetUserCases
   * @summary Получить дела пользователя
   * @request GET:/api/users/{id}/cases
   * @secure
   */
  getUserCases = (id: number, params: RequestParams = {}) =>
    this.request<CaseMainInfoDto[], any>({
      path: `/api/users/${id}/cases`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Обновляет координаты пользователя по идентификатору
   *
   * @tags users
   * @name UpdateUserLocation
   * @summary Обновить местоположение пользователя
   * @request PUT:/api/users/{id}/location
   * @secure
   */
  updateUserLocation = (
    id: number,
    data: LocationUpdateRequest,
    params: RequestParams = {},
  ) =>
    this.request<UserDto, void>({
      path: `/api/users/${id}/location`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Возвращает список привилегий пользователя
   *
   * @tags users
   * @name GetUserPermissions
   * @summary Получить права пользователя
   * @request GET:/api/users/{id}/privileges
   * @secure
   */
  getUserPermissions = (id: number, params: RequestParams = {}) =>
    this.request<UserPermissionsResponse, void>({
      path: `/api/users/${id}/privileges`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Изменяет список прав пользователя
   *
   * @tags users
   * @name UpdateUserPermissions
   * @summary Обновить права пользователя
   * @request PUT:/api/users/{id}/privileges
   * @secure
   */
  updateUserPermissions = (
    id: number,
    data: UserPermissionsRequest,
    params: RequestParams = {},
  ) =>
    this.request<UserPermissionsResponse, void>({
      path: `/api/users/${id}/privileges`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
}
