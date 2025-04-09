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

import { ItemDto } from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Items<
  SecurityDataType = unknown,
> extends HttpClient<SecurityDataType> {
  /**
   * No description
   *
   * @tags item-controller
   * @name GetItemById
   * @request GET:/items/{id}
   */
  getItemById = (id: number, params: RequestParams = {}) =>
    this.request<ItemDto, any>({
      path: `/items/${id}`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags item-controller
   * @name UpdateItem
   * @request PUT:/items/{id}
   */
  updateItem = (id: number, data: ItemDto, params: RequestParams = {}) =>
    this.request<ItemDto, any>({
      path: `/items/${id}`,
      method: "PUT",
      body: data,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags item-controller
   * @name DeleteItem
   * @request DELETE:/items/{id}
   */
  deleteItem = (id: number, params: RequestParams = {}) =>
    this.request<void, any>({
      path: `/items/${id}`,
      method: "DELETE",
      ...params,
    });
  /**
   * No description
   *
   * @tags item-controller
   * @name GetAllItems
   * @request GET:/items
   */
  getAllItems = (params: RequestParams = {}) =>
    this.request<ItemDto[], any>({
      path: `/items`,
      method: "GET",
      ...params,
    });
  /**
   * No description
   *
   * @tags item-controller
   * @name CreateItem
   * @request POST:/items
   */
  createItem = (data: ItemDto, params: RequestParams = {}) =>
    this.request<ItemDto, any>({
      path: `/items`,
      method: "POST",
      body: data,
      type: ContentType.Json,
      ...params,
    });
}
