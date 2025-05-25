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
   * @request GET:/users
   * @secure
   */
  getUsers = (params: RequestParams = {}) =>
    this.request<UserInfoResponse[], any>({
      path: `/users`,
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
   * @request POST:/users
   * @secure
   */
  addUser = (data: RegisterRequest, params: RequestParams = {}) =>
    this.request<UserDto, any>({
      path: `/users`,
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
   * @request GET:/users/{id}
   * @secure
   */
  getUser = (id: number, params: RequestParams = {}) =>
    this.request<UserDto, any>({
      path: `/users/${id}`,
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
   * @request DELETE:/users/{id}
   * @secure
   */
  deleteUser = (id: number, params: RequestParams = {}) =>
    this.request<void, any>({
      path: `/users/${id}`,
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
   * @request GET:/users/{id}/cases
   * @secure
   */
  getUserCases = (id: number, params: RequestParams = {}) =>
    this.request<CaseMainInfoDto[], any>({
      path: `/users/${id}/cases`,
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
   * @request PUT:/users/{id}/location
   * @secure
   */
  updateUserLocation = (
    id: number,
    data: LocationUpdateRequest,
    params: RequestParams = {},
  ) =>
    this.request<UserDto, any>({
      path: `/users/${id}/location`,
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
   * @name GetUserPrivileges
   * @summary Get user privileges
   * @request GET:/users/{id}/privileges
   * @secure
   */
  getUserPrivileges = (id: number, params: RequestParams = {}) =>
    this.request<UserPermissionsResponse, any>({
      path: `/users/${id}/privileges`,
      method: "GET",
      secure: true,
      format: "json",
      ...params,
    });
  /**
   * @description Update user privileges
   *
   * @tags users
   * @name UpdateUserPrivileges
   * @summary Update user privileges
   * @request PUT:/users/{id}/privileges
   * @secure
   */
  updateUserPrivileges = (
    id: number,
    data: UserPermissionsRequest,
    params: RequestParams = {},
  ) =>
    this.request<object, any>({
      path: `/users/${id}/privileges`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
}
