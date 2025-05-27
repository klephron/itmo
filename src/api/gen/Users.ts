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
   * @description Get users
   *
   * @tags users
   * @name GetUsers
   * @summary Get users
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
   * @description Add user
   *
   * @tags users
   * @name AddUser
   * @summary Add user
   * @request POST:/api/users
   * @secure
   */
  addUser = (data: RegisterRequest, params: RequestParams = {}) =>
    this.request<UserDto, any>({
      path: `/api/users`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Get user
   *
   * @tags users
   * @name GetUser
   * @summary Get user
   * @request GET:/api/users/{id}
   * @secure
   */
  getUser = (id: number, params: RequestParams = {}) =>
    this.request<UserDto, any>({
      path: `/api/users/${id}`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Delete user
   *
   * @tags users
   * @name DeleteUser
   * @summary Delete user
   * @request DELETE:/api/users/{id}
   * @secure
   */
  deleteUser = (id: number, params: RequestParams = {}) =>
    this.request<void, any>({
      path: `/api/users/${id}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * @description Get user cases
   *
   * @tags users
   * @name GetUserCases
   * @summary Get user cases
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
   * @description Update user location
   *
   * @tags users
   * @name UpdateUserLocation
   * @summary Update user location
   * @request PUT:/api/users/{id}/location
   * @secure
   */
  updateUserLocation = (
    id: number,
    data: LocationUpdateRequest,
    params: RequestParams = {},
  ) =>
    this.request<UserDto, any>({
      path: `/api/users/${id}/location`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
  /**
   * @description Get user privileges
   *
   * @tags users
   * @name GetUserPermissions
   * @summary Get user privileges
   * @request GET:/api/users/{id}/privileges
   * @secure
   */
  getUserPermissions = (id: number, params: RequestParams = {}) =>
    this.request<UserPermissionsResponse, any>({
      path: `/api/users/${id}/privileges`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Update user privileges
   *
   * @tags users
   * @name UpdateUserPermissions
   * @summary Update user privileges
   * @request PUT:/api/users/{id}/privileges
   * @secure
   */
  updateUserPermissions = (
    id: number,
    data: UserPermissionsRequest,
    params: RequestParams = {},
  ) =>
    this.request<UserPermissionsResponse, any>({
      path: `/api/users/${id}/privileges`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
}
