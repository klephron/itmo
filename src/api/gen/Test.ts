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

import { RegisterRequest, UserDto } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Test<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * No description
   *
   * @tags test
   * @name CheckPermissions
   * @request GET:/test/check/{id}
   * @secure
   */
  checkPermissions = (id: number, params: RequestParams = {}) =>
    this.request<string, any>({
      path: `/test/check/${id}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags test
   * @name CheckPermissions2
   * @request GET:/test/check2/{id}
   * @secure
   */
  checkPermissions2 = (id: number, params: RequestParams = {}) =>
    this.request<string, any>({
      path: `/test/check2/${id}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags test
   * @name CheckPermissions3
   * @request GET:/test/check3/{id}
   * @secure
   */
  checkPermissions3 = (id: number, params: RequestParams = {}) =>
    this.request<string, any>({
      path: `/test/check3/${id}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags test
   * @name CheckPermissions4
   * @request GET:/test/check4/{id}
   * @secure
   */
  checkPermissions4 = (id: number, params: RequestParams = {}) =>
    this.request<string, any>({
      path: `/test/check4/${id}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags test
   * @name Index
   * @request GET:/test/hello
   * @secure
   */
  index = (params: RequestParams = {}) =>
    this.request<string, any>({
      path: `/test/hello`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags test
   * @name ManualCheck
   * @request GET:/test/manual/{id}
   * @secure
   */
  manualCheck = (id: number, params: RequestParams = {}) =>
    this.request<string, any>({
      path: `/test/manual/${id}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags test
   * @name Register
   * @request POST:/test/register
   * @secure
   */
  register = (data: RegisterRequest, params: RequestParams = {}) =>
    this.request<UserDto, any>({
      path: `/test/register`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
}
