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
  LocationUpdateRequest,
  DefaultPermissions,
  DefaultPermissionsRequest,
  PermissionsInScopeResponse,
  ScopesResponse,
  RegisterRequest,
  RankedUserInfoResponse,
  UserCaseDto,
  UserDto,
  UserInfoResponse,
  UserPermissionsRequest,
  UserPermissionsResponse,
  UserPrimaryPosition,
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
    this.request<UserCaseDto[], any>({
      path: `/api/users/${id}/cases`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Возвращает список должностей из UserEntity.PrimaryPosition
   *
   * @tags users
   * @name GetPrimaryPositions
   * @summary Получить список должностей
   * @request GET:/api/users/primary-positions
   * @secure
   */
  getPrimaryPositions = (params: RequestParams = {}) =>
    this.request<UserPrimaryPosition[], any>({
      path: `/api/users/primary-positions`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Возвращает все scopes доступных прав
   *
   * @tags users
   * @name ListPermissionScopes
   * @summary Получить все scopes доступных прав
   * @request GET:/api/users/permissions
   * @secure
   */
  listPermissionScopes = (params: RequestParams = {}) =>
    this.request<ScopesResponse, any>({
      path: `/api/users/permissions`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Возвращает список прав в scope
   *
   * @tags users
   * @name GetPermissionsInScope
   * @summary Получить список прав в scope
   * @request GET:/api/users/permissions/{scopeName}
   * @secure
   */
  getPermissionsInScope = (scopeName: string, params: RequestParams = {}) =>
    this.request<PermissionsInScopeResponse, any>({
      path: `/api/users/permissions/${scopeName}`,
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
   * @request GET:/api/users/{id}/permissions
   * @secure
   */
  getUserPermissions = (id: number, params: RequestParams = {}) =>
    this.request<UserPermissionsResponse, void>({
      path: `/api/users/${id}/permissions`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Ранжированный список пользователей по координатам и должности
   *
   * @tags users
   * @name GetRankedUsers
   * @summary Получить ранжированный список пользователей
   * @request GET:/api/users/ranked
   * @secure
   */
  getRankedUsers = (
    query: {
      latitude: number;
      longitude: number;
      altitude: number;
      /** @default null */
      primaryPosition?: string | null;
    },
    params: RequestParams = {},
  ) =>
    this.request<RankedUserInfoResponse[], any>({
      path: `/api/users/ranked`,
      method: "GET",
      query: query,
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
   * @request PUT:/api/users/{id}/permissions
   * @secure
   */
  updateUserPermissions = (
    id: number,
    data: UserPermissionsRequest,
    params: RequestParams = {},
  ) =>
    this.request<UserPermissionsResponse, void>({
      path: `/api/users/${id}/permissions`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Полностью заменяет права пользователя на один из предопределённых наборов DefaultPermissions
   *
   * @tags users
   * @name UpdateUserPermissionsByPreset
   * @summary Установить права пользователя по предопределённому набору
   * @request PUT:/api/users/{id}/permissions/default
   * @secure
   */
  updateUserPermissionsByPreset = (
    id: number,
    data: DefaultPermissionsRequest,
    params: RequestParams = {},
  ) =>
    this.request<UserPermissionsResponse, void>({
      path: `/api/users/${id}/permissions/default`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
}
