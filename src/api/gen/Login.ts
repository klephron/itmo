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

import { LoginRequest, LoginResponse } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Login<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * No description
   *
   * @tags auth
   * @name Login
   * @request POST:/login
   * @secure
   */
  login = (data: LoginRequest, params: RequestParams = {}) =>
    this.request<LoginResponse, any>({
      path: `/login`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      format: "json",
      ...params,
    });
}
