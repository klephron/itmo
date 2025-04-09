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

import { NetworkDto } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Networks<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * No description
   *
   * @tags network-controller
   * @name GetNetworkById
   * @request GET:/networks/{id}
   */
  getNetworkById = (id: number, params: RequestParams = {}) =>
    this.request<NetworkDto, any>({
      path: `/networks/${id}`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags network-controller
   * @name UpdateNetwork
   * @request PUT:/networks/{id}
   */
  updateNetwork = (id: number, data: NetworkDto, params: RequestParams = {}) =>
    this.request<NetworkDto, any>({
      path: `/networks/${id}`,
      method: "PUT",
      body: data,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags network-controller
   * @name DeleteNetwork
   * @request DELETE:/networks/{id}
   */
  deleteNetwork = (id: number, params: RequestParams = {}) =>
    this.request<void, any>({
      path: `/networks/${id}`,
      method: "DELETE",
      ...params,
    });
  /**
   * No description
   *
   * @tags network-controller
   * @name GetAllNetworks
   * @request GET:/networks
   */
  getAllNetworks = (params: RequestParams = {}) =>
    this.request<NetworkDto[], any>({
      path: `/networks`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags network-controller
   * @name CreateNetwork
   * @request POST:/networks
   */
  createNetwork = (data: NetworkDto, params: RequestParams = {}) =>
    this.request<NetworkDto, any>({
      path: `/networks`,
      method: "POST",
      body: data,
      type: ContentType.Json,
      ...params,
    });
}
